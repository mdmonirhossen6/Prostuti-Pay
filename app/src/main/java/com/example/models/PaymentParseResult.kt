package com.example.models

/**
 * Normalized result of parsing a payment SMS or Notification text.
 * Represents raw extracted and normalized transaction evidence.
 */
data class PaymentParseResult(
    val method: String,
    val transactionId: String,
    val amount: Double,
    val sender: String,
    val receiver: String? = null,
    val reference: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "sms", // "sms", "notification", "test_mode"
    val rawText: String
) {
    /**
     * Creates a deterministic fingerprint to guard against duplicate SMS/Notification broadcasts.
     * Combines canonical payment method, uppercase transaction ID, and amount.
     */
    fun createFingerprint(): String {
        val cleanTrx = transactionId.trim().uppercase()
        val cleanMethod = method.trim().lowercase()
        val formattedAmount = String.format(java.util.Locale.US, "%.2f", amount)
        val rawHash = "$cleanMethod:$cleanTrx:$formattedAmount"
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(rawHash.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
