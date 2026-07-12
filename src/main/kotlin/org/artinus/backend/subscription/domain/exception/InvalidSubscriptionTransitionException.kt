package org.artinus.backend.subscription.domain.exception

import org.artinus.backend.subscription.domain.vo.SubscriptionAction
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus

class InvalidSubscriptionTransitionException(
    current: SubscriptionStatus,
    action: SubscriptionAction,
    target: SubscriptionStatus,
) : SubscriptionBusinessException("허용되지 않은 구독 상태 전이입니다: $current --$action--> $target")
