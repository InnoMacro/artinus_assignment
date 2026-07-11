package org.artinus.backend.channel.application.port.outbound

import org.artinus.backend.channel.domain.Channel
import org.artinus.backend.channel.domain.ChannelId

interface ChannelRepository {
    fun findById(id: ChannelId): Channel?
}
