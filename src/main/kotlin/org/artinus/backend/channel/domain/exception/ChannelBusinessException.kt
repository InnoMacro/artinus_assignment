package org.artinus.backend.channel.domain.exception

import org.artinus.backend.common.error.BusinessException

abstract class ChannelBusinessException(
    message: String,
    cause: Throwable? = null,
) : BusinessException(message, cause)
