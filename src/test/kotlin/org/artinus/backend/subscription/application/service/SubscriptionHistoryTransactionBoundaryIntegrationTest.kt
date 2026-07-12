package org.artinus.backend.subscription.application.service

import org.artinus.backend.TestcontainersConfiguration
import org.artinus.backend.subscription.application.port.inbound.GetSubscriptionHistoryUseCase
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistoryQueryPort
import org.artinus.backend.subscription.application.port.outbound.SubscriptionHistorySummarizer
import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionAction
import org.artinus.backend.subscription.domain.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Import(
    TestcontainersConfiguration::class,
    SubscriptionHistoryTransactionBoundaryIntegrationTest.BoundaryTestConfiguration::class,
)
@SpringBootTest
class SubscriptionHistoryTransactionBoundaryIntegrationTest @Autowired constructor(
    private val useCase: GetSubscriptionHistoryUseCase,
    private val transactionTemplate: TransactionTemplate,
    private val summarizer: TransactionRecordingSummarizer,
) {
    @Test
    fun `호출자의 transaction을 suspend하고 LLM 호출 후 다시 복원한다`() {
        transactionTemplate.executeWithoutResult {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive())

            useCase.getHistory(PhoneNumber("01012345678"))

            assertEquals(false, summarizer.transactionActive)
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive())
        }
    }

    class TransactionRecordingSummarizer : SubscriptionHistorySummarizer {
        var transactionActive: Boolean? = null

        override fun summarize(histories: List<SubscriptionHistoryItem>): String {
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive()
            return "요약"
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class BoundaryTestConfiguration {
        @Bean
        @Primary
        fun historyQueryPort(): SubscriptionHistoryQueryPort =
            SubscriptionHistoryQueryPort {
                listOf(
                    SubscriptionHistoryItem(
                        id = 1,
                        channelName = "홈페이지",
                        action = SubscriptionAction.SUBSCRIBE,
                        previousStatus = SubscriptionStatus.NONE,
                        changedStatus = SubscriptionStatus.BASIC,
                        changedAt = Instant.parse("2026-01-01T12:00:00Z"),
                    ),
                )
            }

        @Bean
        @Primary
        fun transactionRecordingSummarizer(): TransactionRecordingSummarizer =
            TransactionRecordingSummarizer()
    }
}
