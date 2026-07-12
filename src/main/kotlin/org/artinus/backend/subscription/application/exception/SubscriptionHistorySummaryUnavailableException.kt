package org.artinus.backend.subscription.application.exception

class SubscriptionHistorySummaryUnavailableException(
    message: String = "구독 이력 요약을 생성할 수 없습니다.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
