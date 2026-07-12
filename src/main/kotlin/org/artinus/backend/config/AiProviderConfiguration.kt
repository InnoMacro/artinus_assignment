package org.artinus.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class AiProviderConfiguration {
    @Bean
    fun aiChatProviderRequirement(
        @Value("\${spring.ai.model.chat:none}") provider: String,
    ): AiChatProviderRequirement = AiChatProviderRequirement(provider)

    class AiChatProviderRequirement(provider: String) {
        init {
            require(provider in SUPPORTED_PROVIDERS) {
                "spring.ai.model.chat은 none 또는 openai만 지원합니다: $provider"
            }
        }

        companion object {
            private val SUPPORTED_PROVIDERS = setOf("none", "openai")
        }
    }
}
