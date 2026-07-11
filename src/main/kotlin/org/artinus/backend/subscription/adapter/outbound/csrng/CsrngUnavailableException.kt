package org.artinus.backend.subscription.adapter.outbound.csrng

class CsrngUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
