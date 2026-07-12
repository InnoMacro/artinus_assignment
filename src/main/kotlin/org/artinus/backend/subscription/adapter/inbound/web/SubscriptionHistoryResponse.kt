package org.artinus.backend.subscription.adapter.inbound.web

import org.artinus.backend.subscription.application.result.SubscriptionHistoryResult
import org.artinus.backend.subscription.application.result.SummarySource

data class SubscriptionHistoryResponse(
    val history: List<SubscriptionHistoryItemResponse>,
    val summary: String,
    val summarySource: SummarySource,
) {
    companion object {
        fun from(result: SubscriptionHistoryResult): SubscriptionHistoryResponse =
            SubscriptionHistoryResponse(
                history = result.history.map(SubscriptionHistoryItemResponse::from),
                summary = result.summary,
                summarySource = result.summarySource,
            )
    }
}
