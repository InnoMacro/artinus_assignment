package org.artinus.backend.subscription.adapter.outbound.ai.prompt

import org.artinus.backend.subscription.application.result.SubscriptionHistoryItem
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import java.nio.charset.StandardCharsets

@Component
class SubscriptionHistoryPrompt {
    val systemText: String = SYSTEM_RESOURCE.getContentAsString(StandardCharsets.UTF_8).trim()
    private val userTemplate: String = USER_RESOURCE.getContentAsString(StandardCharsets.UTF_8).trim()

    fun renderUserPrompt(histories: List<SubscriptionHistoryItem>): String =
        userTemplate.replace(HISTORIES_PLACEHOLDER, histories.toPromptData())

    private fun List<SubscriptionHistoryItem>.toPromptData(): String =
        joinToString(
            separator = "\n",
            prefix = "<subscription_histories>\n",
            postfix = "\n</subscription_histories>",
        ) { history ->
            "  <history changedAt=\"${history.changedAt}\" " +
                "channelName=\"${HtmlUtils.htmlEscape(history.channelName)}\" " +
                "action=\"${history.action}\" " +
                "previousStatus=\"${history.previousStatus}\" " +
                "changedStatus=\"${history.changedStatus}\" />"
        }

    companion object {
        private const val HISTORIES_PLACEHOLDER = "{histories}"
        private val SYSTEM_RESOURCE =
            ClassPathResource("prompt/subscription-history-summary/system-v1.txt")
        private val USER_RESOURCE =
            ClassPathResource("prompt/subscription-history-summary/user-v1.txt")
    }
}
