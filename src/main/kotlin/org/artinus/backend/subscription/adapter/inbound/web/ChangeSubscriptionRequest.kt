package org.artinus.backend.subscription.adapter.inbound.web

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionStatus

data class ChangeSubscriptionRequest(
    @field:NotBlank(message = "휴대폰 번호는 필수입니다.")
    @field:Pattern(
        regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
        message = "유효한 휴대폰 번호 형식이어야 합니다.",
    )
    val phoneNumber: String?,
    @field:NotNull(message = "채널 ID는 필수입니다.")
    @field:Positive(message = "채널 ID는 양수여야 합니다.")
    val channelId: Long?,
    @field:NotNull(message = "변경할 구독 상태는 필수입니다.")
    val targetStatus: SubscriptionStatus?,
) {
    fun toCommand(): ChangeSubscriptionCommand =
        ChangeSubscriptionCommand(
            phoneNumber = PhoneNumber(requireNotNull(phoneNumber)),
            channelId = ChannelId(requireNotNull(channelId)),
            targetStatus = requireNotNull(targetStatus),
        )
}
