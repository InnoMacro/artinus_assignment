package org.artinus.backend.subscription.adapter.outbound.csrng.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("external.csrng")
data class CsrngProperties(
    val baseUrl: String = "https://csrng.net",
    val connectTimeout: Duration = Duration.ofSeconds(1),
    val readTimeout: Duration = Duration.ofSeconds(2),
)
