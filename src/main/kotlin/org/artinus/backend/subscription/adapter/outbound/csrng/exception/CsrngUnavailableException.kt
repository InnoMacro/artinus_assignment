package org.artinus.backend.subscription.adapter.outbound.csrng.exception

class CsrngUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
