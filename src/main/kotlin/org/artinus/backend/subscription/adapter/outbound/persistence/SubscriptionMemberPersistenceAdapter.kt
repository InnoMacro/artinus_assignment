package org.artinus.backend.subscription.adapter.outbound.persistence

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.springframework.stereotype.Repository

@Repository
class SubscriptionMemberPersistenceAdapter(
    private val entityManager: EntityManager,
) : SubscriptionMemberRepository {
    private val queryFactory = JPAQueryFactory(entityManager)
    private val member = QSubscriptionMemberJpaEntity.subscriptionMemberJpaEntity

    override fun findByPhoneNumberForUpdate(phoneNumber: PhoneNumber): SubscriptionMember? =
        queryFactory
            .selectFrom(member)
            .where(member.phoneNumber.eq(phoneNumber.value))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .fetchOne()
            ?.toDomain()

    override fun save(member: SubscriptionMember): SubscriptionMember {
        val entity =
            member.id?.let { memberId ->
                queryFactory
                    .selectFrom(this.member)
                    .where(this.member.id.eq(memberId.value))
                    .fetchOne()
                    ?.also { it.updateFrom(member) }
                    ?: throw IllegalStateException("저장할 회원 Entity를 찾을 수 없습니다: ${memberId.value}")
            } ?: SubscriptionMemberJpaEntity.from(member).also(entityManager::persist)

        return entity.toDomain()
    }
}
