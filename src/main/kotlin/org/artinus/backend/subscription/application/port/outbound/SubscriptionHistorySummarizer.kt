package org.artinus.backend.subscription.application.port.outbound

import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem

fun interface SubscriptionHistorySummarizer {
    fun summarize(histories: List<SubscriptionHistoryItem>): String
}
