package org.artinus.backend.subscription.adapter.outbound.csrng

data class CsrngResponse(
    val status: String? = null,
    val random: Int? = null,
)
