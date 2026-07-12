package org.artinus.backend.subscription.adapter.outbound.ai

import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistorySummarizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

class SubscriptionHistoryAiConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(
                SubscriptionHistoryAiConfiguration::class.java,
                TestDependencies::class.java,
            )

    @Test
    fun `provider가 none이면 ChatModel 없이 fallback summarizer를 조립한다`() {
        contextRunner
            .withPropertyValues("spring.ai.model.chat=none")
            .run { context ->
                assertNull(context.startupFailure)
                val summarizer = context.getBean(SubscriptionHistorySummarizer::class.java)
                assertEquals("", summarizer.summarize(emptyList()))
            }
    }

    @Test
    fun `provider가 openai인데 ChatModel이 없으면 시작에 실패한다`() {
        contextRunner
            .withPropertyValues("spring.ai.model.chat=openai")
            .run { context ->
                val failure = context.startupFailure
                assertNotNull(failure)
                assertTrue(requireNotNull(failure).rootCause().message.orEmpty().contains("ChatModel"))
            }
    }

    @Test
    fun `지원하지 않는 모델과 프로필 조합은 시작에 실패한다`() {
        contextRunner
            .withPropertyValues(
                "spring.ai.model.chat=none",
                "app.ai.subscription-history-summary.model=gpt-5-mini",
                "app.ai.subscription-history-summary.model-profile=STANDARD",
            )
            .run { context ->
                val failure = context.startupFailure
                assertNotNull(failure)
                assertTrue(requireNotNull(failure).message.orEmpty().contains("SubscriptionHistoryAiProperties"))
            }
    }

    private fun Throwable.rootCause(): Throwable =
        generateSequence(this) { it.cause }
            .last()

    @TestConfiguration(proxyBeanMethods = false)
    class TestDependencies {
        @Bean
        fun bulkheadRegistry(): BulkheadRegistry = BulkheadRegistry.ofDefaults()

        @Bean
        fun circuitBreakerRegistry(): CircuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()

        @Bean
        fun subscriptionHistoryPrompt(): SubscriptionHistoryPrompt = SubscriptionHistoryPrompt()
    }
}
