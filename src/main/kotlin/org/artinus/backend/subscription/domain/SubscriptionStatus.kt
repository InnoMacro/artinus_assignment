package org.artinus.backend.subscription.domain

enum class SubscriptionStatus(val dbCode: Byte) {
    NONE(0),
    BASIC(1),
    PREMIUM(2),
    ;

    companion object {
        fun fromDbCode(code: Byte): SubscriptionStatus =
            entries.firstOrNull { it.dbCode == code }
                ?: throw IllegalArgumentException("지원하지 않는 구독 상태 코드입니다: $code")
    }
}
