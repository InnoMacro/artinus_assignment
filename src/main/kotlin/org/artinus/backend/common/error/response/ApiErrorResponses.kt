package org.artinus.backend.common.error.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object ApiErrorResponses {
    fun response(
        status: HttpStatus,
        code: String,
        message: String?,
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(ApiErrorResponse(code, message ?: status.reasonPhrase))
}
