package org.artinus.backend.subscription.application.service

import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class SubscriptionHistoryFallbackSummaryGenerator {
    fun generate(histories: List<SubscriptionHistoryItem>): String {
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

    companion object {
        private val SUMMARY_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
    }
}
