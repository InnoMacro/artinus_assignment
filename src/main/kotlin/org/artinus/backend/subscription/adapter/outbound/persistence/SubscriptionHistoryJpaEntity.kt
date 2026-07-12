package org.artinus.backend.subscription.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.subscription.domain.vo.MemberId
import org.artinus.backend.subscription.domain.vo.SubscriptionAction
import org.artinus.backend.subscription.domain.SubscriptionHistory
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import java.time.Instant

@Entity
@Table(name = "subscription_history")
class SubscriptionHistoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "channel_id", nullable = false)
    val channelId: Long,
    @Convert(converter = SubscriptionActionConverter::class)
    @Column(
        nullable = false,
        columnDefinition = "TINYINT",
        comment = "구독 행위: 0=SUBSCRIBE, 1=UNSUBSCRIBE",
    )
    val action: SubscriptionAction,
    @Convert(converter = SubscriptionStatusConverter::class)
    @Column(
        name = "previous_status",
        nullable = false,
        columnDefinition = "TINYINT",
        comment = "변경 전 구독 상태: 0=NONE, 1=BASIC, 2=PREMIUM",
    )
    val previousStatus: SubscriptionStatus,
    @Convert(converter = SubscriptionStatusConverter::class)
    @Column(
        name = "changed_status",
        nullable = false,
        columnDefinition = "TINYINT",
        comment = "변경 후 구독 상태: 0=NONE, 1=BASIC, 2=PREMIUM",
    )
    val changedStatus: SubscriptionStatus,
    @Column(name = "changed_at", nullable = false)
    val changedAt: Instant,
) {
    fun toDomain(): SubscriptionHistory =
        SubscriptionHistory(
            id = id,
            memberId = MemberId(memberId),
            channelId = ChannelId(channelId),
            action = action,
            previousStatus = previousStatus,
            changedStatus = changedStatus,
            changedAt = changedAt,
        )

    companion object {
        fun from(domain: SubscriptionHistory): SubscriptionHistoryJpaEntity =
            SubscriptionHistoryJpaEntity(
                id = domain.id,
                memberId = domain.memberId.value,
                channelId = domain.channelId.value,
                action = domain.action,
                previousStatus = domain.previousStatus,
                changedStatus = domain.changedStatus,
                changedAt = domain.changedAt,
            )
    }
}
