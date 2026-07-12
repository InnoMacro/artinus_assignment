package org.artinus.backend.subscription.adapter.outbound.persistence

import org.artinus.backend.subscription.domain.vo.SubscriptionAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SubscriptionActionConverterTest {
    private val converter = SubscriptionActionConverter()

    @Test
    fun `행위 Enum을 고정된 tinyint 코드로 변환한다`() {
        assertEquals(0.toByte(), converter.convertToDatabaseColumn(SubscriptionAction.SUBSCRIBE))
        assertEquals(1.toByte(), converter.convertToDatabaseColumn(SubscriptionAction.UNSUBSCRIBE))
    }

    @Test
    fun `tinyint 코드를 행위 Enum으로 복원한다`() {
        assertEquals(SubscriptionAction.SUBSCRIBE, converter.convertToEntityAttribute(0))
        assertEquals(SubscriptionAction.UNSUBSCRIBE, converter.convertToEntityAttribute(1))
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
