package org.artinus.backend.subscription.domain.vo

import org.artinus.backend.subscription.domain.exception.InvalidPhoneNumberException

@JvmInline
value class PhoneNumber private constructor(val value: String) {
    constructor(raw: CharSequence) : this(normalize(raw.toString()))

    fun masked(): String = "${value.take(3)}****${value.takeLast(4)}"

    companion object {
        private val pattern = Regex("^01[016789]\\d{7,8}$")

        private fun normalize(raw: String): String {
            val normalized = raw.filter(Char::isDigit)
            if (!pattern.matches(normalized)) {
                throw InvalidPhoneNumberException()
            }
            return normalized
        }
    }
}
