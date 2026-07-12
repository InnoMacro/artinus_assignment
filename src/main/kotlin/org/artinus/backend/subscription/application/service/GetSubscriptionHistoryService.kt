package org.artinus.backend.subscription.application.service

import org.apache.logging.log4j.LogManager
import org.artinus.backend.subscription.application.exception.SubscriptionHistorySummaryUnavailableException
import org.artinus.backend.subscription.application.port.inbound.GetSubscriptionHistoryUseCase
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryQueryPort
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistorySummarizer
import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.application.result.SubscriptionHistoryResult
import org.artinus.backend.subscription.application.result.SummarySource
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class GetSubscriptionHistoryService(
    private val historyQueryPort: SubscriptionHistoryQueryPort,
    private val summarizer: SubscriptionHistorySummarizer,
) : GetSubscriptionHistoryUseCase {
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun getHistory(phoneNumber: PhoneNumber): SubscriptionHistoryResult {
        val snapshot =
            historyQueryPort.findAllByPhoneNumber(phoneNumber)
                .sortedWith(compareBy(SubscriptionHistoryItem::changedAt, SubscriptionHistoryItem::id))

        val generatedSummary = generateSummary(snapshot)

        return SubscriptionHistoryResult(
            history = snapshot,
            summary = generatedSummary.text,
            summarySource = generatedSummary.source,
        )
    }

    private fun generateSummary(histories: List<SubscriptionHistoryItem>): GeneratedSummary {
        if (histories.isEmpty()) {
            return GeneratedSummary(fallbackSummary(histories), SummarySource.FALLBACK)
        }

        return try {
            summarizer.summarize(histories)
                .takeIf(String::isNotBlank)
                ?.let { GeneratedSummary(it, SummarySource.LLM) }
                ?: GeneratedSummary(fallbackSummary(histories), SummarySource.FALLBACK)
        } catch (exception: SubscriptionHistorySummaryUnavailableException) {
            logger.warn(
                "구독 이력 LLM 요약에 실패해 규칙 기반 요약을 사용합니다. exceptionType={}",
                exception.cause?.javaClass?.simpleName ?: exception.javaClass.simpleName,
            )
            GeneratedSummary(fallbackSummary(histories), SummarySource.FALLBACK)
        }
    }

    private fun fallbackSummary(histories: List<SubscriptionHistoryItem>): String {
        if (histories.isEmpty()) {
            return "구독 변경 이력이 없습니다."
        }

        return histories.joinToString(separator = " ") { history ->
            val date = DATE_FORMATTER.format(history.changedAt.atZone(SUMMARY_ZONE_ID))
            "$date ${history.channelName}에서 ${history.fallbackDescription()}"
        }
    }

    private fun SubscriptionHistoryItem.fallbackDescription(): String =
        when {
            previousStatus == SubscriptionStatus.NONE && changedStatus == SubscriptionStatus.BASIC ->
                "일반 구독을 시작했습니다."

            previousStatus == SubscriptionStatus.NONE && changedStatus == SubscriptionStatus.PREMIUM ->
                "프리미엄 구독을 시작했습니다."

            changedStatus == SubscriptionStatus.PREMIUM ->
                "프리미엄 구독으로 변경했습니다."

            changedStatus == SubscriptionStatus.BASIC ->
                "일반 구독으로 변경했습니다."

            changedStatus == SubscriptionStatus.NONE ->
                "구독을 해지했습니다."

            else -> "구독 상태를 변경했습니다."
        }

    private data class GeneratedSummary(
        val text: String,
        val source: SummarySource,
    )

    companion object {
        private val logger = LogManager.getLogger(GetSubscriptionHistoryService::class.java)
        private val SUMMARY_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
    }
}
