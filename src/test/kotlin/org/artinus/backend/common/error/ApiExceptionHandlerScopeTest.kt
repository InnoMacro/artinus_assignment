package org.artinus.backend.common.error

import org.artinus.backend.subscription.adapter.inbound.web.error.SubscriptionApiExceptionHandler
import org.artinus.backend.subscription.domain.exception.InvalidPhoneNumberException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class ApiExceptionHandlerScopeTest {
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders.standaloneSetup(NonSubscriptionController())
                .setControllerAdvice(CommonApiExceptionHandler(), SubscriptionApiExceptionHandler())
                .build()
    }

    @Test
    fun `subscription 범위 밖의 controller에는 subscription advice가 적용되지 않는다`() {
        mockMvc.perform(get("/test/non-subscription/business-error"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
    }

    @RestController
    private class NonSubscriptionController {
        @GetMapping("/test/non-subscription/business-error")
        fun fail(): Nothing = throw InvalidPhoneNumberException()
    }
}
