package org.artinus.backend.subscription.application.port.outbound

import org.artinus.backend.subscription.domain.SubscriptionHistory

interface SubscriptionHistoryRepository {
    fun save(history: SubscriptionHistory): SubscriptionHistory
}
