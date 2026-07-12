package org.artinus.backend.subscription.application.result

import org.artinus.backend.subscription.domain.vo.SubscriptionAction
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import java.time.Instant

data class SubscriptionHistoryItem(
    val id: Long,
    val channelName: String,
    val action: SubscriptionAction,
    val previousStatus: SubscriptionStatus,
    val changedStatus: SubscriptionStatus,
    val changedAt: Instant,
)
