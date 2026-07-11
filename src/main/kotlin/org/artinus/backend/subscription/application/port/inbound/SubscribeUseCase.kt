package org.artinus.backend.subscription.application.port.inbound

import org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand
import org.artinus.backend.subscription.application.result.ChangeSubscriptionResult

fun interface SubscribeUseCase {
    fun subscribe(command: ChangeSubscriptionCommand): ChangeSubscriptionResult
}
