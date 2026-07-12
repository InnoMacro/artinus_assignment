package org.artinus.backend.subscription.adapter.outbound.persistence

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import jakarta.persistence.LockTimeoutException
import jakarta.persistence.PessimisticLockException
import org.artinus.backend.subscription.application.exception.SubscriptionChangeConflictException
import org.artinus.backend.subscription.application.exception.SubscriptionMemberNotFoundException
import org.artinus.backend.subscription.application.port.outbound.SubscriptionMemberRepository
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.hibernate.exception.LockAcquisitionException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.stereotype.Repository

@Repository
class SubscriptionMemberPersistenceAdapter(
    private val queryFactory: JPAQueryFactory,
    private val jpaRepository: SubscriptionMemberJpaRepository,
) : SubscriptionMemberRepository {
    private val member = QSubscriptionMemberJpaEntity.subscriptionMemberJpaEntity

    override fun findByPhoneNumber(phoneNumber: PhoneNumber): SubscriptionMember? =
        queryFactory
            .selectFrom(member)
            .where(member.phoneNumber.eq(phoneNumber.value))
            .fetchOne()
            ?.toDomain()

    override fun findByPhoneNumberForUpdate(phoneNumber: PhoneNumber): SubscriptionMember? =
        translateLockConflict {
            queryFactory
                .selectFrom(member)
                .where(member.phoneNumber.eq(phoneNumber.value))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("jakarta.persistence.lock.timeout", 2_000)
                .fetchOne()
                ?.toDomain()
        }

    override fun getByPhoneNumberForUpdate(phoneNumber: PhoneNumber): SubscriptionMember =
        findByPhoneNumberForUpdate(phoneNumber)
            ?: throw SubscriptionMemberNotFoundException(phoneNumber)

    override fun save(member: SubscriptionMember): SubscriptionMember {
        val entity = try {
            member.id?.let { memberId ->
                jpaRepository.findById(memberId.value)
                    .orElseThrow {
                        IllegalStateException("저장할 회원 Entity를 찾을 수 없습니다: ${memberId.value}")
                    }
                    .also { it.updateFrom(member) }
            } ?: jpaRepository.save(SubscriptionMemberJpaEntity.from(member))
        } catch (exception: DataIntegrityViolationException) {
            if (exception.hasCauseMessage(PHONE_NUMBER_UNIQUE_CONSTRAINT)) {
                throw SubscriptionChangeConflictException(exception)
            }
            throw exception
        } catch (exception: PessimisticLockingFailureException) {
            throw SubscriptionChangeConflictException(exception)
        }

        return entity.toDomain()
    }

    private fun <T> translateLockConflict(block: () -> T): T =
        try {
            block()
        } catch (exception: RuntimeException) {
            if (exception.isLockConflict()) {
                throw SubscriptionChangeConflictException(exception)
            }
            throw exception
        }

    private fun Throwable.isLockConflict(): Boolean =
        causeSequence().any { cause ->
            cause is LockTimeoutException ||
                cause is PessimisticLockException ||
                cause is LockAcquisitionException ||
                cause is PessimisticLockingFailureException
        }

    private fun Throwable.hasCauseMessage(fragment: String): Boolean =
        causeSequence().any { cause -> cause.message?.contains(fragment, ignoreCase = true) == true }

    private fun Throwable.causeSequence(): Sequence<Throwable> = generateSequence(this) { it.cause }

    companion object {
        private const val PHONE_NUMBER_UNIQUE_CONSTRAINT = "uk_subscription_member_phone_number"
    }
}
