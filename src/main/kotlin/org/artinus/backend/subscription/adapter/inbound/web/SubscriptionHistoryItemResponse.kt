package org.artinus.backend.subscription.adapter.inbound.web

import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.domain.SubscriptionAction
import org.artinus.backend.subscription.domain.SubscriptionStatus
import java.time.OffsetDateTime
import java.time.ZoneId

data class SubscriptionHistoryItemResponse(
    val channelName: String,
    val action: SubscriptionAction,
    val previousStatus: SubscriptionStatus,
    val changedStatus: SubscriptionStatus,
    val changedAt: OffsetDateTime,
) {
    companion object {
        private val RESPONSE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

        fun from(item: SubscriptionHistoryItem): SubscriptionHistoryItemResponse =
            SubscriptionHistoryItemResponse(
                channelName = item.channelName,
                action = item.action,
                previousStatus = item.previousStatus,
                changedStatus = item.changedStatus,
                changedAt = item.changedAt.atZone(RESPONSE_ZONE_ID).toOffsetDateTime(),
            )
    }
}
