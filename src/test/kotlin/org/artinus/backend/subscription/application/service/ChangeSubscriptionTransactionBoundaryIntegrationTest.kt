package org.artinus.backend.subscription.application.service

import org.artinus.backend.TestcontainersConfiguration
import org.artinus.backend.subscription.adapter.outbound.persistence.SubscriptionHistoryPersistenceAdapter
import org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalRejectedException
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalUnavailableException
import org.artinus.backend.subscription.application.exception.SubscriptionChangeConflictException
import org.artinus.backend.subscription.application.port.inbound.SubscribeUseCase
import org.artinus.backend.subscription.application.port.outbound.ApprovalDecision
import org.artinus.backend.subscription.application.port.outbound.SubscriptionApprovalPort
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryRepository
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.application.result.ChangeSubscriptionResult
import org.artinus.backend.subscription.domain.SubscriptionHistory
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Import(
    TestcontainersConfiguration::class,
    ChangeSubscriptionTransactionBoundaryIntegrationTest.BoundaryTestConfiguration::class,
)
@SpringBootTest
class ChangeSubscriptionTransactionBoundaryIntegrationTest @Autowired constructor(
    private val subscribeUseCase: SubscribeUseCase,
    private val memberRepository: SubscriptionMemberRepository,
    private val approvalPort: TransactionRecordingApprovalPort,
    private val historyRepository: ControllableHistoryRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val transactionTemplate: TransactionTemplate,
) {
    @BeforeEach
    fun setUp() {
        cleanup()
        approvalPort.reset()
        historyRepository.reset()
    }

    @AfterEach
    fun tearDown() {
        approvalPort.release()
        cleanup()
    }

    @Test
    fun `외부 승인은 transaction 밖에서 호출하고 저장만 쓰기 transaction에서 수행한다`() {
        val phoneNumber = PhoneNumber(APPROVED_PHONE_NUMBER)

        subscribeUseCase.subscribe(command(phoneNumber))

        assertFalse(requireNotNull(approvalPort.transactionActive))
        assertTrue(requireNotNull(historyRepository.transactionActiveDuringSave))
        assertEquals(1L, memberCount(APPROVED_PHONE_NUMBER))
        assertEquals(1L, historyCount(APPROVED_PHONE_NUMBER))
    }

    @Test
    fun `transaction 호출자는 외부 승인 전에 거절한다`() {
        val phoneNumber = PhoneNumber(TRANSACTIONAL_CALLER_PHONE_NUMBER)

        transactionTemplate.executeWithoutResult {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive())

            assertThrows(IllegalTransactionStateException::class.java) {
                subscribeUseCase.subscribe(command(phoneNumber))
            }
        }

        assertEquals(null, approvalPort.transactionActive)
        assertNoWrites(TRANSACTIONAL_CALLER_PHONE_NUMBER)
    }

    @Test
    fun `외부 승인이 거절되면 회원과 이력을 쓰지 않는다`() {
        approvalPort.decision = ApprovalDecision.REJECTED

        assertThrows(SubscriptionApprovalRejectedException::class.java) {
            subscribeUseCase.subscribe(command(PhoneNumber(REJECTED_PHONE_NUMBER)))
        }

        assertFalse(requireNotNull(approvalPort.transactionActive))
        assertNoWrites(REJECTED_PHONE_NUMBER)
    }

    @Test
    fun `외부 승인 호출이 실패하면 회원과 이력을 쓰지 않는다`() {
        approvalPort.failure = SubscriptionApprovalUnavailableException()

        assertThrows(SubscriptionApprovalUnavailableException::class.java) {
            subscribeUseCase.subscribe(command(PhoneNumber(UNAVAILABLE_PHONE_NUMBER)))
        }

        assertFalse(requireNotNull(approvalPort.transactionActive))
        assertNoWrites(UNAVAILABLE_PHONE_NUMBER)
    }

    @Test
    fun `이력 저장이 실패하면 회원 저장도 함께 rollback한다`() {
        historyRepository.failureAfterSave = IllegalStateException("이력 저장 실패")

        assertThrows(IllegalStateException::class.java) {
            subscribeUseCase.subscribe(command(PhoneNumber(ROLLBACK_PHONE_NUMBER)))
        }

        assertTrue(requireNotNull(historyRepository.transactionActiveDuringSave))
        assertNoWrites(ROLLBACK_PHONE_NUMBER)
    }

    @Test
    fun `외부 승인을 기다리는 동안 회원 row lock을 점유하지 않는다`() {
        val phoneNumber = PhoneNumber(LOCK_PHONE_NUMBER)
        jdbcTemplate.update(
            "INSERT INTO subscription_member (phone_number, status) VALUES (?, ?)",
            phoneNumber.value,
            0,
        )
        approvalPort.blockUntilReleased()
        val executor = Executors.newFixedThreadPool(2)

        try {
            val change =
                executor.submit<ChangeSubscriptionResult> {
                    subscribeUseCase.subscribe(command(phoneNumber))
                }

            assertTrue(approvalPort.awaitCall(5, TimeUnit.SECONDS))

            val lockAttempt =
                executor.submit<Boolean> {
                    transactionTemplate.execute {
                        assertTrue(TransactionSynchronizationManager.isActualTransactionActive())
                        memberRepository.findByPhoneNumberForUpdate(phoneNumber)
                        true
                    }
                }
            assertTrue(lockAttempt.get(10, TimeUnit.SECONDS))

            approvalPort.release()
            assertEquals(SubscriptionStatus.BASIC, change.get(5, TimeUnit.SECONDS).status)
        } finally {
            approvalPort.release()
            executor.shutdownNow()
        }
    }

    @Test
    fun `동일한 신규 회원의 동시 구독은 하나만 저장한다`() {
        val phoneNumber = PhoneNumber(CONCURRENT_NEW_MEMBER_PHONE_NUMBER)
        approvalPort.blockUntilCalls(2)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val requests =
                List(2) {
                    executor.submit<ChangeSubscriptionResult> {
                        subscribeUseCase.subscribe(command(phoneNumber))
                    }
                }

            assertTrue(approvalPort.awaitCall(5, TimeUnit.SECONDS))
            approvalPort.release()

            val outcomes =
                requests.map { request ->
                    runCatching { request.get(10, TimeUnit.SECONDS) }
                }
            assertEquals(1, outcomes.count { it.isSuccess })
            val failure =
                requireNotNull(outcomes.single { it.isFailure }.exceptionOrNull()).let { exception ->
                    if (exception is ExecutionException) exception.cause else exception
                }

            assertTrue(
                failure is SubscriptionChangeConflictException,
                "실패를 구독 변경 충돌으로 번역해야 합니다. actual=${failure?.javaClass?.name}",
            )
            assertEquals(1L, memberCount(CONCURRENT_NEW_MEMBER_PHONE_NUMBER))
            assertEquals(1L, historyCount(CONCURRENT_NEW_MEMBER_PHONE_NUMBER))
        } finally {
            approvalPort.release()
            executor.shutdownNow()
        }
    }

    private fun command(phoneNumber: PhoneNumber): ChangeSubscriptionCommand =
        ChangeSubscriptionCommand(
            phoneNumber = phoneNumber,
            channelId = org.artinus.backend.channel.domain.ChannelId(1),
            targetStatus = SubscriptionStatus.BASIC,
        )

    private fun assertNoWrites(phoneNumber: String) {
        assertEquals(0L, memberCount(phoneNumber))
        assertEquals(0L, historyCount(phoneNumber))
    }

    private fun memberCount(phoneNumber: String): Long =
        requireNotNull(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subscription_member WHERE phone_number = ?",
                Long::class.java,
                phoneNumber,
            ),
        )

    private fun historyCount(phoneNumber: String): Long =
        requireNotNull(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM subscription_history h
                JOIN subscription_member m ON m.id = h.member_id
                WHERE m.phone_number = ?
                """.trimIndent(),
                Long::class.java,
                phoneNumber,
            ),
        )

    private fun cleanup() {
        val placeholders = TEST_PHONE_NUMBERS.joinToString(",") { "?" }
        jdbcTemplate.update(
            "DELETE FROM subscription_history WHERE member_id IN " +
                "(SELECT id FROM subscription_member WHERE phone_number IN ($placeholders))",
            *TEST_PHONE_NUMBERS.toTypedArray(),
        )
        jdbcTemplate.update(
            "DELETE FROM subscription_member WHERE phone_number IN ($placeholders)",
            *TEST_PHONE_NUMBERS.toTypedArray(),
        )
    }

    class TransactionRecordingApprovalPort : SubscriptionApprovalPort {
        @Volatile
        var decision: ApprovalDecision = ApprovalDecision.APPROVED

        @Volatile
        var failure: RuntimeException? = null

        @Volatile
        var transactionActive: Boolean? = null

        @Volatile
        private var blocking = false

        @Volatile
        private var called = CountDownLatch(1)

        @Volatile
        private var released = CountDownLatch(1)

        override fun requestApproval(): ApprovalDecision {
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive()
            called.countDown()
            if (blocking && !released.await(10, TimeUnit.SECONDS)) {
                throw IllegalStateException("승인 대기 해제 시간이 초과되었습니다.")
            }
            failure?.let { throw it }
            return decision
        }

        fun blockUntilReleased() {
            blockUntilCalls(1)
        }

        fun blockUntilCalls(expectedCalls: Int) {
            blocking = true
            called = CountDownLatch(expectedCalls)
        }

        fun awaitCall(
            timeout: Long,
            unit: TimeUnit,
        ): Boolean = called.await(timeout, unit)

        fun release() {
            released.countDown()
        }

        fun reset() {
            decision = ApprovalDecision.APPROVED
            failure = null
            transactionActive = null
            blocking = false
            called = CountDownLatch(1)
            released = CountDownLatch(1)
        }
    }

    class ControllableHistoryRepository(
        private val delegate: SubscriptionHistoryPersistenceAdapter,
    ) : SubscriptionHistoryRepository {
        @Volatile
        var failureAfterSave: RuntimeException? = null

        @Volatile
        var transactionActiveDuringSave: Boolean? = null

        override fun save(history: SubscriptionHistory): SubscriptionHistory {
            transactionActiveDuringSave = TransactionSynchronizationManager.isActualTransactionActive()
            val saved = delegate.save(history)
            failureAfterSave?.let { throw it }
            return saved
        }

        fun reset() {
            failureAfterSave = null
            transactionActiveDuringSave = null
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class BoundaryTestConfiguration {
        @Bean
        @Primary
        fun transactionRecordingApprovalPort(): TransactionRecordingApprovalPort =
            TransactionRecordingApprovalPort()

        @Bean
        @Primary
        fun controllableHistoryRepository(
            delegate: SubscriptionHistoryPersistenceAdapter,
        ): ControllableHistoryRepository = ControllableHistoryRepository(delegate)
    }

    companion object {
        private const val APPROVED_PHONE_NUMBER = "01070000001"
        private const val REJECTED_PHONE_NUMBER = "01070000002"
        private const val UNAVAILABLE_PHONE_NUMBER = "01070000003"
        private const val ROLLBACK_PHONE_NUMBER = "01070000004"
        private const val LOCK_PHONE_NUMBER = "01070000005"
        private const val TRANSACTIONAL_CALLER_PHONE_NUMBER = "01070000006"
        private const val CONCURRENT_NEW_MEMBER_PHONE_NUMBER = "01070000007"
        private val TEST_PHONE_NUMBERS =
            listOf(
                APPROVED_PHONE_NUMBER,
                REJECTED_PHONE_NUMBER,
                UNAVAILABLE_PHONE_NUMBER,
                ROLLBACK_PHONE_NUMBER,
                LOCK_PHONE_NUMBER,
                TRANSACTIONAL_CALLER_PHONE_NUMBER,
                CONCURRENT_NEW_MEMBER_PHONE_NUMBER,
            )
    }
}
