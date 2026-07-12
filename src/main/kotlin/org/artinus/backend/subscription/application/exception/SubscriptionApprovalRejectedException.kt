package org.artinus.backend.subscription.application.exception

import org.artinus.backend.subscription.domain.exception.SubscriptionBusinessException

class SubscriptionApprovalRejectedException :
    SubscriptionBusinessException("외부 승인 결과 구독 상태 변경이 거절되었습니다.")
