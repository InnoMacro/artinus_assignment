package org.artinus.backend.subscription.adapter.outbound.ai

import org.artinus.backend.TestcontainersConfiguration
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistorySummarizer
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest(
    properties = [
        "spring.ai.model.chat=openai",
        "spring.ai.openai.api-key=test-key-that-is-never-called",
    ],
)
class SubscriptionHistoryAiWiringIntegrationTest @Autowired constructor(
    private val chatModel: ChatModel,
    private val summarizer: SubscriptionHistorySummarizer,
) {
    @Test
    fun `OpenAI provider가 활성화되면 실제 Spring AI adapter를 조립한다`() {
        assertNotNull(chatModel)
        assertInstanceOf(SpringAiSubscriptionHistorySummarizer::class.java, summarizer)

        val commonOptions = chatModel.options as OpenAiChatOptions
        assertNull(commonOptions.temperature)
        assertNull(commonOptions.maxTokens)
        assertNull(commonOptions.maxCompletionTokens)
    }
}
