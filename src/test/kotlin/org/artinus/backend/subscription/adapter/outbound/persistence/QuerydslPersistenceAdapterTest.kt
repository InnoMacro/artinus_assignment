package org.artinus.backend.subscription.adapter.outbound.persistence

import jakarta.persistence.EntityManager
import org.artinus.backend.TestcontainersConfiguration
import org.artinus.backend.channel.application.port.outbound.ChannelRepository
import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryQueryPort
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryRepository
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.domain.vo.MemberId
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.artinus.backend.subscription.domain.vo.SubscriptionAction
import org.artinus.backend.subscription.domain.SubscriptionHistory
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@Transactional
class QuerydslPersistenceAdapterTest @Autowired constructor(
    private val memberRepository: SubscriptionMemberRepository,
    private val historyRepository: SubscriptionHistoryRepository,
    private val historyQueryPort: SubscriptionHistoryQueryPort,
    private val channelRepository: ChannelRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `회원 저장 후 QueryDSL 비관적 잠금 조회로 복원한다`() {
        val saved = memberRepository.save(SubscriptionMember.new(PhoneNumber("01012345678")))
        entityManager.flush()
        entityManager.clear()

        val locked = memberRepository.findByPhoneNumberForUpdate(PhoneNumber("010-1234-5678"))

        assertNotNull(saved.id)
        assertEquals(saved.id, locked?.id)
        assertEquals(SubscriptionStatus.NONE, locked?.status)
    }

    @Test
    fun `초기 채널을 JPA Repository로 조회한다`() {
        val channel = channelRepository.getById(ChannelId(1L))

        assertEquals("WEB", channel.code)
        assertEquals("홈페이지", channel.name)
    }

    @Test
    fun `필수 회원 잠금 조회에 결과가 없으면 마스킹 가능한 식별자를 포함한 예외를 발생시킨다`() {
        val exception =
            org.junit.jupiter.api.Assertions.assertThrows(SubscriptionMemberNotFoundException::class.java) {
                memberRepository.getByPhoneNumberForUpdate(PhoneNumber("01099998888"))
            }

        assertEquals("010****8888", exception.phoneNumber.masked())
    }

    @Test
    fun `휴대폰 번호로 채널 이름을 포함한 이력을 안정적인 시간순으로 조회한다`() {
        val member = memberRepository.save(SubscriptionMember.new(PhoneNumber("01012345678")))
        val memberId = requireNotNull(member.id)
        historyRepository.save(history(memberId, channelId = 2, changedAt = "2026-02-01T12:00:00Z"))
        historyRepository.save(history(memberId, channelId = 1, changedAt = "2026-01-01T12:00:00Z"))
        entityManager.flush()
        entityManager.clear()

        val result = requireNotNull(historyQueryPort.findAllByPhoneNumber(PhoneNumber("010-1234-5678")))

        assertEquals(listOf("홈페이지", "모바일앱"), result.map { it.channelName })
        assertEquals(
            listOf(Instant.parse("2026-01-01T12:00:00Z"), Instant.parse("2026-02-01T12:00:00Z")),
            result.map { it.changedAt },
        )
    }

    @Test
    fun `동일한 변경 시각의 이력은 id 오름차순으로 조회한다`() {
        val member = memberRepository.save(SubscriptionMember.new(PhoneNumber("01012345678")))
        val memberId = requireNotNull(member.id)
        val first = historyRepository.save(history(memberId, channelId = 2, changedAt = "2026-01-01T12:00:00Z"))
        val second = historyRepository.save(history(memberId, channelId = 1, changedAt = "2026-01-01T12:00:00Z"))
        entityManager.flush()
        entityManager.clear()

        val result = requireNotNull(historyQueryPort.findAllByPhoneNumber(PhoneNumber("01012345678")))

        assertEquals(listOf(first.id, second.id), result.map(SubscriptionHistoryItem::id))
    }

    @Test
    fun `이력 조회 대상 회원이 없으면 null을 반환한다`() {
        assertNull(historyQueryPort.findAllByPhoneNumber(PhoneNumber("01099998888")))
    }

    @Test
    fun `회원은 있지만 변경 이력이 없으면 빈 목록을 반환한다`() {
        memberRepository.save(SubscriptionMember.new(PhoneNumber("01012345678")))
        entityManager.flush()
        entityManager.clear()

        val result = historyQueryPort.findAllByPhoneNumber(PhoneNumber("01012345678"))

        assertEquals(emptyList<SubscriptionHistoryItem>(), result)
    }

    private fun history(
        memberId: MemberId,
        channelId: Long,
        changedAt: String,
    ): SubscriptionHistory =
        SubscriptionHistory(
            memberId = memberId,
            channelId = ChannelId(channelId),
            action = SubscriptionAction.SUBSCRIBE,
            previousStatus = SubscriptionStatus.NONE,
            changedStatus = SubscriptionStatus.BASIC,
            changedAt = Instant.parse(changedAt),
        )
}
