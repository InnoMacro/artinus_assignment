package org.artinus.backend.subscription.application.port.outbound

import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionMember

interface SubscriptionMemberRepository {
    fun findByPhoneNumberForUpdate(phoneNumber: PhoneNumber): SubscriptionMember?

    fun save(member: SubscriptionMember): SubscriptionMember
}
