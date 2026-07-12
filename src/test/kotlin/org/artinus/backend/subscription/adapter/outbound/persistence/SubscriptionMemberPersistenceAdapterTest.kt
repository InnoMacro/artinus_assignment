package org.artinus.backend.subscription.adapter.outbound.persistence

import com.querydsl.jpa.impl.JPAQueryFactory
import org.artinus.backend.subscription.application.exception.SubscriptionChangeConflictException
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException

class SubscriptionMemberPersistenceAdapterTest {
    private val queryFactory = Mockito.mock(JPAQueryFactory::class.java)
    private val jpaRepository = Mockito.mock(SubscriptionMemberJpaRepository::class.java)
    private val adapter = SubscriptionMemberPersistenceAdapter(queryFactory, jpaRepository)

    @Test
    fun `휴대폰 번호 unique 충돌은 구독 변경 충돌으로 번역한다`() {
        val failure =
            DataIntegrityViolationException(
                "Duplicate entry for key 'uk_subscription_member_phone_number'",
            )
        failOnSave(failure)

        val exception =
            assertThrows(SubscriptionChangeConflictException::class.java) {
                adapter.save(SubscriptionMember.new(PhoneNumber("01012345678")))
            }

        assertSame(failure, exception.cause)
    }

    @Test
    fun `알 수 없는 무결성 위반은 충돌으로 숨기지 않는다`() {
        val failure = DataIntegrityViolationException("fk_subscription_history_channel violation")
        failOnSave(failure)

        val exception =
            assertThrows(DataIntegrityViolationException::class.java) {
                adapter.save(SubscriptionMember.new(PhoneNumber("01012345678")))
            }

        assertSame(failure, exception)
    }

    private fun failOnSave(failure: RuntimeException) {
        Mockito.doThrow(failure)
            .`when`(jpaRepository)
            .save(Mockito.any(SubscriptionMemberJpaEntity::class.java))
    }
}
