package org.artinus.backend.subscription.adapter.outbound.csrng

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import org.artinus.backend.subscription.adapter.outbound.csrng.exception.CsrngInvalidResponseException
import org.artinus.backend.subscription.adapter.outbound.csrng.exception.CsrngUnavailableException
import org.artinus.backend.subscription.adapter.outbound.csrng.response.CsrngResponse
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalInvalidResponseException
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalUnavailableException
import org.artinus.backend.subscription.application.port.outbound.ApprovalDecision
import org.artinus.backend.subscription.application.port.outbound.SubscriptionApprovalPort
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

@Component
class CsrngSubscriptionApprovalAdapter(
    @Qualifier("csrngRestClient") private val restClient: RestClient,
    @Qualifier("csrngRetry") private val retry: Retry,
    @Qualifier("csrngCircuitBreaker") private val circuitBreaker: CircuitBreaker,
) : SubscriptionApprovalPort {
    override fun requestApproval(): ApprovalDecision =
        try {
            circuitBreaker.executeSupplier {
                retry.executeSupplier {
                    requestOnce()
                }
            }
        } catch (exception: CsrngInvalidResponseException) {
            throw SubscriptionApprovalInvalidResponseException(exception)
        } catch (exception: CsrngUnavailableException) {
            throw SubscriptionApprovalUnavailableException(exception)
        } catch (exception: CallNotPermittedException) {
            throw SubscriptionApprovalUnavailableException(exception)
        }

    private fun requestOnce(): ApprovalDecision {
        val response =
            try {
                restClient.get()
                    .uri("/csrng/csrng.php?min=0&max=1")
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError) { _, httpResponse ->
                        throw CsrngUnavailableException("csrng 서버 오류: ${httpResponse.statusCode}")
                    }
                    .body(Array<CsrngResponse>::class.java)
            } catch (exception: CsrngUnavailableException) {
                throw exception
            } catch (exception: ResourceAccessException) {
                throw CsrngUnavailableException("csrng 연결 또는 응답 시간 초과", exception)
            } catch (exception: RestClientResponseException) {
                throw CsrngInvalidResponseException("csrng HTTP 응답을 처리할 수 없습니다.", exception)
            } catch (exception: RestClientException) {
                throw CsrngInvalidResponseException("csrng 응답을 해석할 수 없습니다.", exception)
            }

        val item = response?.singleOrNull()
            ?: throw CsrngInvalidResponseException("csrng 응답은 항목 하나를 포함해야 합니다.")
        if (item.status != "success") {
            throw CsrngInvalidResponseException("csrng 응답 상태가 success가 아닙니다.")
        }

        return when (item.random) {
            1 -> ApprovalDecision.APPROVED
            0 -> ApprovalDecision.REJECTED
            else -> throw CsrngInvalidResponseException("csrng random 값은 0 또는 1이어야 합니다.")
        }
    }
}
