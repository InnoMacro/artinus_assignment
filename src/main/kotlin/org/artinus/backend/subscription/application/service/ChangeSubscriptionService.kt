package org.artinus.backend.subscription.application.service

import org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalRejectedException
import org.artinus.backend.subscription.application.port.inbound.SubscribeUseCase
import org.artinus.backend.subscription.application.port.inbound.UnsubscribeUseCase
import org.artinus.backend.subscription.application.port.outbound.ApprovalDecision
import org.artinus.backend.subscription.application.port.outbound.SubscriptionApprovalPort
import org.artinus.backend.subscription.application.result.ChangeSubscriptionResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.NEVER)
class ChangeSubscriptionService(
    private val preflightService: SubscriptionChangePreflightService,
    private val transactionService: SubscriptionChangeTransactionService,
    private val approvalPort: SubscriptionApprovalPort,
) : SubscribeUseCase,
    UnsubscribeUseCase {
    override fun subscribe(command: ChangeSubscriptionCommand): ChangeSubscriptionResult {
        val preflight = preflightService.validateSubscribe(command)
        requireApproval()
        return transactionService.subscribe(command, preflight)
    }

    override fun unsubscribe(command: ChangeSubscriptionCommand): ChangeSubscriptionResult {
        preflightService.validateUnsubscribe(command)
        requireApproval()
        return transactionService.unsubscribe(command)
    }

    private fun requireApproval() {
        if (approvalPort.requestApproval() == ApprovalDecision.REJECTED) {
            throw SubscriptionApprovalRejectedException()
        }
    }
}
