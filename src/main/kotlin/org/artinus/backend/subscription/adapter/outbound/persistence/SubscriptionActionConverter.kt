package org.artinus.backend.subscription.adapter.outbound.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.artinus.backend.subscription.domain.SubscriptionAction

@Converter(autoApply = false)
class SubscriptionActionConverter : AttributeConverter<SubscriptionAction, Byte> {
    override fun convertToDatabaseColumn(attribute: SubscriptionAction?): Byte? = attribute?.dbCode

    override fun convertToEntityAttribute(dbData: Byte?): SubscriptionAction? =
        dbData?.let(SubscriptionAction::fromDbCode)
}
