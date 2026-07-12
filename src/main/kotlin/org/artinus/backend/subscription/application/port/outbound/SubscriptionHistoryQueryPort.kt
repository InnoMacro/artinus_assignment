package org.artinus.backend.subscription.application.port.outbound

import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.artinus.backend.subscription.domain.vo.PhoneNumber

fun interface SubscriptionHistoryQueryPort {
    /**
     * 회원의 구독 변경 이력을 조회한다.
     *
     * 반환 이력은 [SubscriptionHistoryItem.changedAt] 오름차순으로 정렬되며,
     * 변경 시각이 같으면 [SubscriptionHistoryItem.id] 오름차순으로 정렬된다.
     *
     * @return 회원이 없으면 `null`, 회원은 있지만 변경 이력이 없으면 빈 목록
     */
    fun findAllByPhoneNumber(phoneNumber: PhoneNumber): List<SubscriptionHistoryItem>?
}
