package org.artinus.backend.subscription.adapter.outbound.persistence

import org.artinus.backend.subscription.domain.MemberId
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.artinus.backend.subscription.domain.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubscriptionMemberJpaEntityTest {
    @Test
    fun `도메인 객체와 JPA Entity를 상호 변환한다`() {
        val member = SubscriptionMember.restore(
            MemberId(10L),
            PhoneNumber("010-1234-5678"),
            SubscriptionStatus.BASIC,
        )

        val entity = SubscriptionMemberJpaEntity.from(member)
        val restored = entity.toDomain()

        assertEquals(10L, entity.id)
        assertEquals("01012345678", entity.phoneNumber)
        assertEquals(MemberId(10L), restored.id)
        assertEquals(SubscriptionStatus.BASIC, restored.status)
    }
}
