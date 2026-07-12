package org.artinus.backend.subscription.application.exception

class SubscriptionChangeConflictException(
    cause: Throwable? = null,
) : RuntimeException("다른 구독 변경 요청과 충돌했습니다.", cause)
