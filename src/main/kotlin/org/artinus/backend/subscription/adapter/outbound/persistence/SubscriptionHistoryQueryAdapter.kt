package org.artinus.backend.subscription.adapter.outbound.persistence

import com.querydsl.jpa.impl.JPAQueryFactory
import org.artinus.backend.channel.adapter.outbound.persistence.QChannelJpaEntity
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryQueryPort
import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.domain.PhoneNumber
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class SubscriptionHistoryQueryAdapter(
    private val queryFactory: JPAQueryFactory,
) : SubscriptionHistoryQueryPort {
    private val member = QSubscriptionMemberJpaEntity.subscriptionMemberJpaEntity
    private val history = QSubscriptionHistoryJpaEntity.subscriptionHistoryJpaEntity
    private val channel = QChannelJpaEntity.channelJpaEntity

    @Transactional(readOnly = true)
    override fun findAllByPhoneNumber(phoneNumber: PhoneNumber): List<SubscriptionHistoryItem> {
        val memberId =
            queryFactory.select(member.id)
                .from(member)
                .where(member.phoneNumber.eq(phoneNumber.value))
                .fetchOne()
                ?: throw SubscriptionMemberNotFoundException(phoneNumber)

        return queryFactory
            .select(history, channel.name)
            .from(history)
            .join(channel).on(channel.id.eq(history.channelId))
            .where(history.memberId.eq(memberId))
            .orderBy(history.changedAt.asc(), history.id.asc())
            .fetch()
            .map { row ->
                val entity = requireNotNull(row.get(history))
                SubscriptionHistoryItem(
                    id = requireNotNull(entity.id),
                    channelName = requireNotNull(row.get(channel.name)),
                    action = entity.action,
                    previousStatus = entity.previousStatus,
                    changedStatus = entity.changedStatus,
                    changedAt = entity.changedAt,
                )
            }
    }
}
