package org.artinus.backend.subscription.adapter.outbound.persistence

import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SubscriptionStatusConverterTest {
    private val converter = SubscriptionStatusConverter()

    @Test
    fun `상태 Enum을 고정된 tinyint 코드로 변환한다`() {
        assertEquals(0.toByte(), converter.convertToDatabaseColumn(SubscriptionStatus.NONE))
        assertEquals(1.toByte(), converter.convertToDatabaseColumn(SubscriptionStatus.BASIC))
        assertEquals(2.toByte(), converter.convertToDatabaseColumn(SubscriptionStatus.PREMIUM))
    }

    @Test
    fun `tinyint 코드를 상태 Enum으로 복원한다`() {
        assertEquals(SubscriptionStatus.NONE, converter.convertToEntityAttribute(0))
        assertEquals(SubscriptionStatus.BASIC, converter.convertToEntityAttribute(1))
        assertEquals(SubscriptionStatus.PREMIUM, converter.convertToEntityAttribute(2))
    }

    @Test
    fun `null은 null로 변환한다`() {
        assertEquals(null, converter.convertToDatabaseColumn(null))
        assertEquals(null, converter.convertToEntityAttribute(null))
    }

    @Test
    fun `정의하지 않은 tinyint 코드는 거절한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.convertToEntityAttribute(9)
        }
    }
}
