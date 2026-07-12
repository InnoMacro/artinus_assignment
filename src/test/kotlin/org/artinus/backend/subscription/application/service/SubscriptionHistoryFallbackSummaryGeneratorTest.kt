package org.artinus.backend.subscription.application.service

import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.domain.vo.SubscriptionAction
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class SubscriptionHistoryFallbackSummaryGeneratorTest {
    private val generator = SubscriptionHistoryFallbackSummaryGenerator()

    @Test
    fun `이력이 없으면 빈 이력 안내를 생성한다`() {
        assertEquals("구독 변경 이력이 없습니다.", generator.generate(emptyList()))
    }

    @Test
    fun `변경 시각을 KST 날짜로 변환한다`() {
        val history =
            history(
                id = 1,
                channelName = "홈페이지",
                previousStatus = SubscriptionStatus.NONE,
                changedStatus = SubscriptionStatus.BASIC,
                changedAt = "2026-01-01T15:00:00Z",
            )

        assertEquals(
            "2026년 1월 2일 홈페이지에서 일반 구독을 시작했습니다.",
            generator.generate(listOf(history)),
        )
    }

    @Test
    fun `시작 업그레이드 다운그레이드 해지 전이에 맞는 문구를 생성한다`() {
        val histories =
            listOf(
                history(
                    id = 1,
                    channelName = "홈페이지",
                    previousStatus = SubscriptionStatus.NONE,
                    changedStatus = SubscriptionStatus.PREMIUM,
                    changedAt = "2026-01-01T12:00:00Z",
                ),
                history(
                    id = 2,
                    channelName = "모바일앱",
                    previousStatus = SubscriptionStatus.BASIC,
                    changedStatus = SubscriptionStatus.PREMIUM,
                    changedAt = "2026-02-01T12:00:00Z",
                ),
                history(
                    id = 3,
                    channelName = "콜센터",
                    previousStatus = SubscriptionStatus.PREMIUM,
                    changedStatus = SubscriptionStatus.BASIC,
                    changedAt = "2026-03-01T12:00:00Z",
                ),
                history(
                    id = 4,
                    channelName = "홈페이지",
                    previousStatus = SubscriptionStatus.BASIC,
                    changedStatus = SubscriptionStatus.NONE,
                    changedAt = "2026-04-01T12:00:00Z",
                )
                    .copy(action = SubscriptionAction.UNSUBSCRIBE),
            )

        assertEquals(
            "2026년 1월 1일 홈페이지에서 프리미엄 구독을 시작했습니다. " +
                "2026년 2월 1일 모바일앱에서 프리미엄 구독으로 변경했습니다. " +
                "2026년 3월 1일 콜센터에서 일반 구독으로 변경했습니다. " +
                "2026년 4월 1일 홈페이지에서 구독을 해지했습니다.",
            generator.generate(histories),
        )
    }

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
}
