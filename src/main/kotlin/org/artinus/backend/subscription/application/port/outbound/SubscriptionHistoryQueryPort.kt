package org.artinus.backend.subscription.application.port.outbound

import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.domain.PhoneNumber

fun interface SubscriptionHistoryQueryPort {
    fun findAllByPhoneNumber(phoneNumber: PhoneNumber): List<SubscriptionHistoryItem>
}
