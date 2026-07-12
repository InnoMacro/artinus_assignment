package org.artinus.backend.subscription.application.exception

class SubscriptionApprovalInvalidResponseException(
    cause: Throwable? = null,
) : RuntimeException("외부 승인 서비스 응답을 처리할 수 없습니다.", cause)
