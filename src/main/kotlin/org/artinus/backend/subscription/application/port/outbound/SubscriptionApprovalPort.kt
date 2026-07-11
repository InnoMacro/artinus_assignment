package org.artinus.backend.subscription.application.port.outbound

fun interface SubscriptionApprovalPort {
    fun requestApproval(): ApprovalDecision
}
