package org.artinus.backend.subscription.adapter.outbound.csrng

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@Configuration
@EnableConfigurationProperties(CsrngProperties::class)
class CsrngClientConfiguration {
    @Bean("csrngRestClient")
    fun csrngRestClient(properties: CsrngProperties): RestClient {
        val httpClient =
            HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(properties.readTimeout)

        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    @Bean("csrngRetry")
    fun csrngRetry(registry: RetryRegistry): Retry = registry.retry("csrng")

    @Bean("csrngCircuitBreaker")
    fun csrngCircuitBreaker(registry: CircuitBreakerRegistry): CircuitBreaker =
        registry.circuitBreaker("csrng")
}
