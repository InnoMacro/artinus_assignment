package org.artinus.backend.subscription.application.service

import org.artinus.backend.channel.application.port.outbound.ChannelRepository
import org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand
import org.artinus.backend.subscription.application.exception.SubscriptionChangeConflictException
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryRepository
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.application.result.ChangeSubscriptionResult
import org.artinus.backend.subscription.domain.SubscriptionChange
import org.artinus.backend.subscription.domain.SubscriptionHistory
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.artinus.backend.subscription.domain.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class SubscriptionChangeTransactionService(
    private val memberRepository: SubscriptionMemberRepository,
    private val historyRepository: SubscriptionHistoryRepository,
    private val channelRepository: ChannelRepository,
    private val clock: Clock,
) {
    @Transactional
    fun subscribe(
        command: ChangeSubscriptionCommand,
        preflight: SubscriptionChangePreflight,
    ): ChangeSubscriptionResult {
        channelRepository.getById(command.channelId).requireSubscribable()

        val currentMember = memberRepository.findByPhoneNumberForUpdate(command.phoneNumber)
        if (!preflight.memberExisted && currentMember != null) {
            throw SubscriptionChangeConflictException()
        }
        val member = currentMember ?: SubscriptionMember.new(command.phoneNumber)
        return save(member, command, member.subscribe(command.targetStatus))
    }

    @Transactional
    fun unsubscribe(command: ChangeSubscriptionCommand): ChangeSubscriptionResult {
        channelRepository.getById(command.channelId).requireUnsubscribable()

        val member = memberRepository.getByPhoneNumberForUpdate(command.phoneNumber)
        return save(member, command, member.unsubscribe(command.targetStatus))
    }

    private fun save(
        member: SubscriptionMember,
        command: ChangeSubscriptionCommand,
        change: SubscriptionChange,
    ): ChangeSubscriptionResult {
        val savedMember = memberRepository.save(member)
        historyRepository.save(change.toHistory(requireNotNull(savedMember.id), command, clock.instant()))

        return ChangeSubscriptionResult(
            memberId = requireNotNull(savedMember.id),
            phoneNumber = savedMember.phoneNumber,
            status = savedMember.status,
        )
    }

    private fun SubscriptionChange.toHistory(
        memberId: MemberId,
        command: ChangeSubscriptionCommand,
        changedAt: Instant,
    ): SubscriptionHistory =
        SubscriptionHistory(
            memberId = memberId,
            channelId = command.channelId,
            action = action,
            previousStatus = previousStatus,
            changedStatus = changedStatus,
            changedAt = changedAt,
        )
}
