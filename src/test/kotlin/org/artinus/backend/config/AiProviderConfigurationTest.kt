package org.artinus.backend.config

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class AiProviderConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(AiProviderConfiguration::class.java)

    @Test
    fun `지원하지 않는 chat provider는 시작 단계에서 거부한다`() {
        contextRunner
            .withPropertyValues("spring.ai.model.chat=opneai")
            .run { context ->
                val failure = context.startupFailure
                assertNotNull(failure)
                assertTrue(requireNotNull(failure).rootCause().message.orEmpty().contains("none 또는 openai"))
            }
    }

    @Test
    fun `none과 openai provider는 허용한다`() {
        listOf("none", "openai").forEach { provider ->
            contextRunner
                .withPropertyValues("spring.ai.model.chat=$provider")
                .run { context ->
                    assertNull(context.startupFailure)
                }
        }
    }

    private fun Throwable.rootCause(): Throwable =
        generateSequence(this) { it.cause }
            .last()
}
