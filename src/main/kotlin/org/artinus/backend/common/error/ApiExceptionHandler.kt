package org.artinus.backend.common.error

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.apache.logging.log4j.LogManager
import org.artinus.backend.channel.application.exception.ChannelNotFoundException
import org.artinus.backend.channel.domain.ChannelActionNotAllowedException
import org.artinus.backend.subscription.adapter.outbound.csrng.CsrngInvalidResponseException
import org.artinus.backend.subscription.adapter.outbound.csrng.CsrngUnavailableException
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.application.service.SubscriptionApprovalRejectedException
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
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LogManager.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class, HttpMessageNotReadableException::class)
    fun handleInvalidRequest(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.debug("Invalid API request. reason={}", exception.message)
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다.")
    }

    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun handleResourceNotFound(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.debug("Resource not found. reason={}", exception.message)
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.")
    }

    @ExceptionHandler(InvalidSubscriptionTransitionException::class)
    fun handleInvalidTransition(exception: InvalidSubscriptionTransitionException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Invalid subscription transition. reason={}", exception.message)
        return response(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", exception.message)
    }

    @ExceptionHandler(ChannelActionNotAllowedException::class)
    fun handleChannelActionNotAllowed(exception: ChannelActionNotAllowedException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Channel action not allowed. reason={}", exception.message)
        return response(HttpStatus.FORBIDDEN, "CHANNEL_ACTION_NOT_ALLOWED", exception.message)
    }

    @ExceptionHandler(ChannelNotFoundException::class)
    fun handleChannelNotFound(exception: ChannelNotFoundException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Channel not found. channelId={}", exception.channelId.value)
        return response(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND", exception.message)
    }

    @ExceptionHandler(SubscriptionMemberNotFoundException::class)
    fun handleMemberNotFound(exception: SubscriptionMemberNotFoundException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Subscription member not found. phoneNumber={}", exception.phoneNumber.masked())
        return response(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", exception.message)
    }

    @ExceptionHandler(SubscriptionApprovalRejectedException::class)
    fun handleApprovalRejected(exception: SubscriptionApprovalRejectedException): ResponseEntity<ApiErrorResponse> {
        logger.info("Subscription change rejected by csrng")
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "CSRNG_REJECTED", exception.message)
    }

    @ExceptionHandler(CsrngInvalidResponseException::class)
    fun handleInvalidCsrngResponse(exception: CsrngInvalidResponseException): ResponseEntity<ApiErrorResponse> {
        logger.error("Invalid csrng response", exception)
        return response(HttpStatus.BAD_GATEWAY, "CSRNG_INVALID_RESPONSE", exception.message)
    }

    @ExceptionHandler(CsrngUnavailableException::class, CallNotPermittedException::class)
    fun handleCsrngUnavailable(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Csrng unavailable", exception)
        return response(HttpStatus.SERVICE_UNAVAILABLE, "CSRNG_UNAVAILABLE", "외부 승인 서비스를 사용할 수 없습니다.")
    }

    @ExceptionHandler(
        CannotAcquireLockException::class,
        PessimisticLockingFailureException::class,
        DataIntegrityViolationException::class,
    )
    fun handleConflict(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.warn("Subscription change conflict. type={}", exception.javaClass.simpleName)
        return response(HttpStatus.CONFLICT, "SUBSCRIPTION_CONFLICT", "다른 구독 변경 요청과 충돌했습니다.")
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected server error", exception)
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.")
    }

    private fun response(
        status: HttpStatus,
        code: String,
        message: String?,
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(status).body(ApiErrorResponse(code, message ?: status.reasonPhrase))
}
