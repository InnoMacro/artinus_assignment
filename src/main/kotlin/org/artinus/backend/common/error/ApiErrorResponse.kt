package org.artinus.backend.common.error

import java.time.Instant

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
)
