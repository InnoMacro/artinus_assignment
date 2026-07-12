package org.artinus.backend.subscription.application.service

import org.artinus.backend.subscription.application.exception.SubscriptionHistorySummaryUnavailableException
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryQueryPort
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistorySummarizer
import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.application.result.SummarySource
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionAction
import org.artinus.backend.subscription.domain.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class GetSubscriptionHistoryServiceTest {
    private val historyQueryPort = FakeSubscriptionHistoryQueryPort()
    private val summarizer = RecordingSubscriptionHistorySummarizer()
    private val service = GetSubscriptionHistoryService(historyQueryPort, summarizer)

    @Test
    fun `구독 이력을 시간순으로 정렬하고 LLM 요약과 함께 반환한다`() {
        historyQueryPort.histories =
            listOf(
                history(
                    id = 3,
                    channelName = "모바일앱",
                    previousStatus = SubscriptionStatus.BASIC,
                    changedStatus = SubscriptionStatus.PREMIUM,
                    changedAt = "2026-02-01T12:00:00Z",
                ),
                history(
                    id = 2,
                    channelName = "홈페이지",
                    previousStatus = SubscriptionStatus.NONE,
                    changedStatus = SubscriptionStatus.BASIC,
                    changedAt = "2026-01-01T12:00:00Z",
                ),
                history(
                    id = 1,
                    channelName = "홈페이지",
                    previousStatus = SubscriptionStatus.NONE,
                    changedStatus = SubscriptionStatus.BASIC,
                    changedAt = "2026-01-01T12:00:00Z",
                ),
            )
        summarizer.response = "홈페이지에서 일반 구독을 시작한 뒤 모바일앱에서 프리미엄으로 변경했습니다."

        val result = service.getHistory(PhoneNumber("010-1234-5678"))

        assertEquals(listOf(1L, 2L, 3L), result.history.map(SubscriptionHistoryItem::id))
        assertEquals(summarizer.response, result.summary)
        assertEquals(SummarySource.LLM, result.summarySource)
    }

    @Test
    fun `LLM 호출이 실패하면 같은 이력으로 규칙 기반 요약을 반환한다`() {
        historyQueryPort.histories = unorderedHistories()
        summarizer.failure = SubscriptionHistorySummaryUnavailableException("LLM unavailable")

        val result = service.getHistory(PhoneNumber("01012345678"))

        assertEquals(FALLBACK_SUMMARY, result.summary)
        assertEquals(SummarySource.FALLBACK, result.summarySource)
        assertEquals(listOf(1L, 2L), result.history.map(SubscriptionHistoryItem::id))
    }

    @Test
    fun `예상하지 못한 summarizer 구현 오류는 fallback으로 숨기지 않는다`() {
        historyQueryPort.histories = unorderedHistories()
        summarizer.failure = IllegalArgumentException("programming error")

        assertThrows(IllegalArgumentException::class.java) {
            service.getHistory(PhoneNumber("01012345678"))
        }
    }

    @Test
    fun `LLM이 빈 요약을 반환하면 같은 이력으로 규칙 기반 요약을 반환한다`() {
        historyQueryPort.histories = unorderedHistories()
        summarizer.response = "   "

        val result = service.getHistory(PhoneNumber("01012345678"))

        assertEquals(FALLBACK_SUMMARY, result.summary)
        assertEquals(SummarySource.FALLBACK, result.summarySource)
        assertEquals(listOf(1L, 2L), result.history.map(SubscriptionHistoryItem::id))
    }

    @Test
    fun `fallback은 KST 날짜와 시작 다운그레이드 해지 전이를 모두 반영한다`() {
        historyQueryPort.histories =
            listOf(
                history(
                    id = 1,
                    channelName = "홈페이지",
                    previousStatus = SubscriptionStatus.NONE,
                    changedStatus = SubscriptionStatus.PREMIUM,
                    changedAt = "2026-01-01T15:00:00Z",
                ),
                history(
                    id = 2,
                    channelName = "모바일앱",
                    previousStatus = SubscriptionStatus.PREMIUM,
                    changedStatus = SubscriptionStatus.BASIC,
                    changedAt = "2026-02-01T15:00:00Z",
                ),
                history(
                    id = 3,
                    channelName = "콜센터",
                    previousStatus = SubscriptionStatus.BASIC,
                    changedStatus = SubscriptionStatus.NONE,
                    changedAt = "2026-03-01T15:00:00Z",
                ).copy(action = SubscriptionAction.UNSUBSCRIBE),
            )
        summarizer.failure = SubscriptionHistorySummaryUnavailableException()

        val result = service.getHistory(PhoneNumber("01012345678"))

        assertEquals(
            "2026년 1월 2일 홈페이지에서 프리미엄 구독을 시작했습니다. " +
                "2026년 2월 2일 모바일앱에서 일반 구독으로 변경했습니다. " +
                "2026년 3월 2일 콜센터에서 구독을 해지했습니다.",
            result.summary,
        )
    }

    @Test
    fun `조회한 단일 snapshot을 LLM 입력과 응답에 함께 사용한다`() {
        historyQueryPort.histories = unorderedHistories()

        val result = service.getHistory(PhoneNumber("01012345678"))

        assertEquals(1, historyQueryPort.callCount)
        assertSame(summarizer.receivedHistories, result.history)
        assertEquals(listOf(1L, 2L), summarizer.receivedHistories?.map(SubscriptionHistoryItem::id))
    }

    @Test
    fun `변경 이력이 없으면 LLM을 호출하지 않고 빈 이력 요약을 반환한다`() {
        historyQueryPort.histories = emptyList()

        val result = service.getHistory(PhoneNumber("01012345678"))

        assertEquals(emptyList<SubscriptionHistoryItem>(), result.history)
        assertEquals("구독 변경 이력이 없습니다.", result.summary)
        assertEquals(SummarySource.FALLBACK, result.summarySource)
        assertEquals(0, summarizer.callCount)
    }

    private fun unorderedHistories(): List<SubscriptionHistoryItem> =
        listOf(
            history(
                id = 2,
                channelName = "모바일앱",
                previousStatus = SubscriptionStatus.BASIC,
                changedStatus = SubscriptionStatus.PREMIUM,
                changedAt = "2026-02-01T12:00:00Z",
            ),
            history(
                id = 1,
                channelName = "홈페이지",
                previousStatus = SubscriptionStatus.NONE,
                changedStatus = SubscriptionStatus.BASIC,
                changedAt = "2026-01-01T12:00:00Z",
            ),
        )

    private fun history(
        id: Long,
        channelName: String,
        previousStatus: SubscriptionStatus,
        changedStatus: SubscriptionStatus,
        changedAt: String,
    ) =
        SubscriptionHistoryItem(
            id = id,
            channelName = channelName,
            action = SubscriptionAction.SUBSCRIBE,
            previousStatus = previousStatus,
            changedStatus = changedStatus,
            changedAt = Instant.parse(changedAt),
        )

    private class FakeSubscriptionHistoryQueryPort : SubscriptionHistoryQueryPort {
        var histories: List<SubscriptionHistoryItem> = emptyList()
        var callCount = 0

        override fun findAllByPhoneNumber(phoneNumber: PhoneNumber): List<SubscriptionHistoryItem> {
            callCount += 1
            return histories
        }
    }

    private class RecordingSubscriptionHistorySummarizer : SubscriptionHistorySummarizer {
        var response = "LLM summary"
        var failure: RuntimeException? = null
        var receivedHistories: List<SubscriptionHistoryItem>? = null
        var callCount = 0

        override fun summarize(histories: List<SubscriptionHistoryItem>): String {
            callCount += 1
            receivedHistories = histories
            failure?.let { throw it }
            return response
        }
    }

    companion object {
        private const val FALLBACK_SUMMARY =
            "2026년 1월 1일 홈페이지에서 일반 구독을 시작했습니다. " +
                "2026년 2월 1일 모바일앱에서 프리미엄 구독으로 변경했습니다."
    }
}
