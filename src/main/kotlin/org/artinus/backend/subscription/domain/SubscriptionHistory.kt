package org.artinus.backend.subscription.domain

import org.artinus.backend.channel.domain.ChannelId
import java.time.Instant

data class SubscriptionHistory(
    val id: Long? = null,
    val memberId: MemberId,
    val channelId: ChannelId,
    val action: SubscriptionAction,
    val previousStatus: SubscriptionStatus,
    val changedStatus: SubscriptionStatus,
    val changedAt: Instant,
)
