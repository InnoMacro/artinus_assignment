package org.artinus.backend.subscription.domain.vo

@JvmInline
value class MemberId(val value: Long) {
    init {
        require(value > 0) { "회원 ID는 양수여야 합니다." }
    }
}
