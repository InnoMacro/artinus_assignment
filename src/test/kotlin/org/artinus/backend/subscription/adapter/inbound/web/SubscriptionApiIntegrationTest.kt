package org.artinus.backend.subscription.adapter.inbound.web

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.artinus.backend.TestcontainersConfiguration
import org.artinus.backend.subscription.adapter.outbound.persistence.QSubscriptionHistoryJpaEntity
import org.artinus.backend.subscription.adapter.outbound.persistence.QSubscriptionMemberJpaEntity
import org.artinus.backend.subscription.application.port.outbound.ApprovalDecision
import org.artinus.backend.subscription.application.port.outbound.SubscriptionApprovalPort
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistorySummarizer
import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.context.WebApplicationContext

@Import(TestcontainersConfiguration::class, SubscriptionApiIntegrationTest.ApprovalTestConfiguration::class)
@SpringBootTest
@Transactional
class SubscriptionApiIntegrationTest @Autowired constructor(
    private val context: WebApplicationContext,
    private val approvalPort: ControllableApprovalPort,
    private val historySummarizer: ControllableHistorySummarizer,
    private val entityManager: EntityManager,
    private val jdbcTemplate: JdbcTemplate,
) {
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        approvalPort.decision = ApprovalDecision.APPROVED
        historySummarizer.response = "홈페이지에서 일반 구독을 시작했습니다."
        historySummarizer.receivedHistories.clear()
        historySummarizer.transactionActiveDuringSummary = null
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    @Test
    fun `구독 후 해지 API를 호출하면 현재 상태와 이력이 함께 변경된다`() {
        performChange("/api/v1/subscriptions", "BASIC")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("BASIC"))

        performChange("/api/v1/subscriptions/unsubscribe", "NONE")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("NONE"))

        entityManager.flush()
        val queryFactory = JPAQueryFactory(entityManager)
        val member = QSubscriptionMemberJpaEntity.subscriptionMemberJpaEntity
        val history = QSubscriptionHistoryJpaEntity.subscriptionHistoryJpaEntity

        assertEquals(
            0.toByte(),
            queryFactory.select(member.status).from(member)
                .where(member.phoneNumber.eq("01012345678"))
                .fetchOne()
                ?.dbCode,
        )
        assertEquals(
            2L,
            queryFactory.select(history.count()).from(history).fetchOne(),
        )
    }

    @Test
    fun `csrng 거절 시 422를 반환하고 회원과 이력을 저장하지 않는다`() {
        approvalPort.decision = ApprovalDecision.REJECTED

        performChange("/api/v1/subscriptions", "PREMIUM")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("CSRNG_REJECTED"))

        entityManager.flush()
        val queryFactory = JPAQueryFactory(entityManager)
        val member = QSubscriptionMemberJpaEntity.subscriptionMemberJpaEntity
        val history = QSubscriptionHistoryJpaEntity.subscriptionHistoryJpaEntity

        assertEquals(0L, queryFactory.select(member.count()).from(member).fetchOne())
        assertEquals(0L, queryFactory.select(history.count()).from(history).fetchOne())
    }

    @Test
    fun `존재하지 않는 회원의 이력 조회는 404를 반환한다`() {
        mockMvc.perform(get("/api/v1/subscriptions/01099998888/histories"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"))
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `구독 후 이력 조회 API는 채널 정보와 LLM 요약을 함께 반환한다`() {
        val phoneNumber = "01033334444"

        try {
            performChange("/api/v1/subscriptions", "BASIC", phoneNumber)
                .andExpect(status().isOk)

            mockMvc.perform(get("/api/v1/subscriptions/$phoneNumber/histories"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.history[0].channelName").value("홈페이지"))
                .andExpect(jsonPath("$.history[0].action").value("SUBSCRIBE"))
                .andExpect(jsonPath("$.history[0].previousStatus").value("NONE"))
                .andExpect(jsonPath("$.history[0].changedStatus").value("BASIC"))
                .andExpect(jsonPath("$.summary").value("홈페이지에서 일반 구독을 시작했습니다."))
                .andExpect(jsonPath("$.summarySource").value("LLM"))

            assertEquals(1, historySummarizer.receivedHistories.single().size)
        } finally {
            cleanup(phoneNumber)
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `이력 조회 transaction을 종료한 뒤 LLM 요약을 호출한다`() {
        val phoneNumber = "01022223333"

        try {
            performChange("/api/v1/subscriptions", "BASIC", phoneNumber)
                .andExpect(status().isOk)

            mockMvc.perform(get("/api/v1/subscriptions/$phoneNumber/histories"))
                .andExpect(status().isOk)

            assertEquals(false, historySummarizer.transactionActiveDuringSummary)
        } finally {
            cleanup(phoneNumber)
        }
    }

    private fun cleanup(phoneNumber: String) {
        jdbcTemplate.update(
            "DELETE FROM subscription_history WHERE member_id IN " +
                "(SELECT id FROM subscription_member WHERE phone_number = ?)",
            phoneNumber,
        )
        jdbcTemplate.update(
            "DELETE FROM subscription_member WHERE phone_number = ?",
            phoneNumber,
        )
    }

    private fun performChange(
        path: String,
        status: String,
        phoneNumber: String = "01012345678",
    ) =
        mockMvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"phoneNumber":"$phoneNumber","channelId":1,"targetStatus":"$status"}""",
                ),
        )

    class ControllableApprovalPort : SubscriptionApprovalPort {
        var decision: ApprovalDecision = ApprovalDecision.APPROVED

        override fun requestApproval(): ApprovalDecision = decision
    }

    class ControllableHistorySummarizer : SubscriptionHistorySummarizer {
        var response: String = ""
        val receivedHistories = mutableListOf<List<SubscriptionHistoryItem>>()
        var transactionActiveDuringSummary: Boolean? = null

        override fun summarize(histories: List<SubscriptionHistoryItem>): String {
            receivedHistories += histories
            transactionActiveDuringSummary = TransactionSynchronizationManager.isActualTransactionActive()
            return response
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class ApprovalTestConfiguration {
        @Bean
        @Primary
        fun controllableApprovalPort(): ControllableApprovalPort = ControllableApprovalPort()

        @Bean
        @Primary
        fun controllableHistorySummarizer(): ControllableHistorySummarizer =
            ControllableHistorySummarizer()
    }
}
