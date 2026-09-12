package com.example.models

/**
 * Representation of an existing Prostuti pending payment request in Firestore.
 */
data class PaymentRequest(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val fromNumber: String = "",
    val transactionId: String = "",
    val method: String = "",
    val plan: String = "",
    val price: Double = 0.0,
    val finalPrice: Double = 0.0,
    val status: String = "pending",
    val orderId: String = "",
    val timestamp: Long = 0L
) {
    /**
     * Resolves the expected payable amount prioritizing finalPrice over price.
     */
    val expectedAmount: Double
        get() = if (finalPrice > 0.0) finalPrice else price
}

/**
 * Result returned by the backend matching engine or Cloud Function.
 */
data class BackendMatchResult(
    val success: Boolean,
    val status: String, // "approved", "unmatched", "ambiguous", "duplicate", "error"
    val transactionId: String,
    val matchedRequestId: String? = null,
    val candidateCount: Int = 0,
    val candidateIds: List<String> = emptyList(),
    val message: String? = null,
    val auditTrail: Map<String, Any?> = emptyMap()
)
