package org.artinus.backend.subscription.application.service

import org.artinus.backend.channel.application.port.outbound.ChannelRepository
import org.artinus.backend.channel.application.exception.ChannelNotFoundException
import org.artinus.backend.channel.domain.Channel
import org.artinus.backend.channel.domain.exception.ChannelActionNotAllowedException
import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalInvalidResponseException
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalRejectedException
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalUnavailableException
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.application.port.outbound.ApprovalDecision
import org.artinus.backend.subscription.application.port.outbound.SubscriptionApprovalPort
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryRepository
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.domain.vo.MemberId
import org.artinus.backend.subscription.domain.SubscriptionHistory
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ChangeSubscriptionServiceTest {
    private val memberRepository = FakeMemberRepository()
    private val historyRepository = FakeHistoryRepository()
    private val channelRepository = FakeChannelRepository()
    private val approvalPort = FakeApprovalPort()
    private val clock = Clock.fixed(Instant.parse("2026-07-11T12:00:00Z"), ZoneOffset.UTC)
    private val preflightService = SubscriptionChangePreflightService(memberRepository, channelRepository)
    private val transactionService =
        SubscriptionChangeTransactionService(
            memberRepository,
            historyRepository,
            channelRepository,
            clock,
        )
    private val service =
        ChangeSubscriptionService(
            preflightService,
            transactionService,
            approvalPort,
        )

    @Test
    fun `최초 회원을 일반 구독으로 가입시키고 이력을 저장한다`() {
        val result = service.subscribe(command(SubscriptionStatus.BASIC))

        assertEquals(SubscriptionStatus.BASIC, result.status)
        assertEquals(SubscriptionStatus.BASIC, memberRepository.saved.single().status)
        assertEquals(SubscriptionStatus.NONE, historyRepository.saved.single().previousStatus)
        assertEquals(Instant.parse("2026-07-11T12:00:00Z"), historyRepository.saved.single().changedAt)
        assertEquals(1, memberRepository.findCount)
        assertEquals(1, memberRepository.findForUpdateCount)
    }

    @Test
    fun `csrng가 거절하면 회원과 이력을 저장하지 않는다`() {
        approvalPort.decision = ApprovalDecision.REJECTED

        assertThrows(SubscriptionApprovalRejectedException::class.java) {
            service.subscribe(command(SubscriptionStatus.BASIC))
        }

        assertEquals(0, memberRepository.saved.size)
        assertEquals(0, historyRepository.saved.size)
    }

    @Test
    fun `승인 서비스를 사용할 수 없으면 회원과 이력을 저장하지 않는다`() {
        approvalPort.failure = SubscriptionApprovalUnavailableException()

        assertThrows(SubscriptionApprovalUnavailableException::class.java) {
            service.subscribe(command(SubscriptionStatus.BASIC))
        }

        assertEquals(0, memberRepository.saved.size)
        assertEquals(0, historyRepository.saved.size)
    }

    @Test
    fun `승인 서비스 응답이 잘못되면 회원과 이력을 저장하지 않는다`() {
        approvalPort.failure = SubscriptionApprovalInvalidResponseException()

        assertThrows(SubscriptionApprovalInvalidResponseException::class.java) {
            service.subscribe(command(SubscriptionStatus.BASIC))
        }

        assertEquals(0, memberRepository.saved.size)
        assertEquals(0, historyRepository.saved.size)
    }

    @Test
    fun `구독 불가 채널은 csrng 호출 전에 거절한다`() {
        channelRepository.channel = Channel(ChannelId(1), "CALL_CENTER", "콜센터", false, true)

        assertThrows(ChannelActionNotAllowedException::class.java) {
            service.subscribe(command(SubscriptionStatus.BASIC))
        }

        assertFalse(approvalPort.called)
    }

    @Test
    fun `기존 프리미엄 회원을 일반 구독으로 해지하고 이력을 저장한다`() {
        memberRepository.member =
            SubscriptionMember.restore(
                MemberId(1),
                PhoneNumber("01012345678"),
                SubscriptionStatus.PREMIUM,
            )

        val result = service.unsubscribe(command(SubscriptionStatus.BASIC))

        assertEquals(SubscriptionStatus.BASIC, result.status)
        assertEquals(SubscriptionStatus.PREMIUM, historyRepository.saved.single().previousStatus)
    }

    @Test
    fun `존재하지 않는 회원은 해지할 수 없고 csrng도 호출하지 않는다`() {
        assertThrows(SubscriptionMemberNotFoundException::class.java) {
            service.unsubscribe(command(SubscriptionStatus.NONE))
        }

        assertFalse(approvalPort.called)
    }

    @Test
    fun `승인 중 상태가 바뀌면 쓰기 단계에서 최신 상태로 전이를 다시 계산한다`() {
        memberRepository.member =
            SubscriptionMember.restore(
                MemberId(1),
                PhoneNumber("01012345678"),
                SubscriptionStatus.NONE,
            )
        approvalPort.beforeDecision = {
            memberRepository.member =
                SubscriptionMember.restore(
                    MemberId(1),
                    PhoneNumber("01012345678"),
                    SubscriptionStatus.BASIC,
                )
        }

        val result = service.subscribe(command(SubscriptionStatus.PREMIUM))

        assertEquals(SubscriptionStatus.PREMIUM, result.status)
        assertEquals(SubscriptionStatus.BASIC, historyRepository.saved.single().previousStatus)
        assertEquals(SubscriptionStatus.PREMIUM, historyRepository.saved.single().changedStatus)
    }

    private fun command(target: SubscriptionStatus) =
        ChangeSubscriptionCommand(
            phoneNumber = PhoneNumber("01012345678"),
            channelId = ChannelId(1),
            targetStatus = target,
        )

    private class FakeMemberRepository : SubscriptionMemberRepository {
        var member: SubscriptionMember? = null
        val saved = mutableListOf<SubscriptionMember>()
        var findCount = 0
        var findForUpdateCount = 0

        override fun findByPhoneNumber(phoneNumber: PhoneNumber): SubscriptionMember? {
            findCount++
            return member?.detachedCopy()
        }

        override fun findByPhoneNumberForUpdate(phoneNumber: PhoneNumber): SubscriptionMember? {
            findForUpdateCount++
            return member?.detachedCopy()
        }

        override fun getByPhoneNumberForUpdate(phoneNumber: PhoneNumber): SubscriptionMember =
            findByPhoneNumberForUpdate(phoneNumber)
                ?: throw SubscriptionMemberNotFoundException(phoneNumber)

        override fun save(member: SubscriptionMember): SubscriptionMember {
            val savedMember =
                member.id?.let { member }
                    ?: SubscriptionMember.restore(MemberId(1), member.phoneNumber, member.status)
            saved += savedMember
            this.member = savedMember
            return savedMember
        }

        private fun SubscriptionMember.detachedCopy(): SubscriptionMember =
            SubscriptionMember.restore(requireNotNull(id), phoneNumber, status)
    }

    private class FakeHistoryRepository : SubscriptionHistoryRepository {
        val saved = mutableListOf<SubscriptionHistory>()

        override fun save(history: SubscriptionHistory): SubscriptionHistory =
            history.copy(id = 1).also(saved::add)
    }

    private class FakeChannelRepository : ChannelRepository {
        var channel: Channel? = Channel(ChannelId(1), "WEB", "홈페이지", true, true)

        override fun getById(id: ChannelId): Channel = channel ?: throw ChannelNotFoundException(id)
    }

    private class FakeApprovalPort : SubscriptionApprovalPort {
        var decision = ApprovalDecision.APPROVED
        var called = false
        var failure: RuntimeException? = null
        var beforeDecision: () -> Unit = {}

        override fun requestApproval(): ApprovalDecision {
            called = true
            beforeDecision()
            failure?.let { throw it }
            return decision
        }
    }
}
