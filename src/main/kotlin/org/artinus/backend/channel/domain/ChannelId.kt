package org.artinus.backend.channel.domain

@JvmInline
value class ChannelId(val value: Long) {
    init {
        require(value > 0) { "채널 ID는 양수여야 합니다." }
    }
}
