package org.artinus.backend.subscription.domain

object SubscriptionStateMachine {
    private data class TransitionKey(
        val current: SubscriptionStatus,
        val action: SubscriptionAction,
    )

    private val transitions =
        mapOf(
            TransitionKey(SubscriptionStatus.NONE, SubscriptionAction.SUBSCRIBE) to
                setOf(SubscriptionStatus.BASIC, SubscriptionStatus.PREMIUM),
            TransitionKey(SubscriptionStatus.BASIC, SubscriptionAction.SUBSCRIBE) to
                setOf(SubscriptionStatus.PREMIUM),
            TransitionKey(SubscriptionStatus.PREMIUM, SubscriptionAction.UNSUBSCRIBE) to
                setOf(SubscriptionStatus.BASIC, SubscriptionStatus.NONE),
            TransitionKey(SubscriptionStatus.BASIC, SubscriptionAction.UNSUBSCRIBE) to
                setOf(SubscriptionStatus.NONE),
        )

    fun transition(
        current: SubscriptionStatus,
        action: SubscriptionAction,
        target: SubscriptionStatus,
    ): SubscriptionStatus {
        if (target !in transitions[TransitionKey(current, action)].orEmpty()) {
            throw InvalidSubscriptionTransitionException(current, action, target)
        }
        return target
    }
}
