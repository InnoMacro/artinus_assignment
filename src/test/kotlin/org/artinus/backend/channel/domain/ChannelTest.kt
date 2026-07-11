package org.artinus.backend.channel.domain

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ChannelTest {
    @Test
    fun `구독 가능 채널은 구독 요청을 허용한다`() {
        val channel = Channel(ChannelId(1L), "WEB", "홈페이지", subscribable = true, unsubscribable = true)

        assertDoesNotThrow { channel.requireSubscribable() }
    }

    @Test
    fun `해지 불가능 채널은 해지 요청을 거절한다`() {
        val channel = Channel(ChannelId(1L), "NAVER", "네이버", subscribable = true, unsubscribable = false)

        assertThrows(ChannelActionNotAllowedException::class.java) {
            channel.requireUnsubscribable()
        }
    }
}
