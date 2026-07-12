package org.artinus.backend

import io.github.resilience4j.bulkhead.Bulkhead
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class BackendApplicationTests @Autowired constructor(
    @Qualifier("applicationTaskExecutor") private val applicationTaskExecutor: Executor,
    private val environment: Environment,
    private val chatModelProvider: ObjectProvider<ChatModel>,
    @Qualifier("subscriptionHistoryAiBulkhead") private val aiBulkhead: Bulkhead,
) {
    @Test
    fun contextLoads() {
    }

    @Test
    fun `Boot가 관리하는 application task executor도 virtual thread를 사용한다`() {
        val result = CompletableFuture<Boolean>()

        applicationTaskExecutor.execute {
            result.complete(Thread.currentThread().isVirtual)
        }

        assertTrue(result.get())
    }

    @Test
    fun `이력 요약은 기본적으로 SDK 재시도 없이 timeout 후 fallback한다`() {
        assertEquals("0", environment.getProperty("spring.ai.openai.chat.max-retries"))
    }

    @Test
    fun `테스트 기본 설정에서는 외부 ChatModel을 생성하지 않는다`() {
        assertEquals(null, chatModelProvider.getIfAvailable())
    }

    @Test
    fun `AI bulkhead는 대기 없이 인스턴스당 동시 호출을 제한한다`() {
        assertEquals(8, aiBulkhead.bulkheadConfig.maxConcurrentCalls)
        assertEquals(Duration.ZERO, aiBulkhead.bulkheadConfig.maxWaitDuration)
    }
}
