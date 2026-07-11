package org.artinus.backend.subscription.domain

enum class SubscriptionAction(val dbCode: Byte) {
    SUBSCRIBE(0),
    UNSUBSCRIBE(1),
    ;

    companion object {
        fun fromDbCode(code: Byte): SubscriptionAction =
            entries.firstOrNull { it.dbCode == code }
                ?: throw IllegalArgumentException("지원하지 않는 구독 행위 코드입니다: $code")
    }
}
