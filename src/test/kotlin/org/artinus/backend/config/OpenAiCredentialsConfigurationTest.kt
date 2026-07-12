package org.artinus.backend.config

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class OpenAiCredentialsConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(OpenAiCredentialsConfiguration::class.java)

    @Test
    fun `OpenAI chat 활성화 시 API key가 비어 있으면 시작에 실패한다`() {
        contextRunner
            .withPropertyValues(
                "spring.ai.model.chat=openai",
                "spring.ai.openai.api-key=",
            )
            .run { context ->
                val failure = context.startupFailure
                assertNotNull(failure)
                assertTrue(requireNotNull(failure).rootCause().message.orEmpty().contains("OPENAI_API_KEY"))
            }
    }

    @Test
    fun `OpenAI chat이 비활성화되어 있으면 API key를 요구하지 않는다`() {
        contextRunner
            .withPropertyValues("spring.ai.model.chat=none")
            .run { context ->
                assertNull(context.startupFailure)
            }
    }

    private fun Throwable.rootCause(): Throwable =
        generateSequence(this) { it.cause }
            .last()
}
