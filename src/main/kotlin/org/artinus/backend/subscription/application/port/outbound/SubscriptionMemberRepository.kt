package org.artinus.backend.subscription.application.port.outbound

import org.artinus.backend.subscription.application.exception.SubscriptionChangeConflictException
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.artinus.backend.subscription.domain.vo.PhoneNumber

interface SubscriptionMemberRepository {
    /** 사전 검증용 비잠금 조회다. */
    fun findByPhoneNumber(phoneNumber: PhoneNumber): SubscriptionMember?

    /**
     * 활성 쓰기 transaction에서 회원을 비관적 잠금으로 조회한다.
     *
     * @throws SubscriptionChangeConflictException 잠금을 획득하지 못한 경우
     */
    fun findByPhoneNumberForUpdate(phoneNumber: PhoneNumber): SubscriptionMember?

    /**
     * 활성 쓰기 transaction에서 회원을 비관적 잠금으로 조회한다.
     *
     * @throws SubscriptionChangeConflictException 잠금을 획득하지 못한 경우
     */
    fun getByPhoneNumberForUpdate(phoneNumber: PhoneNumber): SubscriptionMember

    /**
     * @throws SubscriptionChangeConflictException 동시 생성으로 식별자 충돌이 발생한 경우
     */
    fun save(member: SubscriptionMember): SubscriptionMember
}
