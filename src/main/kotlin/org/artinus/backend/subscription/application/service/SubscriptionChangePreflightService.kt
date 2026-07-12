package org.artinus.backend.subscription.application.service

import org.artinus.backend.channel.application.port.outbound.ChannelRepository
import org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubscriptionChangePreflightService(
    private val memberRepository: SubscriptionMemberRepository,
    private val channelRepository: ChannelRepository,
) {
    @Transactional(readOnly = true)
    fun validateSubscribe(command: ChangeSubscriptionCommand): SubscriptionChangePreflight {
        channelRepository.getById(command.channelId).requireSubscribable()

        val existingMember = memberRepository.findByPhoneNumber(command.phoneNumber)
        val member = existingMember ?: SubscriptionMember.new(command.phoneNumber)
        member.subscribe(command.targetStatus)

        return SubscriptionChangePreflight(memberExisted = existingMember != null)
    }

    @Transactional(readOnly = true)
    fun validateUnsubscribe(command: ChangeSubscriptionCommand) {
        channelRepository.getById(command.channelId).requireUnsubscribable()

        val member =
            memberRepository.findByPhoneNumber(command.phoneNumber)
                ?: throw SubscriptionMemberNotFoundException(command.phoneNumber)
        member.unsubscribe(command.targetStatus)
    }
}
