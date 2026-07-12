package org.artinus.backend.subscription.adapter.inbound.web

import jakarta.validation.Valid
import org.artinus.backend.subscription.application.port.inbound.GetSubscriptionHistoryUseCase
import org.artinus.backend.subscription.application.port.inbound.SubscribeUseCase
import org.artinus.backend.subscription.application.port.inbound.UnsubscribeUseCase
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionController(
    private val subscribeUseCase: SubscribeUseCase,
    private val unsubscribeUseCase: UnsubscribeUseCase,
    private val getSubscriptionHistoryUseCase: GetSubscriptionHistoryUseCase,
) {
    @PostMapping
    fun subscribe(
        @Valid @RequestBody request: ChangeSubscriptionRequest,
    ): ResponseEntity<ChangeSubscriptionResponse> =
        ResponseEntity.ok(
            ChangeSubscriptionResponse.from(subscribeUseCase.subscribe(request.toCommand())),
        )

    @PostMapping("/unsubscribe")
    fun unsubscribe(
        @Valid @RequestBody request: ChangeSubscriptionRequest,
    ): ResponseEntity<ChangeSubscriptionResponse> =
        ResponseEntity.ok(
            ChangeSubscriptionResponse.from(unsubscribeUseCase.unsubscribe(request.toCommand())),
        )

    @GetMapping("/{phoneNumber}/histories")
    fun getHistory(
        @PathVariable phoneNumber: String,
    ): ResponseEntity<SubscriptionHistoryResponse> =
        ResponseEntity.ok(
            SubscriptionHistoryResponse.from(
                getSubscriptionHistoryUseCase.getHistory(PhoneNumber(phoneNumber)),
            ),
        )
}
