package org.artinus.backend.subscription.domain

import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.subscription.domain.vo.MemberId
import org.artinus.backend.subscription.domain.vo.SubscriptionAction
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
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
