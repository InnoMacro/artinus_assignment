package org.artinus.backend.subscription.application.port.outbound

/**
 * 구독 변경 가능 여부만 판정하는 무상태 외부 포트다.
 *
 * 호출 자체가 외부의 영속적인 비즈니스 상태를 생성하거나 변경해서는 안 된다.
 * 승인되면 [ApprovalDecision.APPROVED], 거절되면 [ApprovalDecision.REJECTED]를 반환하고,
 * 통신 실패나 해석할 수 없는 응답은 application 예외로 변환한다.
 */
fun interface SubscriptionApprovalPort {
    fun requestApproval(): ApprovalDecision
}
