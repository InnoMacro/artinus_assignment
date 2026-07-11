package org.artinus.backend.channel.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.artinus.backend.channel.domain.Channel
import org.artinus.backend.channel.domain.ChannelId

@Entity
@Table(name = "channel")
class ChannelJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true, length = 30)
    val code: String,
    @Column(nullable = false, length = 50)
    val name: String,
    @Column(nullable = false)
    val subscribable: Boolean,
    @Column(nullable = false)
    val unsubscribable: Boolean,
) {
    fun toDomain(): Channel =
        Channel(
            id = ChannelId(requireNotNull(id) { "저장되지 않은 채널 Entity입니다." }),
            code = code,
            name = name,
            subscribable = subscribable,
            unsubscribable = unsubscribable,
        )
}
