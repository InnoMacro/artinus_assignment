package org.artinus.backend.subscription.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionHistoryJpaRepository : JpaRepository<SubscriptionHistoryJpaEntity, Long>
