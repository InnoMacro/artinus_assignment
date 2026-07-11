package org.artinus.backend.subscription.adapter.outbound.persistence

import jakarta.persistence.EntityManager
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryRepository
import org.artinus.backend.subscription.domain.SubscriptionHistory
import org.springframework.stereotype.Repository

@Repository
class SubscriptionHistoryPersistenceAdapter(
    private val entityManager: EntityManager,
) : SubscriptionHistoryRepository {
    override fun save(history: SubscriptionHistory): SubscriptionHistory {
        val entity = SubscriptionHistoryJpaEntity.from(history)
        entityManager.persist(entity)
        return entity.toDomain()
    }
}
