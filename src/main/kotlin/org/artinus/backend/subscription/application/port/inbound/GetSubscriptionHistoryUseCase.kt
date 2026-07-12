package org.artinus.backend.subscription.application.port.inbound

import org.artinus.backend.subscription.application.result.SubscriptionHistoryResult
import org.artinus.backend.subscription.domain.PhoneNumber

fun interface GetSubscriptionHistoryUseCase {
    fun getHistory(phoneNumber: PhoneNumber): SubscriptionHistoryResult
}
