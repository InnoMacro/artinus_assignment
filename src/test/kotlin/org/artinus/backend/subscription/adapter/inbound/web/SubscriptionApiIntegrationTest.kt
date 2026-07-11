package org.artinus.backend.subscription.adapter.inbound.web

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.artinus.backend.TestcontainersConfiguration
import org.artinus.backend.subscription.adapter.outbound.persistence.QSubscriptionHistoryJpaEntity
import org.artinus.backend.subscription.adapter.outbound.persistence.QSubscriptionMemberJpaEntity
import org.artinus.backend.subscription.application.port.outbound.ApprovalDecision
import org.artinus.backend.subscription.application.port.outbound.SubscriptionApprovalPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@Import(TestcontainersConfiguration::class, SubscriptionApiIntegrationTest.ApprovalTestConfiguration::class)
@SpringBootTest
@Transactional
class SubscriptionApiIntegrationTest @Autowired constructor(
    private val context: WebApplicationContext,
    private val approvalPort: ControllableApprovalPort,
    private val entityManager: EntityManager,
) {
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        approvalPort.decision = ApprovalDecision.APPROVED
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    @Test
    fun `구독 후 해지 API를 호출하면 현재 상태와 이력이 함께 변경된다`() {
        performChange("/api/v1/subscriptions", "BASIC")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("BASIC"))

        performChange("/api/v1/subscriptions/unsubscribe", "NONE")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("NONE"))

        entityManager.flush()
        val queryFactory = JPAQueryFactory(entityManager)
        val member = QSubscriptionMemberJpaEntity.subscriptionMemberJpaEntity
        val history = QSubscriptionHistoryJpaEntity.subscriptionHistoryJpaEntity

        assertEquals(
            0.toByte(),
            queryFactory.select(member.status).from(member)
                .where(member.phoneNumber.eq("01012345678"))
                .fetchOne()
                ?.dbCode,
        )
        assertEquals(
            2L,
            queryFactory.select(history.count()).from(history).fetchOne(),
        )
    }

    @Test
    fun `csrng 거절 시 422를 반환하고 회원과 이력을 저장하지 않는다`() {
        approvalPort.decision = ApprovalDecision.REJECTED

        performChange("/api/v1/subscriptions", "PREMIUM")
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("CSRNG_REJECTED"))

        entityManager.flush()
        val queryFactory = JPAQueryFactory(entityManager)
        val member = QSubscriptionMemberJpaEntity.subscriptionMemberJpaEntity
        val history = QSubscriptionHistoryJpaEntity.subscriptionHistoryJpaEntity

        assertEquals(0L, queryFactory.select(member.count()).from(member).fetchOne())
        assertEquals(0L, queryFactory.select(history.count()).from(history).fetchOne())
    }

    private fun performChange(path: String, status: String) =
        mockMvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"phoneNumber":"01012345678","channelId":1,"targetStatus":"$status"}""",
                ),
        )

    class ControllableApprovalPort : SubscriptionApprovalPort {
        var decision: ApprovalDecision = ApprovalDecision.APPROVED

        override fun requestApproval(): ApprovalDecision = decision
    }

    @TestConfiguration(proxyBeanMethods = false)
    class ApprovalTestConfiguration {
        @Bean
        @Primary
        fun controllableApprovalPort(): ControllableApprovalPort = ControllableApprovalPort()
    }
}
