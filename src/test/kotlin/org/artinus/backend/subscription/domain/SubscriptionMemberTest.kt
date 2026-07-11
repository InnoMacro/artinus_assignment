package org.artinus.backend.subscription.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubscriptionMemberTest {
    @Test
    fun `회원 구독 시 상태와 변경 이력을 함께 만든다`() {
        val member = SubscriptionMember.new(PhoneNumber("01012345678"))

        val change = member.subscribe(SubscriptionStatus.BASIC)

        assertEquals(SubscriptionStatus.BASIC, member.status)
        assertEquals(SubscriptionAction.SUBSCRIBE, change.action)
        assertEquals(SubscriptionStatus.NONE, change.previousStatus)
        assertEquals(SubscriptionStatus.BASIC, change.changedStatus)
    }

    @Test
    fun `회원 해지 시 상태와 변경 이력을 함께 만든다`() {
        val member = SubscriptionMember.restore(
            id = MemberId(1L),
            phoneNumber = PhoneNumber("01012345678"),
            status = SubscriptionStatus.PREMIUM,
        )

        val change = member.unsubscribe(SubscriptionStatus.NONE)

        assertEquals(SubscriptionStatus.NONE, member.status)
        assertEquals(SubscriptionAction.UNSUBSCRIBE, change.action)
        assertEquals(SubscriptionStatus.PREMIUM, change.previousStatus)
        assertEquals(SubscriptionStatus.NONE, change.changedStatus)
    }
}
