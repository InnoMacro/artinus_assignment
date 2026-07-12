package org.artinus.backend.subscription.adapter.outbound.csrng.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import org.artinus.backend.config.RestClientFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(CsrngProperties::class)
class CsrngClientConfiguration {
    @Bean("csrngRestClient")
    fun csrngRestClient(
        builder: RestClient.Builder,
        restClientFactory: RestClientFactory,
        properties: CsrngProperties,
    ): RestClient =
        restClientFactory.create(
            builder = builder,
            baseUrl = properties.baseUrl,
            connectTimeout = properties.connectTimeout,
            readTimeout = properties.readTimeout,
        )

    @Bean("csrngRetry")
    fun csrngRetry(registry: RetryRegistry): Retry = registry.retry("csrng")

    @Bean("csrngCircuitBreaker")
    fun csrngCircuitBreaker(registry: CircuitBreakerRegistry): CircuitBreaker =
        registry.circuitBreaker("csrng")
}
