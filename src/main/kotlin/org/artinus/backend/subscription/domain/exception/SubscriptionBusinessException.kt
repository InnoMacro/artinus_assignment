package org.artinus.backend.subscription.domain.exception

import org.artinus.backend.common.error.BusinessException

abstract class SubscriptionBusinessException(
    message: String,
    cause: Throwable? = null,
) : BusinessException(message, cause)
