package org.artinus.backend.subscription.adapter.outbound.ai

import org.artinus.backend.subscription.adapter.outbound.ai.config.SubscriptionHistoryAiModelProfile
import org.artinus.backend.subscription.adapter.outbound.ai.config.SubscriptionHistoryAiProperties
import org.artinus.backend.subscription.adapter.outbound.ai.config.SubscriptionHistoryAiReasoningEffort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SubscriptionHistoryOpenAiOptionsFactoryTest {
    @Test
    fun `standard 프로필은 temperature와 max tokens를 사용한다`() {
        val options =
            SubscriptionHistoryOpenAiOptionsFactory(
                SubscriptionHistoryAiProperties(
                    model = "gpt-4o-mini",
                    modelProfile = SubscriptionHistoryAiModelProfile.STANDARD,
                    temperature = 0.0,
                    maxOutputTokens = 300,
                ),
            ).create().build()

        assertEquals("gpt-4o-mini", options.model)
        assertEquals(0.0, options.temperature)
        assertEquals(300, options.maxTokens)
        assertNull(options.maxCompletionTokens)
        assertNull(options.reasoningEffort)
    }

    @Test
    fun `reasoning 프로필은 temperature를 제외하고 completion token과 reasoning effort를 사용한다`() {
        val options =
            SubscriptionHistoryOpenAiOptionsFactory(
                SubscriptionHistoryAiProperties(
                    model = "gpt-5-mini",
                    modelProfile = SubscriptionHistoryAiModelProfile.REASONING,
                    temperature = 0.0,
                    maxOutputTokens = 500,
                    reasoningEffort = SubscriptionHistoryAiReasoningEffort.LOW,
                ),
            ).create().build()

        assertEquals("gpt-5-mini", options.model)
        assertNull(options.temperature)
        assertNull(options.maxTokens)
        assertEquals(500, options.maxCompletionTokens)
        assertEquals("low", options.reasoningEffort)
    }
}
