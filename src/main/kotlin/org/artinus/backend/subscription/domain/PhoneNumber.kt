package org.artinus.backend.subscription.domain

@JvmInline
value class PhoneNumber private constructor(val value: String) {
    constructor(raw: CharSequence) : this(normalize(raw.toString()))

    companion object {
        private val pattern = Regex("^01[016789]\\d{7,8}$")

        private fun normalize(raw: String): String =
            raw.filter(Char::isDigit).also {
                require(pattern.matches(it)) { "유효하지 않은 휴대폰 번호입니다." }
            }
    }
}
