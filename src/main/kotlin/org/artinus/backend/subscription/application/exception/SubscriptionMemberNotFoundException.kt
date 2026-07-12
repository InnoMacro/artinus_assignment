package org.artinus.backend.subscription.application.exception

import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.artinus.backend.subscription.domain.exception.SubscriptionBusinessException

class SubscriptionMemberNotFoundException(
    val phoneNumber: PhoneNumber,
) : SubscriptionBusinessException("구독 회원을 찾을 수 없습니다.")
