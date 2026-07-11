package org.artinus.backend.subscription.adapter.outbound.persistence

import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryRepository
import org.artinus.backend.subscription.domain.SubscriptionHistory
import org.springframework.stereotype.Repository

@Repository
class SubscriptionHistoryPersistenceAdapter(
    private val jpaRepository: SubscriptionHistoryJpaRepository,
) : SubscriptionHistoryRepository {
    override fun save(history: SubscriptionHistory): SubscriptionHistory {
        return jpaRepository.save(SubscriptionHistoryJpaEntity.from(history)).toDomain()
    }
}
