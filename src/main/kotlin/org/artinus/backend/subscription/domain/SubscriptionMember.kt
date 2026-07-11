package org.artinus.backend.subscription.domain

class SubscriptionMember private constructor(
    val id: MemberId?,
    val phoneNumber: PhoneNumber,
    status: SubscriptionStatus,
) {
    var status: SubscriptionStatus = status
        private set

    fun subscribe(target: SubscriptionStatus): SubscriptionChange =
        change(SubscriptionAction.SUBSCRIBE, target)

    fun unsubscribe(target: SubscriptionStatus): SubscriptionChange =
        change(SubscriptionAction.UNSUBSCRIBE, target)

    private fun change(
        action: SubscriptionAction,
        target: SubscriptionStatus,
    ): SubscriptionChange {
        val previous = status
        status = SubscriptionStateMachine.transition(previous, action, target)
        return SubscriptionChange(action, previous, status)
    }

    companion object {
        fun new(phoneNumber: PhoneNumber): SubscriptionMember =
            SubscriptionMember(null, phoneNumber, SubscriptionStatus.NONE)

        fun restore(
            id: MemberId,
            phoneNumber: PhoneNumber,
            status: SubscriptionStatus,
        ): SubscriptionMember = SubscriptionMember(id, phoneNumber, status)
    }
}
