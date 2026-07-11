package org.artinus.backend.subscription.application.service

import org.artinus.backend.channel.application.port.outbound.ChannelRepository
import org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand
import org.artinus.backend.subscription.application.port.inbound.SubscribeUseCase
import org.artinus.backend.subscription.application.port.inbound.UnsubscribeUseCase
import org.artinus.backend.subscription.application.port.outbound.ApprovalDecision
import org.artinus.backend.subscription.application.port.outbound.SubscriptionApprovalPort
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryRepository
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.application.result.ChangeSubscriptionResult
import org.artinus.backend.subscription.domain.MemberId
import org.artinus.backend.subscription.domain.SubscriptionChange
import org.artinus.backend.subscription.domain.SubscriptionHistory
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class ChangeSubscriptionService(
    private val memberRepository: SubscriptionMemberRepository,
    private val historyRepository: SubscriptionHistoryRepository,
    private val channelRepository: ChannelRepository,
    private val approvalPort: SubscriptionApprovalPort,
    private val clock: Clock,
) : SubscribeUseCase,
    UnsubscribeUseCase {
    @Transactional
    override fun subscribe(command: ChangeSubscriptionCommand): ChangeSubscriptionResult {
        val channel = channelRepository.findById(command.channelId) ?: throw ChannelNotFoundException()
        channel.requireSubscribable()

        val member =
            memberRepository.findByPhoneNumberForUpdate(command.phoneNumber)
                ?: SubscriptionMember.new(command.phoneNumber)
        val change = member.subscribe(command.targetStatus)

        return approveAndSave(member, command, change)
    }

    @Transactional
    override fun unsubscribe(command: ChangeSubscriptionCommand): ChangeSubscriptionResult {
        val channel = channelRepository.findById(command.channelId) ?: throw ChannelNotFoundException()
        channel.requireUnsubscribable()

        val member =
            memberRepository.findByPhoneNumberForUpdate(command.phoneNumber)
                ?: throw SubscriptionMemberNotFoundException()
        val change = member.unsubscribe(command.targetStatus)

        return approveAndSave(member, command, change)
    }

    private fun approveAndSave(
        member: SubscriptionMember,
        command: ChangeSubscriptionCommand,
        change: SubscriptionChange,
    ): ChangeSubscriptionResult {
        if (approvalPort.requestApproval() == ApprovalDecision.REJECTED) {
            throw SubscriptionApprovalRejectedException()
        }

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
