package org.artinus.backend.subscription.adapter.outbound.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus

@Converter(autoApply = false)
class SubscriptionStatusConverter : AttributeConverter<SubscriptionStatus, Byte> {
    override fun convertToDatabaseColumn(attribute: SubscriptionStatus?): Byte? =
        when (attribute) {
            null -> null
            SubscriptionStatus.NONE -> 0
            SubscriptionStatus.BASIC -> 1
            SubscriptionStatus.PREMIUM -> 2
        }

    override fun convertToEntityAttribute(dbData: Byte?): SubscriptionStatus? =
        when (dbData) {
            null -> null
            0.toByte() -> SubscriptionStatus.NONE
            1.toByte() -> SubscriptionStatus.BASIC
            2.toByte() -> SubscriptionStatus.PREMIUM
            else -> throw IllegalArgumentException("지원하지 않는 구독 상태 코드입니다: $dbData")
        }
}
