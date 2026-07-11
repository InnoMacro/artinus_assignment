package org.artinus.backend.subscription.adapter.outbound.persistence

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.springframework.stereotype.Repository

@Repository
class SubscriptionMemberPersistenceAdapter(
    private val queryFactory: JPAQueryFactory,
    private val jpaRepository: SubscriptionMemberJpaRepository,
) : SubscriptionMemberRepository {
    private val member = QSubscriptionMemberJpaEntity.subscriptionMemberJpaEntity

    override fun findByPhoneNumberForUpdate(phoneNumber: PhoneNumber): SubscriptionMember? =
        queryFactory
            .selectFrom(member)
            .where(member.phoneNumber.eq(phoneNumber.value))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint("jakarta.persistence.lock.timeout", 2_000)
            .fetchOne()
            ?.toDomain()

    override fun save(member: SubscriptionMember): SubscriptionMember {
        val entity =
            member.id?.let { memberId ->
                jpaRepository.findById(memberId.value)
                    .orElseThrow {
                        IllegalStateException("저장할 회원 Entity를 찾을 수 없습니다: ${memberId.value}")
                    }
                    .also { it.updateFrom(member) }
            } ?: jpaRepository.save(SubscriptionMemberJpaEntity.from(member))

        return entity.toDomain()
    }
}
