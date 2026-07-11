package org.artinus.backend.subscription.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PhoneNumberTest {
    @Test
    fun `하이픈이 포함된 휴대폰 번호를 숫자 형식으로 정규화한다`() {
        val phoneNumber = PhoneNumber("010-1234-5678")

        assertEquals("01012345678", phoneNumber.value)
    }

    @Test
    fun `유효하지 않은 휴대폰 번호는 생성할 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            PhoneNumber("02-123-4567")
        }
    }
}
