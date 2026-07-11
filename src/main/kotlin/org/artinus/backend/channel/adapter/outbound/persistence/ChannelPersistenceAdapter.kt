package org.artinus.backend.channel.adapter.outbound.persistence

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.artinus.backend.channel.application.port.outbound.ChannelRepository
import org.artinus.backend.channel.domain.Channel
import org.artinus.backend.channel.domain.ChannelId
import org.springframework.stereotype.Repository

@Repository
class ChannelPersistenceAdapter(
    entityManager: EntityManager,
) : ChannelRepository {
    private val queryFactory = JPAQueryFactory(entityManager)
    private val channel = QChannelJpaEntity.channelJpaEntity

    override fun findById(id: ChannelId): Channel? =
        queryFactory
            .selectFrom(channel)
            .where(channel.id.eq(id.value))
            .fetchOne()
            ?.toDomain()
}
