package org.artinus.backend.subscription.adapter.outbound.ai

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.ai.subscription-history-summary")
data class SubscriptionHistoryAiProperties(
    @field:NotBlank
    val model: String = "gpt-4o-mini",
    val modelProfile: SubscriptionHistoryAiModelProfile = SubscriptionHistoryAiModelProfile.STANDARD,
    @field:DecimalMin("0.0")
    @field:DecimalMax("2.0")
    val temperature: Double = 0.0,
    @field:Positive
    val maxOutputTokens: Int = 300,
    @field:Positive
    val maxHistoryItems: Int = 100,
    val reasoningEffort: SubscriptionHistoryAiReasoningEffort = SubscriptionHistoryAiReasoningEffort.LOW,
    @field:NotBlank
    val promptCacheKey: String = "subscription-history-summary-v1",
) {
    @get:AssertTrue(message = "model과 model-profile 조합이 지원되지 않습니다.")
    val modelProfileCompatible: Boolean
        get() = modelProfile.supports(model)
}
