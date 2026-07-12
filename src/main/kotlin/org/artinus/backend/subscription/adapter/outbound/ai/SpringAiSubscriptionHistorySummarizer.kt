package org.artinus.backend.subscription.adapter.outbound.ai

import com.openai.errors.OpenAIException
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.artinus.backend.subscription.adapter.outbound.ai.config.SubscriptionHistoryAiProperties
import org.artinus.backend.subscription.adapter.outbound.ai.prompt.SubscriptionHistoryPrompt
import org.artinus.backend.subscription.application.exception.SubscriptionHistorySummaryUnavailableException
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistorySummarizer
import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.springframework.ai.chat.client.ChatClient

class SpringAiSubscriptionHistorySummarizer(
    private val chatClient: ChatClient,
    private val prompt: SubscriptionHistoryPrompt,
    private val properties: SubscriptionHistoryAiProperties,
    private val bulkhead: Bulkhead,
    private val circuitBreaker: CircuitBreaker,
) : SubscriptionHistorySummarizer {
    private val optionsFactory = SubscriptionHistoryOpenAiOptionsFactory(properties)

    override fun summarize(histories: List<SubscriptionHistoryItem>): String {
        if (histories.size > properties.maxHistoryItems) {
            return ""
        }

        return try {
            bulkhead.executeSupplier {
                circuitBreaker.executeSupplier {
                    val response =
                        chatClient.prompt()
                            .system(prompt.systemText)
                            .user(prompt.renderUserPrompt(histories))
                            .options(optionsFactory.create())
                            .call()
                            .chatResponse()

                    response?.result?.output?.text
                        ?.takeIf(String::isNotBlank)
                        ?: throw SubscriptionHistorySummaryUnavailableException(
                            "OpenAI가 빈 구독 이력 요약을 반환했습니다.",
                        )
                }
            }
        } catch (exception: SubscriptionHistorySummaryUnavailableException) {
            throw exception
        } catch (exception: OpenAIException) {
            throw SubscriptionHistorySummaryUnavailableException(cause = exception)
        } catch (exception: BulkheadFullException) {
            throw SubscriptionHistorySummaryUnavailableException(cause = exception)
        } catch (exception: CallNotPermittedException) {
            throw SubscriptionHistorySummaryUnavailableException(cause = exception)
        }
    }
}
