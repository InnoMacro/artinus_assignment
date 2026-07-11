package org.artinus.backend.channel.adapter.outbound.persistence

import org.artinus.backend.channel.application.port.outbound.ChannelRepository
import org.artinus.backend.channel.domain.Channel
import org.artinus.backend.channel.domain.ChannelId
import org.springframework.stereotype.Repository

@Repository
class ChannelPersistenceAdapter(
    private val jpaRepository: ChannelJpaRepository,
) : ChannelRepository {
    override fun findById(id: ChannelId): Channel? =
        jpaRepository.findById(id.value)
            .map(ChannelJpaEntity::toDomain)
            .orElse(null)
}
