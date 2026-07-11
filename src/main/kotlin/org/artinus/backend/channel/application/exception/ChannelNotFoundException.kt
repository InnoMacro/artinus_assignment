package org.artinus.backend.channel.application.exception

import org.artinus.backend.channel.domain.ChannelId

class ChannelNotFoundException(
    val channelId: ChannelId,
) : RuntimeException("채널을 찾을 수 없습니다. channelId=${channelId.value}")
