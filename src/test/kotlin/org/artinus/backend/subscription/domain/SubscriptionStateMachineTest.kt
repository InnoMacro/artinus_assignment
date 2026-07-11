package org.artinus.backend.subscription.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class SubscriptionStateMachineTest {
    @ParameterizedTest
    @MethodSource("allowedTransitions")
    fun `허용된 상태로 전이한다`(
        current: SubscriptionStatus,
        action: SubscriptionAction,
        target: SubscriptionStatus,
    ) {
        assertEquals(target, SubscriptionStateMachine.transition(current, action, target))
    }

    @ParameterizedTest
    @MethodSource("rejectedTransitions")
    fun `허용되지 않은 상태 전이를 거절한다`(
        current: SubscriptionStatus,
        action: SubscriptionAction,
        target: SubscriptionStatus,
    ) {
        assertThrows(InvalidSubscriptionTransitionException::class.java) {
            SubscriptionStateMachine.transition(current, action, target)
        }
    }

    companion object {
        @JvmStatic
        fun allowedTransitions(): Stream<Arguments> =
            Stream.of(
                Arguments.of(SubscriptionStatus.NONE, SubscriptionAction.SUBSCRIBE, SubscriptionStatus.BASIC),
                Arguments.of(SubscriptionStatus.NONE, SubscriptionAction.SUBSCRIBE, SubscriptionStatus.PREMIUM),
                Arguments.of(SubscriptionStatus.BASIC, SubscriptionAction.SUBSCRIBE, SubscriptionStatus.PREMIUM),
                Arguments.of(SubscriptionStatus.PREMIUM, SubscriptionAction.UNSUBSCRIBE, SubscriptionStatus.BASIC),
                Arguments.of(SubscriptionStatus.PREMIUM, SubscriptionAction.UNSUBSCRIBE, SubscriptionStatus.NONE),
                Arguments.of(SubscriptionStatus.BASIC, SubscriptionAction.UNSUBSCRIBE, SubscriptionStatus.NONE),
            )

        @JvmStatic
        fun rejectedTransitions(): Stream<Arguments> =
            Stream.of(
                Arguments.of(SubscriptionStatus.PREMIUM, SubscriptionAction.SUBSCRIBE, SubscriptionStatus.PREMIUM),
                Arguments.of(SubscriptionStatus.BASIC, SubscriptionAction.SUBSCRIBE, SubscriptionStatus.NONE),
                Arguments.of(SubscriptionStatus.NONE, SubscriptionAction.UNSUBSCRIBE, SubscriptionStatus.NONE),
                Arguments.of(SubscriptionStatus.BASIC, SubscriptionAction.UNSUBSCRIBE, SubscriptionStatus.PREMIUM),
            )
    }
}
