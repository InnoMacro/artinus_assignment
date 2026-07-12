package org.artinus.backend.subscription.adapter.outbound.ai.config

import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

class SubscriptionHistoryAiPropertiesBindingTest {
    @Test
    fun `reasoning 모델 프로필과 옵션을 외부 설정에서 바인딩한다`() {
        val source =
            MapConfigurationPropertySource(
                mapOf(
                    "app.ai.subscription-history-summary.model" to "gpt-5-mini",
                    "app.ai.subscription-history-summary.model-profile" to "reasoning",
                    "app.ai.subscription-history-summary.max-output-tokens" to "600",
                    "app.ai.subscription-history-summary.reasoning-effort" to "medium",
                ),
            )

        val properties =
            Binder(source)
                .bind(
                    "app.ai.subscription-history-summary",
                    Bindable.of(SubscriptionHistoryAiProperties::class.java),
                )
                .get()

        assertEquals("gpt-5-mini", properties.model)
        assertEquals(SubscriptionHistoryAiModelProfile.REASONING, properties.modelProfile)
        assertEquals(600, properties.maxOutputTokens)
        assertEquals(SubscriptionHistoryAiReasoningEffort.MEDIUM, properties.reasoningEffort)
    }

    @Test
    fun `지원 모델과 옵션 프로필이 맞지 않으면 validation에 실패한다`() {
        val validator = Validation.buildDefaultValidatorFactory().validator

        val violations =
            validator.validate(
                SubscriptionHistoryAiProperties(
                    model = "gpt-5-mini",
                    modelProfile = SubscriptionHistoryAiModelProfile.STANDARD,
                ),
            )

        assertTrue(violations.any { it.propertyPath.toString() == "modelProfileCompatible" })
    }

    @Test
    fun `지원 모델과 일치하는 옵션 프로필은 validation을 통과한다`() {
        val validator = Validation.buildDefaultValidatorFactory().validator
        val supported =
            listOf(
                SubscriptionHistoryAiProperties(
                    model = "gpt-4o-mini",
                    modelProfile = SubscriptionHistoryAiModelProfile.STANDARD,
                ),
                SubscriptionHistoryAiProperties(
                    model = "gpt-5-mini",
                    modelProfile = SubscriptionHistoryAiModelProfile.REASONING,
                ),
            )

        supported.forEach { properties ->
            assertTrue(validator.validate(properties).isEmpty())
        }
    }
}
