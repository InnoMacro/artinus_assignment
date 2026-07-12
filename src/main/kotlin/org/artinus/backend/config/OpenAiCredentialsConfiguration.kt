package org.artinus.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "spring.ai.model",
    name = ["chat"],
    havingValue = "openai",
)
class OpenAiCredentialsConfiguration {
    @Bean
    fun openAiApiKeyRequirement(
        @Value("\${spring.ai.openai.api-key:}") apiKey: String,
    ): OpenAiApiKeyRequirement = OpenAiApiKeyRequirement(apiKey)

    class OpenAiApiKeyRequirement(apiKey: String) {
        init {
            require(apiKey.isNotBlank()) {
                "AI_CHAT_PROVIDER=openai이면 OPENAI_API_KEY를 설정해야 합니다."
            }
        }
    }
}
