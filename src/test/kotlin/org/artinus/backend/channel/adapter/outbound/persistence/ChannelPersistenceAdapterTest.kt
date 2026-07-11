package org.artinus.backend.channel.adapter.outbound.persistence

import org.artinus.backend.channel.application.exception.ChannelNotFoundException
import org.artinus.backend.channel.domain.ChannelId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.Optional

class ChannelPersistenceAdapterTest {
    private val jpaRepository = Mockito.mock(ChannelJpaRepository::class.java)
    private val adapter = ChannelPersistenceAdapter(jpaRepository)

    @Test
    fun `필수 채널 ID가 없으면 식별자를 포함한 예외를 발생시킨다`() {
        Mockito.`when`(jpaRepository.findById(99L)).thenReturn(Optional.empty())

        val exception =
            assertThrows(ChannelNotFoundException::class.java) {
                adapter.getById(ChannelId(99L))
            }

        assertEquals(ChannelId(99L), exception.channelId)
        assertEquals("채널을 찾을 수 없습니다. channelId=99", exception.message)
    }
}
