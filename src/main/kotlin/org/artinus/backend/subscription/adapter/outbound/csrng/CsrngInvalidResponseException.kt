package org.artinus.backend.subscription.adapter.outbound.csrng

class CsrngInvalidResponseException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
