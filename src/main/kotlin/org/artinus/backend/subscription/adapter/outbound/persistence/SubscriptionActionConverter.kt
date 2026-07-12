package org.artinus.backend.subscription.adapter.outbound.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.artinus.backend.subscription.domain.vo.SubscriptionAction

@Converter(autoApply = false)
class SubscriptionActionConverter : AttributeConverter<SubscriptionAction, Byte> {
    override fun convertToDatabaseColumn(attribute: SubscriptionAction?): Byte? =
        when (attribute) {
            null -> null
            SubscriptionAction.SUBSCRIBE -> 0
            SubscriptionAction.UNSUBSCRIBE -> 1
        }

    override fun convertToEntityAttribute(dbData: Byte?): SubscriptionAction? =
        when (dbData) {
            null -> null
            0.toByte() -> SubscriptionAction.SUBSCRIBE
            1.toByte() -> SubscriptionAction.UNSUBSCRIBE
            else -> throw IllegalArgumentException("지원하지 않는 구독 행위 코드입니다: $dbData")
        }
}
