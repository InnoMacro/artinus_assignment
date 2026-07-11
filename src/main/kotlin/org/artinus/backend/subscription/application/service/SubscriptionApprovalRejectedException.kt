package org.artinus.backend.subscription.application.service

class SubscriptionApprovalRejectedException : RuntimeException("외부 승인 결과 구독 상태 변경이 거절되었습니다.")
