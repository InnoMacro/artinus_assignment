package org.artinus.backend.common.error

import org.artinus.backend.channel.application.exception.ChannelNotFoundException
import org.artinus.backend.channel.domain.exception.ChannelActionNotAllowedException
import org.artinus.backend.channel.domain.exception.ChannelBusinessException
import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalRejectedException
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.domain.exception.InvalidPhoneNumberException
import org.artinus.backend.subscription.domain.exception.InvalidSubscriptionTransitionException
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.artinus.backend.subscription.domain.vo.SubscriptionAction
import org.artinus.backend.subscription.domain.exception.SubscriptionBusinessException
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BusinessExceptionHierarchyTest {
    @Test
    fun `channel 예외는 공통 BusinessException을 구현한 channel 계층에 속한다`() {
        val exceptions: List<Throwable> =
            listOf(
                ChannelNotFoundException(ChannelId(1)),
                ChannelActionNotAllowedException("허용되지 않은 채널 행위"),
            )

        assertTrue(exceptions.all { it is BusinessException && it is ChannelBusinessException })
    }

    @Test
    fun `subscription 예외는 공통 BusinessException을 구현한 subscription 계층에 속한다`() {
        val exceptions: List<Throwable> =
            listOf(
                InvalidPhoneNumberException(),
                InvalidSubscriptionTransitionException(
                    SubscriptionStatus.NONE,
                    SubscriptionAction.UNSUBSCRIBE,
                    SubscriptionStatus.NONE,
                ),
                SubscriptionMemberNotFoundException(PhoneNumber("01012345678")),
                SubscriptionApprovalRejectedException(),
            )

        assertTrue(exceptions.all { it is BusinessException && it is SubscriptionBusinessException })
    }
}
