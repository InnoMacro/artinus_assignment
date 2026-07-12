package org.artinus.backend.channel.domain

import org.artinus.backend.channel.domain.exception.ChannelActionNotAllowedException

class Channel(
    val id: ChannelId,
    val code: String,
    val name: String,
    private val subscribable: Boolean,
    private val unsubscribable: Boolean,
) {
    fun requireSubscribable() {
        if (!subscribable) {
            throw ChannelActionNotAllowedException("구독할 수 없는 채널입니다: $code")
        }
    }

    fun requireUnsubscribable() {
        if (!unsubscribable) {
            throw ChannelActionNotAllowedException("해지할 수 없는 채널입니다: $code")
        }
    }
}
