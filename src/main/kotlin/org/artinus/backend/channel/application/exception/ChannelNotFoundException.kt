package org.artinus.backend.channel.application.exception

import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.channel.domain.exception.ChannelBusinessException

class ChannelNotFoundException(
    val channelId: ChannelId,
) : ChannelBusinessException("채널을 찾을 수 없습니다. channelId=${channelId.value}")
