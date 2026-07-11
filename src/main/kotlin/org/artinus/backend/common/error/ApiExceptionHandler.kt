package org.artinus.backend.common.error

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.artinus.backend.channel.domain.ChannelActionNotAllowedException
import org.artinus.backend.subscription.adapter.outbound.csrng.CsrngInvalidResponseException
import org.artinus.backend.subscription.adapter.outbound.csrng.CsrngUnavailableException
import org.artinus.backend.subscription.application.service.ChannelNotFoundException
import org.artinus.backend.subscription.application.service.SubscriptionApprovalRejectedException
import org.artinus.backend.subscription.application.service.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.domain.InvalidSubscriptionTransitionException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class, HttpMessageNotReadableException::class)
    fun handleInvalidRequest(exception: Exception): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다.")

    @ExceptionHandler(InvalidSubscriptionTransitionException::class)
    fun handleInvalidTransition(exception: InvalidSubscriptionTransitionException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", exception.message)

    @ExceptionHandler(ChannelActionNotAllowedException::class)
    fun handleChannelActionNotAllowed(exception: ChannelActionNotAllowedException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.FORBIDDEN, "CHANNEL_ACTION_NOT_ALLOWED", exception.message)

    @ExceptionHandler(ChannelNotFoundException::class)
    fun handleChannelNotFound(exception: ChannelNotFoundException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND", exception.message)

    @ExceptionHandler(SubscriptionMemberNotFoundException::class)
    fun handleMemberNotFound(exception: SubscriptionMemberNotFoundException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", exception.message)

    @ExceptionHandler(SubscriptionApprovalRejectedException::class)
    fun handleApprovalRejected(exception: SubscriptionApprovalRejectedException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.UNPROCESSABLE_ENTITY, "CSRNG_REJECTED", exception.message)

    @ExceptionHandler(CsrngInvalidResponseException::class)
    fun handleInvalidCsrngResponse(exception: CsrngInvalidResponseException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.BAD_GATEWAY, "CSRNG_INVALID_RESPONSE", exception.message)

    @ExceptionHandler(CsrngUnavailableException::class, CallNotPermittedException::class)
    fun handleCsrngUnavailable(exception: Exception): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.SERVICE_UNAVAILABLE, "CSRNG_UNAVAILABLE", "외부 승인 서비스를 사용할 수 없습니다.")

    @ExceptionHandler(
        CannotAcquireLockException::class,
        PessimisticLockingFailureException::class,
        DataIntegrityViolationException::class,
    )
    fun handleConflict(exception: Exception): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.CONFLICT, "SUBSCRIPTION_CONFLICT", "다른 구독 변경 요청과 충돌했습니다.")

    private fun response(
        status: HttpStatus,
        code: String,
        message: String?,
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(ApiErrorResponse(code, message ?: status.reasonPhrase))
}
