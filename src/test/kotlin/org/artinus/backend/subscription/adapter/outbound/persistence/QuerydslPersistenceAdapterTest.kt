package org.artinus.backend.subscription.adapter.outbound.persistence

import jakarta.persistence.EntityManager
import org.artinus.backend.TestcontainersConfiguration
import org.artinus.backend.channel.application.port.outbound.ChannelRepository
import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.artinus.backend.subscription.domain.SubscriptionStatus
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@Transactional
class QuerydslPersistenceAdapterTest @Autowired constructor(
    private val memberRepository: SubscriptionMemberRepository,
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
}
