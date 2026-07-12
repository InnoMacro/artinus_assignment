package org.artinus.backend.subscription.adapter.outbound.csrng

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.artinus.backend.subscription.adapter.outbound.csrng.exception.CsrngInvalidResponseException
import org.artinus.backend.subscription.adapter.outbound.csrng.exception.CsrngUnavailableException
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalInvalidResponseException
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalUnavailableException
import org.artinus.backend.subscription.application.port.outbound.ApprovalDecision
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Duration

class CsrngSubscriptionApprovalAdapterTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var adapter: CsrngSubscriptionApprovalAdapter
    private lateinit var circuitBreaker: CircuitBreaker

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl("https://csrng.test")
        server = MockRestServiceServer.bindTo(builder).build()
        circuitBreaker = createCircuitBreaker()
        adapter = CsrngSubscriptionApprovalAdapter(builder.build(), retry(), circuitBreaker)
    }

    @Test
    fun `random 1 응답은 승인한다`() {
        server.expect(requestTo("https://csrng.test/csrng/csrng.php?min=0&max=1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""[{"status":"success","random":1}]""", MediaType.APPLICATION_JSON))

        assertEquals(ApprovalDecision.APPROVED, adapter.requestApproval())
        assertEquals(1, circuitBreaker.metrics.numberOfSuccessfulCalls)
        assertEquals(0, circuitBreaker.metrics.numberOfFailedCalls)
        server.verify()
    }

    @Test
    fun `random 0 응답은 재시도하지 않고 정상 거절한다`() {
        server.expect(ExpectedCount.once(), requestTo("https://csrng.test/csrng/csrng.php?min=0&max=1"))
            .andRespond(withSuccess("""[{"status":"success","random":0}]""", MediaType.APPLICATION_JSON))

        assertEquals(ApprovalDecision.REJECTED, adapter.requestApproval())
        server.verify()
    }

    @Test
    fun `5xx 응답은 지수 백오프와 jitter 정책으로 한 번 재시도한다`() {
        server.expect(ExpectedCount.once(), requestTo("https://csrng.test/csrng/csrng.php?min=0&max=1"))
            .andRespond(withServerError())
        server.expect(ExpectedCount.once(), requestTo("https://csrng.test/csrng/csrng.php?min=0&max=1"))
            .andRespond(withSuccess("""[{"status":"success","random":1}]""", MediaType.APPLICATION_JSON))

        assertEquals(ApprovalDecision.APPROVED, adapter.requestApproval())
        assertEquals(1, circuitBreaker.metrics.numberOfSuccessfulCalls)
        assertEquals(0, circuitBreaker.metrics.numberOfFailedCalls)
        server.verify()
    }

    @Test
    fun `지원하지 않는 random 값은 재시도하지 않는다`() {
        server.expect(ExpectedCount.once(), requestTo("https://csrng.test/csrng/csrng.php?min=0&max=1"))
            .andRespond(withSuccess("""[{"status":"success","random":9}]""", MediaType.APPLICATION_JSON))

        val exception =
            assertThrows(SubscriptionApprovalInvalidResponseException::class.java) {
                adapter.requestApproval()
            }
        assertTrue(exception.cause is CsrngInvalidResponseException)
        assertEquals(0, circuitBreaker.metrics.numberOfFailedCalls)
        server.verify()
    }

    @Test
    fun `JSON 형식이 잘못된 응답은 application 승인 응답 오류로 정규화한다`() {
        server.expect(ExpectedCount.once(), requestTo("https://csrng.test/csrng/csrng.php?min=0&max=1"))
            .andRespond(withSuccess("""[{"status":"success","random":1}""", MediaType.APPLICATION_JSON))

        val exception =
            assertThrows(SubscriptionApprovalInvalidResponseException::class.java) {
                adapter.requestApproval()
            }

        assertTrue(exception.cause is CsrngInvalidResponseException)
        assertEquals(0, circuitBreaker.metrics.numberOfFailedCalls)
        server.verify()
    }

    @Test
    fun `5xx 재시도를 모두 소진하면 application 승인 장애로 정규화한다`() {
        server.expect(ExpectedCount.twice(), requestTo("https://csrng.test/csrng/csrng.php?min=0&max=1"))
            .andRespond(withServerError())

        val exception =
            assertThrows(SubscriptionApprovalUnavailableException::class.java) {
                adapter.requestApproval()
            }

        assertTrue(exception.cause is CsrngUnavailableException)
        assertEquals(1, circuitBreaker.metrics.numberOfFailedCalls)
        server.verify()
    }

    @Test
    fun `서킷이 열려 있으면 HTTP 호출 없이 즉시 실패한다`() {
        val circuitBreaker = createCircuitBreaker()
        circuitBreaker.transitionToOpenState()
        val builder = RestClient.builder().baseUrl("https://csrng.test")
        val unopenedServer = MockRestServiceServer.bindTo(builder).build()
        val openCircuitAdapter = CsrngSubscriptionApprovalAdapter(builder.build(), retry(), circuitBreaker)

        val exception =
            assertThrows(SubscriptionApprovalUnavailableException::class.java) {
                openCircuitAdapter.requestApproval()
            }
        assertTrue(exception.cause is CallNotPermittedException)
        unopenedServer.verify()
    }

    @Test
    fun `예상하지 못한 런타임 예외는 승인 장애로 숨기지 않는다`() {
        val unexpected = IllegalStateException("unexpected client failure")
        val restClient =
            RestClient.builder()
                .baseUrl("https://csrng.test")
                .requestInterceptor { _, _, _ -> throw unexpected }
                .build()
        val unexpectedFailureAdapter = CsrngSubscriptionApprovalAdapter(restClient, retry(), createCircuitBreaker())

        val exception =
            assertThrows(IllegalStateException::class.java) {
                unexpectedFailureAdapter.requestApproval()
            }

        assertEquals(unexpected, exception)
    }

    private fun retry(): Retry {
        val config =
            RetryConfig.custom<Any>()
                .maxAttempts(2)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(1, 2.0, 0.5))
                .retryExceptions(CsrngUnavailableException::class.java)
                .build()
        return Retry.of("csrng-test", config)
    }

    private fun createCircuitBreaker(): CircuitBreaker {
        val config =
            CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .recordExceptions(CsrngUnavailableException::class.java)
                .build()
        return CircuitBreaker.of("csrng-test", config)
    }
}
