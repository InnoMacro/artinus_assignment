package org.artinus.backend.config

import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

class RestClientFactory {
    fun create(
        builder: RestClient.Builder,
        baseUrl: String,
        connectTimeout: Duration,
        readTimeout: Duration,
    ): RestClient {
        val httpClient =
            HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(readTimeout)

        return builder.clone()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
