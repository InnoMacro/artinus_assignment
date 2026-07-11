package org.artinus.backend.subscription.adapter.inbound.web

import org.artinus.backend.subscription.application.result.ChangeSubscriptionResult
import org.artinus.backend.subscription.domain.SubscriptionStatus

data class ChangeSubscriptionResponse(
    val memberId: Long,
    val phoneNumber: String,
    val status: SubscriptionStatus,
) {
    companion object {
        fun from(result: ChangeSubscriptionResult): ChangeSubscriptionResponse =
            ChangeSubscriptionResponse(
                memberId = result.memberId.value,
                phoneNumber = result.phoneNumber.value,
                status = result.status,
            )
    }
}
