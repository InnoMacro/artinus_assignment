package org.artinus.backend.channel.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ChannelJpaRepository : JpaRepository<ChannelJpaEntity, Long>
