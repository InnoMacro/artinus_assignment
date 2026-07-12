package org.artinus.backend.subscription.adapter.inbound.web.error

import org.apache.logging.log4j.LogManager
import org.artinus.backend.channel.application.exception.ChannelNotFoundException
import org.artinus.backend.channel.domain.exception.ChannelActionNotAllowedException
import org.artinus.backend.common.error.response.ApiErrorResponse
import org.artinus.backend.common.error.response.ApiErrorResponses
import org.artinus.backend.subscription.adapter.inbound.web.SubscriptionController
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalInvalidResponseException
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalRejectedException
import org.artinus.backend.subscription.application.exception.SubscriptionApprovalUnavailableException
import org.artinus.backend.subscription.application.exception.SubscriptionChangeConflictException
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.domain.exception.InvalidPhoneNumberException
import org.artinus.backend.subscription.domain.exception.InvalidSubscriptionTransitionException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = [SubscriptionController::class])
class SubscriptionApiExceptionHandler {
    private val logger = LogManager.getLogger(javaClass)

    @ExceptionHandler(InvalidPhoneNumberException::class)
    fun handleInvalidPhoneNumber(exception: InvalidPhoneNumberException): ResponseEntity<ApiErrorResponse> {
        logger.debug("Invalid phone number request")
        return ApiErrorResponses.response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.message)
    }

    @ExceptionHandler(InvalidSubscriptionTransitionException::class)
    fun handleInvalidTransition(exception: InvalidSubscriptionTransitionException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Invalid subscription transition. reason={}", exception.message)
        return ApiErrorResponses.response(
            HttpStatus.BAD_REQUEST,
            "INVALID_STATUS_TRANSITION",
            exception.message,
        )
    }

    @ExceptionHandler(ChannelActionNotAllowedException::class)
    fun handleChannelActionNotAllowed(exception: ChannelActionNotAllowedException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Channel action not allowed. reason={}", exception.message)
        return ApiErrorResponses.response(
            HttpStatus.FORBIDDEN,
            "CHANNEL_ACTION_NOT_ALLOWED",
            exception.message,
        )
    }

    @ExceptionHandler(ChannelNotFoundException::class)
    fun handleChannelNotFound(exception: ChannelNotFoundException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Channel not found. channelId={}", exception.channelId.value)
        return ApiErrorResponses.response(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND", exception.message)
    }

    @ExceptionHandler(SubscriptionMemberNotFoundException::class)
    fun handleMemberNotFound(exception: SubscriptionMemberNotFoundException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Subscription member not found. phoneNumber={}", exception.phoneNumber.masked())
        return ApiErrorResponses.response(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", exception.message)
    }

    @ExceptionHandler(SubscriptionApprovalRejectedException::class)
    fun handleApprovalRejected(exception: SubscriptionApprovalRejectedException): ResponseEntity<ApiErrorResponse> {
        logger.info("Subscription change rejected by approval provider")
        return ApiErrorResponses.response(HttpStatus.UNPROCESSABLE_ENTITY, "CSRNG_REJECTED", exception.message)
    }

    @ExceptionHandler(SubscriptionApprovalInvalidResponseException::class)
    fun handleInvalidApprovalResponse(
        exception: SubscriptionApprovalInvalidResponseException,
    ): ResponseEntity<ApiErrorResponse> {
        logger.error("Invalid approval provider response", exception)
        return ApiErrorResponses.response(HttpStatus.BAD_GATEWAY, "CSRNG_INVALID_RESPONSE", exception.message)
    }

    @ExceptionHandler(SubscriptionApprovalUnavailableException::class)
    fun handleApprovalUnavailable(
        exception: SubscriptionApprovalUnavailableException,
    ): ResponseEntity<ApiErrorResponse> {
        logger.error("Approval provider unavailable", exception)
        return ApiErrorResponses.response(HttpStatus.SERVICE_UNAVAILABLE, "CSRNG_UNAVAILABLE", exception.message)
    }

    @ExceptionHandler(SubscriptionChangeConflictException::class)
    fun handleConflict(exception: SubscriptionChangeConflictException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Subscription change conflict. causeType={}", exception.cause?.javaClass?.simpleName)
        return ApiErrorResponses.response(
            HttpStatus.CONFLICT,
            "SUBSCRIPTION_CONFLICT",
            exception.message,
        )
    }
}
