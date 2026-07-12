package org.artinus.backend.subscription.application.exception

class SubscriptionApprovalUnavailableException(
    cause: Throwable? = null,
) : RuntimeException("외부 승인 서비스를 사용할 수 없습니다.", cause)
