package org.artinus.backend.subscription.application.result

import org.artinus.backend.subscription.domain.MemberId
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionStatus

data class ChangeSubscriptionResult(
    val memberId: MemberId,
    val phoneNumber: PhoneNumber,
    val status: SubscriptionStatus,
)
