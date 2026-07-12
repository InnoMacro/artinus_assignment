package org.artinus.backend.subscription.adapter.outbound.ai.config

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.artinus.backend.subscription.adapter.outbound.ai.SpringAiSubscriptionHistorySummarizer
import org.artinus.backend.subscription.adapter.outbound.ai.prompt.SubscriptionHistoryPrompt
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistorySummarizer
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SubscriptionHistoryAiProperties::class)
class SubscriptionHistoryAiConfiguration {
    @Bean("subscriptionHistoryAiBulkhead")
    fun subscriptionHistoryAiBulkhead(registry: BulkheadRegistry): Bulkhead =
        registry.bulkhead("subscriptionHistoryAi")

    @Bean("subscriptionHistoryAiCircuitBreaker")
    fun subscriptionHistoryAiCircuitBreaker(registry: CircuitBreakerRegistry): CircuitBreaker =
        registry.circuitBreaker("subscriptionHistoryAi")

    @Bean
    fun subscriptionHistorySummarizer(
        chatModelProvider: ObjectProvider<ChatModel>,
        chatClientBuilderProvider: ObjectProvider<ChatClient.Builder>,
        prompt: SubscriptionHistoryPrompt,
        properties: SubscriptionHistoryAiProperties,
        @Value("\${spring.ai.model.chat:none}") provider: String,
        @Qualifier("subscriptionHistoryAiBulkhead") bulkhead: Bulkhead,
        @Qualifier("subscriptionHistoryAiCircuitBreaker") circuitBreaker: CircuitBreaker,
    ): SubscriptionHistorySummarizer {
        if (provider == "none") {
            return SubscriptionHistorySummarizer { "" }
        }
        check(chatModelProvider.getIfAvailable() != null) {
            "OpenAI provider가 활성화됐지만 ChatModel bean이 없습니다."
        }

        val builder = chatClientBuilderProvider.getObject()

        return SpringAiSubscriptionHistorySummarizer(
            chatClient = builder.build(),
            prompt = prompt,
            properties = properties,
            bulkhead = bulkhead,
            circuitBreaker = circuitBreaker,
        )
    }
}
