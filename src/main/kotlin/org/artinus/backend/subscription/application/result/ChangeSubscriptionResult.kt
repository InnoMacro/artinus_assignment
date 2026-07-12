package org.artinus.backend.subscription.application.result

import org.artinus.backend.subscription.domain.vo.MemberId
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus

data class ChangeSubscriptionResult(
    val memberId: MemberId,
    val phoneNumber: PhoneNumber,
    val status: SubscriptionStatus,
)
