package org.artinus.backend.subscription.application.port.inbound

import org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand
import org.artinus.backend.subscription.application.result.ChangeSubscriptionResult

/** 활성 transaction 밖에서 호출해야 하는 구독 해지 진입점이다. */
fun interface UnsubscribeUseCase {
    fun unsubscribe(command: ChangeSubscriptionCommand): ChangeSubscriptionResult
}
