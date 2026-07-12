package org.artinus.backend.subscription.adapter.outbound.ai.config

enum class SubscriptionHistoryAiModelProfile(
    private vararg val supportedModelPrefixes: String,
) {
    STANDARD("gpt-4o-mini"),
    REASONING("gpt-5-mini"),
    ;

    fun supports(model: String): Boolean =
        supportedModelPrefixes.any { prefix ->
            model == prefix || model.startsWith("$prefix-")
        }
}
