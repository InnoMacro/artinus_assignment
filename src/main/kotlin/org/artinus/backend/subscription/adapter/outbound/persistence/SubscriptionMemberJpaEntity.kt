package org.artinus.backend.subscription.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.artinus.backend.subscription.domain.vo.MemberId
import org.artinus.backend.subscription.domain.vo.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionMember
import org.artinus.backend.subscription.domain.vo.SubscriptionStatus

@Entity
@Table(name = "subscription_member")
class SubscriptionMemberJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    val phoneNumber: String,
    @Convert(converter = SubscriptionStatusConverter::class)
    @Column(
        name = "status",
        nullable = false,
        columnDefinition = "TINYINT",
        comment = "구독 상태: 0=NONE, 1=BASIC, 2=PREMIUM",
    )
    var status: SubscriptionStatus,
) {
    fun toDomain(): SubscriptionMember =
        SubscriptionMember.restore(
            id = MemberId(requireNotNull(id) { "저장되지 않은 회원 Entity입니다." }),
            phoneNumber = PhoneNumber(phoneNumber),
            status = status,
        )

    fun updateFrom(domain: SubscriptionMember) {
        require(id == domain.id?.value) { "서로 다른 회원으로 Entity를 갱신할 수 없습니다." }
        status = domain.status
    }

    companion object {
        fun from(domain: SubscriptionMember): SubscriptionMemberJpaEntity =
            SubscriptionMemberJpaEntity(
                id = domain.id?.value,
                phoneNumber = domain.phoneNumber.value,
                status = domain.status,
            )
    }
}
