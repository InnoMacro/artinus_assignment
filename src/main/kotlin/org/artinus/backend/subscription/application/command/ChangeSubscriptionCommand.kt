package org.artinus.backend.subscription.application.command

import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionStatus

data class ChangeSubscriptionCommand(
    val phoneNumber: PhoneNumber,
    val channelId: ChannelId,
    val targetStatus: SubscriptionStatus,
)
