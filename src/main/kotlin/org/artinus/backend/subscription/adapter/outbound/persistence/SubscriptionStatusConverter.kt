package org.artinus.backend.subscription.adapter.outbound.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.artinus.backend.subscription.domain.SubscriptionStatus

@Converter(autoApply = false)
class SubscriptionStatusConverter : AttributeConverter<SubscriptionStatus, Byte> {
    override fun convertToDatabaseColumn(attribute: SubscriptionStatus?): Byte? = attribute?.dbCode

    override fun convertToEntityAttribute(dbData: Byte?): SubscriptionStatus? =
        dbData?.let(SubscriptionStatus::fromDbCode)
}
