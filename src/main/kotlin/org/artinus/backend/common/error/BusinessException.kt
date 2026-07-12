package org.artinus.backend.common.error

abstract class BusinessException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
