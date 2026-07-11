package org.artinus.backend.subscription.domain

class InvalidSubscriptionTransitionException(
    current: SubscriptionStatus,
    action: SubscriptionAction,
    target: SubscriptionStatus,
) : RuntimeException("허용되지 않은 구독 상태 전이입니다: $current --$action--> $target")
