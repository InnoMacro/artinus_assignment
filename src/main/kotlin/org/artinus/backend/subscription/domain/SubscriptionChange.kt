package org.artinus.backend.subscription.domain

data class SubscriptionChange(
    val action: SubscriptionAction,
    val previousStatus: SubscriptionStatus,
    val changedStatus: SubscriptionStatus,
)
