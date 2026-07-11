package org.artinus.backend.subscription.application.exception

import org.artinus.backend.subscription.domain.PhoneNumber

class SubscriptionMemberNotFoundException(
    val phoneNumber: PhoneNumber,
) : RuntimeException("구독 회원을 찾을 수 없습니다.")
