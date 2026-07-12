package org.artinus.backend.subscription.adapter.outbound.ai

import org.springframework.ai.openai.OpenAiChatOptions

class SubscriptionHistoryOpenAiOptionsFactory(
    private val properties: SubscriptionHistoryAiProperties,
) {
    fun create(): OpenAiChatOptions.Builder {
        val builder =
            OpenAiChatOptions.builder()
                .model(properties.model)
                .promptCacheKey(properties.promptCacheKey)

        when (properties.modelProfile) {
            SubscriptionHistoryAiModelProfile.STANDARD ->
                builder
                    .temperature(properties.temperature)
                    .maxTokens(properties.maxOutputTokens)

            SubscriptionHistoryAiModelProfile.REASONING ->
                builder
                    .maxCompletionTokens(properties.maxOutputTokens)
                    .reasoningEffort(properties.reasoningEffort.apiValue)
        }

        return builder
    }
}
