package org.artinus.backend.subscription.adapter.outbound.ai

enum class SubscriptionHistoryAiReasoningEffort {
    NONE,
    MINIMAL,
    LOW,
    MEDIUM,
    HIGH,
    XHIGH,
    ;

    val apiValue: String
        get() = name.lowercase()
}
