package org.artinus.backend.subscription.adapter.inbound.web

import org.artinus.backend.common.error.ApiExceptionHandler
import org.artinus.backend.channel.application.exception.ChannelNotFoundException
import org.artinus.backend.channel.domain.ChannelId
import org.artinus.backend.subscription.application.port.inbound.SubscribeUseCase
import org.artinus.backend.subscription.application.port.inbound.UnsubscribeUseCase
import org.artinus.backend.subscription.application.result.ChangeSubscriptionResult
import org.artinus.backend.subscription.domain.MemberId
import org.artinus.backend.subscription.domain.PhoneNumber
import org.artinus.backend.subscription.domain.SubscriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class SubscriptionControllerTest {
    private val subscribeUseCase = RecordingSubscribeUseCase()
    private val unsubscribeUseCase = RecordingUnsubscribeUseCase()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val controller = SubscriptionController(subscribeUseCase, unsubscribeUseCase)
        mockMvc =
            MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(ApiExceptionHandler())
                .build()
    }

    @Test
    fun `구독 요청을 처리한다`() {
        mockMvc.perform(
            post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"phoneNumber":"010-1234-5678","channelId":1,"targetStatus":"BASIC"}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.memberId").value(1))
            .andExpect(jsonPath("$.phoneNumber").value("01012345678"))
            .andExpect(jsonPath("$.status").value("BASIC"))

        assertEquals(SubscriptionStatus.BASIC, subscribeUseCase.lastTarget)
    }

    @Test
    fun `구독 해지 요청을 처리한다`() {
        mockMvc.perform(
            post("/api/v1/subscriptions/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"phoneNumber":"01012345678","channelId":1,"targetStatus":"NONE"}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("NONE"))

        assertEquals(SubscriptionStatus.NONE, unsubscribeUseCase.lastTarget)
    }

    @Test
    fun `잘못된 휴대폰 번호는 400 오류로 응답한다`() {
        mockMvc.perform(
            post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"phoneNumber":"02-123-4567","channelId":1,"targetStatus":"BASIC"}""",
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `존재하지 않는 채널 ID는 메시지를 포함한 404 오류로 응답한다`() {
        subscribeUseCase.failure = ChannelNotFoundException(ChannelId(99))

        mockMvc.perform(
            post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"phoneNumber":"01012345678","channelId":99,"targetStatus":"BASIC"}""",
                ),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CHANNEL_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("채널을 찾을 수 없습니다. channelId=99"))
    }

    @Test
    fun `예상하지 못한 예외는 상세 내용을 숨긴 500 오류로 응답한다`() {
        subscribeUseCase.failure = IllegalStateException("내부 상세 정보")

        mockMvc.perform(
            post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"phoneNumber":"01012345678","channelId":1,"targetStatus":"BASIC"}""",
                ),
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
    }

    @Test
    fun `존재하지 않는 리소스는 404 오류로 응답한다`() {
        mockMvc.perform(get("/does-not-exist"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    }

    private class RecordingSubscribeUseCase : SubscribeUseCase {
        var lastTarget: SubscriptionStatus? = null
        var failure: RuntimeException? = null

        override fun subscribe(command: org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand): ChangeSubscriptionResult {
            failure?.let { throw it }
            lastTarget = command.targetStatus
            return result(command.targetStatus)
        }
    }

    private class RecordingUnsubscribeUseCase : UnsubscribeUseCase {
        var lastTarget: SubscriptionStatus? = null

        override fun unsubscribe(command: org.artinus.backend.subscription.application.command.ChangeSubscriptionCommand): ChangeSubscriptionResult {
            lastTarget = command.targetStatus
            return result(command.targetStatus)
        }
    }

    companion object {
        private fun result(status: SubscriptionStatus) =
            ChangeSubscriptionResult(
                memberId = MemberId(1),
                phoneNumber = PhoneNumber("01012345678"),
                status = status,
            )
    }
}
