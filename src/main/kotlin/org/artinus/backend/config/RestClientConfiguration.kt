package org.artinus.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RestClientConfiguration {
    @Bean
    fun restClientFactory(): RestClientFactory = RestClientFactory()
}
