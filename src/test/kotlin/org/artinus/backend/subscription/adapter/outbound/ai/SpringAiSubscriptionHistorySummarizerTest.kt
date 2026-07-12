package org.artinus.backend.subscription.adapter.outbound.ai

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.artinus.backend.subscription.adapter.outbound.ai.config.SubscriptionHistoryAiModelProfile
import org.artinus.backend.subscription.adapter.outbound.ai.config.SubscriptionHistoryAiProperties
import org.artinus.backend.subscription.adapter.outbound.ai.prompt.SubscriptionHistoryPrompt
import org.artinus.backend.subscription.application.exception.SubscriptionHistorySummaryUnavailableException
import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.domain.vo.SubscriptionAction
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import java.time.Duration
import java.time.Instant

class SpringAiSubscriptionHistorySummarizerTest {
    @Test
    fun `미리 정의한 prompt와 사용 사례별 모델 옵션으로 이력을 요약한다`() {
        val chatModel = RecordingChatModel("LLM이 만든 요약")
        val summarizer =
            SpringAiSubscriptionHistorySummarizer(
                chatClient = ChatClient.builder(chatModel).build(),
                prompt = SubscriptionHistoryPrompt(),
                properties =
                    SubscriptionHistoryAiProperties(
                        model = "gpt-4o-mini",
                        modelProfile = SubscriptionHistoryAiModelProfile.STANDARD,
                        temperature = 0.0,
                        maxOutputTokens = 300,
                        promptCacheKey = "subscription-history-summary-v1",
                    ),
                circuitBreaker = CircuitBreaker.ofDefaults("subscription-history-summary-test"),
                bulkhead = Bulkhead.ofDefaults("subscription-history-summary-test"),
            )

        val result = summarizer.summarize(listOf(history()))

        assertEquals("LLM이 만든 요약", result)
        val messageTexts = chatModel.receivedPrompt.instructions.map { it.text.orEmpty() }
        assertTrue(messageTexts.any { it.contains("입력에 없는 사실") })
        assertTrue(messageTexts.any { it.contains("홈페이지") })
        assertFalse(messageTexts.any { it.contains("01012345678") })

        val options = chatModel.receivedPrompt.options as OpenAiChatOptions
        assertEquals("gpt-4o-mini", options.model)
        assertEquals(0.0, options.temperature)
        assertEquals(300, options.maxTokens)
        assertEquals("subscription-history-summary-v1", options.promptCacheKey)
    }

    @Test
    fun `서킷이 열려 있으면 모델을 호출하지 않고 즉시 실패한다`() {
        val chatModel = RecordingChatModel("호출되면 안 되는 응답")
        val circuitBreaker = CircuitBreaker.ofDefaults("subscription-history-summary-open-test")
        circuitBreaker.transitionToOpenState()
        val summarizer =
            SpringAiSubscriptionHistorySummarizer(
                chatClient = ChatClient.builder(chatModel).build(),
                prompt = SubscriptionHistoryPrompt(),
                properties = SubscriptionHistoryAiProperties(),
                circuitBreaker = circuitBreaker,
                bulkhead = Bulkhead.ofDefaults("subscription-history-summary-open-test"),
            )

        val exception =
            assertThrows(SubscriptionHistorySummaryUnavailableException::class.java) {
                summarizer.summarize(listOf(history()))
            }
        assertTrue(exception.cause is CallNotPermittedException)
        assertFalse(chatModel.wasCalled)
    }

    @Test
    fun `입력 이력이 설정한 최대 건수를 넘으면 모델을 호출하지 않는다`() {
        val chatModel = RecordingChatModel("호출되면 안 되는 응답")
        val summarizer =
            SpringAiSubscriptionHistorySummarizer(
                chatClient = ChatClient.builder(chatModel).build(),
                prompt = SubscriptionHistoryPrompt(),
                properties = SubscriptionHistoryAiProperties(maxHistoryItems = 1),
                circuitBreaker = CircuitBreaker.ofDefaults("subscription-history-summary-limit-test"),
                bulkhead = Bulkhead.ofDefaults("subscription-history-summary-limit-test"),
            )

        val result = summarizer.summarize(listOf(history(), history().copy(id = 2)))

        assertEquals("", result)
        assertFalse(chatModel.wasCalled)
    }

    @Test
    fun `동시 호출 한도가 가득 차면 모델을 호출하지 않고 즉시 실패한다`() {
        val chatModel = RecordingChatModel("호출되면 안 되는 응답")
        val bulkhead =
            Bulkhead.of(
                "subscription-history-summary-full-test",
                BulkheadConfig.custom()
                    .maxConcurrentCalls(1)
                    .maxWaitDuration(Duration.ZERO)
                    .build(),
            )
        bulkhead.acquirePermission()
        val circuitBreaker = CircuitBreaker.ofDefaults("subscription-history-summary-full-test")
        val summarizer =
            SpringAiSubscriptionHistorySummarizer(
                chatClient = ChatClient.builder(chatModel).build(),
                prompt = SubscriptionHistoryPrompt(),
                properties = SubscriptionHistoryAiProperties(),
                circuitBreaker = circuitBreaker,
                bulkhead = bulkhead,
            )

        try {
            val exception =
                assertThrows(SubscriptionHistorySummaryUnavailableException::class.java) {
                    summarizer.summarize(listOf(history()))
                }
            assertTrue(exception.cause is BulkheadFullException)
            assertFalse(chatModel.wasCalled)
            assertEquals(0, circuitBreaker.metrics.numberOfFailedCalls)
        } finally {
            bulkhead.releasePermission()
        }
    }

    @Test
    fun `모델 호출이 실패해도 bulkhead permit을 반환하고 circuit failure로 기록한다`() {
        val chatModel = RecordingChatModel(failure = IllegalStateException("OpenAI unavailable"))
        val bulkhead =
            Bulkhead.of(
                "subscription-history-summary-release-test",
                BulkheadConfig.custom()
                    .maxConcurrentCalls(1)
                    .maxWaitDuration(Duration.ZERO)
                    .build(),
            )
        val circuitBreaker = CircuitBreaker.ofDefaults("subscription-history-summary-release-test")
        val summarizer =
            SpringAiSubscriptionHistorySummarizer(
                chatClient = ChatClient.builder(chatModel).build(),
                prompt = SubscriptionHistoryPrompt(),
                properties = SubscriptionHistoryAiProperties(),
                circuitBreaker = circuitBreaker,
                bulkhead = bulkhead,
            )

        assertThrows(IllegalStateException::class.java) {
            summarizer.summarize(listOf(history()))
        }

        assertEquals(1, bulkhead.metrics.availableConcurrentCalls)
        assertEquals(1, circuitBreaker.metrics.numberOfFailedCalls)
    }

    @Test
    fun `모델이 빈 응답을 반환하면 외부 호출 실패로 기록한다`() {
        val chatModel = RecordingChatModel("   ")
        val circuitBreaker = CircuitBreaker.ofDefaults("subscription-history-summary-empty-test")
        val summarizer =
            SpringAiSubscriptionHistorySummarizer(
                chatClient = ChatClient.builder(chatModel).build(),
                prompt = SubscriptionHistoryPrompt(),
                properties = SubscriptionHistoryAiProperties(),
                circuitBreaker = circuitBreaker,
                bulkhead = Bulkhead.ofDefaults("subscription-history-summary-empty-test"),
            )

        assertThrows(SubscriptionHistorySummaryUnavailableException::class.java) {
            summarizer.summarize(listOf(history()))
        }
        assertEquals(1, circuitBreaker.metrics.numberOfFailedCalls)
    }

    private fun history() =
        SubscriptionHistoryItem(
            id = 1,
            channelName = "홈페이지",
            action = SubscriptionAction.SUBSCRIBE,
            previousStatus = SubscriptionStatus.NONE,
            changedStatus = SubscriptionStatus.BASIC,
            changedAt = Instant.parse("2026-01-01T12:00:00Z"),
        )

    private class RecordingChatModel(
        private val response: String = "",
        private val failure: RuntimeException? = null,
    ) : ChatModel {
        lateinit var receivedPrompt: Prompt
        var wasCalled: Boolean = false

        override fun getOptions(): ChatOptions = OpenAiChatOptions.builder().build()

        override fun call(prompt: Prompt): ChatResponse {
            wasCalled = true
            receivedPrompt = prompt
            failure?.let { throw it }
            return ChatResponse(listOf(Generation(AssistantMessage(response))))
        }
    }
}
