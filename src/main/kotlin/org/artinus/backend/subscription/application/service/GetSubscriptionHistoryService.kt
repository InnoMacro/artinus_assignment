package org.artinus.backend.subscription.application.service

import org.apache.logging.log4j.LogManager
import org.artinus.backend.subscription.application.exception.SubscriptionHistorySummaryUnavailableException
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.application.port.inbound.GetSubscriptionHistoryUseCase
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryQueryPort
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistorySummarizer
import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.application.result.SubscriptionHistoryResult
import org.artinus.backend.subscription.application.result.SummarySource
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class GetSubscriptionHistoryService(
    private val historyQueryPort: SubscriptionHistoryQueryPort,
    private val summarizer: SubscriptionHistorySummarizer,
    private val fallbackSummaryGenerator: SubscriptionHistoryFallbackSummaryGenerator,
) : GetSubscriptionHistoryUseCase {
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun getHistory(phoneNumber: PhoneNumber): SubscriptionHistoryResult {
        val snapshot =
            historyQueryPort.findAllByPhoneNumber(phoneNumber)
                ?: throw SubscriptionMemberNotFoundException(phoneNumber)

        val generatedSummary = generateSummary(snapshot)

        return SubscriptionHistoryResult(
            history = snapshot,
            summary = generatedSummary.text,
            summarySource = generatedSummary.source,
        )
    }

    private fun generateSummary(histories: List<SubscriptionHistoryItem>): GeneratedSummary {
        if (histories.isEmpty()) {
            return GeneratedSummary(fallbackSummaryGenerator.generate(histories), SummarySource.FALLBACK)
        }

        return try {
            summarizer.summarize(histories)
                .takeIf(String::isNotBlank)
                ?.let { GeneratedSummary(it, SummarySource.LLM) }
                ?: GeneratedSummary(fallbackSummaryGenerator.generate(histories), SummarySource.FALLBACK)
        } catch (exception: SubscriptionHistorySummaryUnavailableException) {
            logger.warn(
                "구독 이력 LLM 요약에 실패해 규칙 기반 요약을 사용합니다. exceptionType={}",
                exception.cause?.javaClass?.simpleName ?: exception.javaClass.simpleName,
            )
            GeneratedSummary(fallbackSummaryGenerator.generate(histories), SummarySource.FALLBACK)
        }
    }

    private data class GeneratedSummary(
        val text: String,
        val source: SummarySource,
    )

    companion object {
        private val logger = LogManager.getLogger(GetSubscriptionHistoryService::class.java)
    }
}
