package org.artinus.backend.subscription.application.result

data class SubscriptionHistoryResult(
    val history: List<SubscriptionHistoryItem>,
    val summary: String,
    val summarySource: SummarySource,
)
