package com.example.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room entity representing a payment event detected by the listener.
 * Synchronized with backend Cloud Function & Firestore collection `payment_listener_events`.
 */
@Entity(
    tableName = "payment_events",
    indices = [
        Index(value = ["fingerprint"], unique = true),
        Index(value = ["transactionId"]),
        Index(value = ["status"])
    ]
)
data class PaymentEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fingerprint: String,
    val method: String,
    val transactionId: String,
    val amount: Double,
    val sender: String,
    val receiver: String? = null,
    val reference: String? = null,
    val source: String, // "sms", "notification", "test_mode"
    val rawText: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val status: String = "received", // "received", "parsed", "matched", "approved", "rejected", "ambiguous", "unmatched", "error"
    val matchedPaymentRequestId: String? = null,
    val verificationDetails: String? = null,
    val retryCount: Int = 0,
    val lastRetryTimestamp: Long = 0L,
    val errorMessage: String? = null
)
