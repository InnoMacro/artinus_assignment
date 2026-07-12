package org.artinus.backend.common.error

import org.apache.logging.log4j.LogManager
import org.artinus.backend.common.error.response.ApiErrorResponse
import org.artinus.backend.common.error.response.ApiErrorResponses
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
class CommonApiExceptionHandler {
    private val logger = LogManager.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class, HttpMessageNotReadableException::class)
    fun handleInvalidRequest(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.debug("Invalid API request. reason={}", exception.message)
        return ApiErrorResponses.response(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST",
            "요청 값이 올바르지 않습니다.",
        )
    }

    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun handleResourceNotFound(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.debug("Resource not found. reason={}", exception.message)
        return ApiErrorResponses.response(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "요청한 리소스를 찾을 수 없습니다.",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected server error", exception)
        return ApiErrorResponses.response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다.",
        )
    }
}
