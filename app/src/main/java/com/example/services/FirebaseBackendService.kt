package com.example.services

import android.util.Log
import com.example.models.BackendMatchResult
import com.example.models.PaymentConfig
import com.example.models.PaymentEvent
import com.example.parsers.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service handling secure communication between the Android helper app and
 * Firebase Cloud Functions / backend endpoints.
 *
 * Implements safe offline handling, idempotency checks, and client-safe operation.
 */
class FirebaseBackendService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "FirebaseBackendService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Submits a parsed payment event to the Firebase Cloud Function endpoint.
     * The cloud function validates schema, prevents duplicates, checks pending payment_requests,
     * and auto-approves or marks as unmatched/ambiguous.
     */
    suspend fun submitPaymentEvent(
        event: PaymentEvent,
        config: PaymentConfig
    ): BackendMatchResult = withContext(Dispatchers.IO) {
        val jsonPayload = JSONObject().apply {
            put("fingerprint", event.fingerprint)
            put("method", Normalizer.normalizeMethod(event.method))
            put("transactionId", Normalizer.normalizeTransactionId(event.transactionId))
            put("amount", event.amount)
            put("sender", Normalizer.normalizePhoneNumber(event.sender))
            put("receiver", event.receiver ?: JSONObject.NULL)
            put("reference", event.reference ?: JSONObject.NULL)
            put("source", event.source)
            put("rawText", event.rawText)
            put("timestamp", event.receivedAt)
        }

        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url(config.backendUrl)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("X-API-Key", config.apiKey)
            requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                Log.d(TAG, "Backend HTTP response: code=${response.code}, body=$responseBody")

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val respJson = JSONObject(responseBody)
                    val success = respJson.optBoolean("success", true)
                    val status = respJson.optString("status", "received")
                    val trxId = respJson.optString("transactionId", event.transactionId)
                    val matchedId = respJson.optString("matchedRequestId", null)
                    val candidateCount = respJson.optInt("candidateCount", if (matchedId != null) 1 else 0)
                    val message = respJson.optString("message", "")

                    val candidateIds = mutableListOf<String>()
                    val candArr = respJson.optJSONArray("candidateIds")
                    if (candArr != null) {
                        for (i in 0 until candArr.length()) {
                            candidateIds.add(candArr.getString(i))
                        }
                    } else if (!matchedId.isNullOrBlank()) {
                        candidateIds.add(matchedId)
                    }

                    return@withContext BackendMatchResult(
                        success = success,
                        status = status,
                        transactionId = trxId,
                        matchedRequestId = matchedId,
                        candidateCount = candidateCount,
                        candidateIds = candidateIds,
                        message = message
                    )
                } else {
                    // Non-200 response or server unreachable
                    Log.w(TAG, "Server returned error: ${response.code} $responseBody")
                    // If endpoint returned 404/500/timeout, fall back to offline queue with status "parsed"
                    return@withContext BackendMatchResult(
                        success = false,
                        status = "error",
                        transactionId = event.transactionId,
                        message = "HTTP ${response.code}: ${response.message}"
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network failure contacting backend: ${e.message}")
            return@withContext BackendMatchResult(
                success = false,
                status = "error",
                transactionId = event.transactionId,
                message = "Network unavailable: ${e.localizedMessage ?: "Connection error"}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error processing response: ${e.message}", e)
            return@withContext BackendMatchResult(
                success = false,
                status = "error",
                transactionId = event.transactionId,
                message = "Error: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Diagnostic sandbox matcher for Test Mode: performs full matching simulation
     * against pending test requests without real money or mutations.
     */
    fun simulateBackendMatch(
        event: PaymentEvent,
        pendingRequests: List<com.example.models.PaymentRequest>
    ): BackendMatchResult {
        val normTrxId = Normalizer.normalizeTransactionId(event.transactionId)
        val normSender = Normalizer.normalizePhoneNumber(event.sender)
        val normMethod = Normalizer.normalizeMethod(event.method)

        // Find candidate pending requests matching TrxID
        val candidates = pendingRequests.filter { req ->
            req.status.equals("pending", ignoreCase = true) &&
                    Normalizer.normalizeTransactionId(req.transactionId) == normTrxId
        }

        if (candidates.isEmpty()) {
            return BackendMatchResult(
                success = true,
                status = "unmatched",
                transactionId = normTrxId,
                candidateCount = 0,
                candidateIds = emptyList(),
                message = "No pending request found with TrxID $normTrxId. Saved for delayed matching."
            )
        }

        if (candidates.size > 1) {
            return BackendMatchResult(
                success = true,
                status = "ambiguous",
                transactionId = normTrxId,
                candidateCount = candidates.size,
                candidateIds = candidates.map { it.id },
                message = "Found ${candidates.size} duplicate pending requests. Flagged for manual review."
            )
        }

        val candidate = candidates.first()
        val candidateSender = Normalizer.normalizePhoneNumber(candidate.fromNumber)
        val candidateMethod = Normalizer.normalizeMethod(candidate.method)
        val expectedAmount = candidate.expectedAmount

        // Validate secondary criteria: sender number, method, and amount >= expectedAmount
        val senderMatches = normSender.isBlank() || candidateSender.isBlank() || normSender == candidateSender
        val methodMatches = normMethod.equals(candidateMethod, ignoreCase = true)
        val amountSufficient = event.amount >= expectedAmount

        if (senderMatches && methodMatches && amountSufficient) {
            return BackendMatchResult(
                success = true,
                status = "approved",
                transactionId = normTrxId,
                matchedRequestId = candidate.id,
                candidateCount = 1,
                candidateIds = listOf(candidate.id),
                message = "Auto-approved! Match confirmed for user ${candidate.userEmail.ifBlank { candidate.userId }} (Expected: ৳$expectedAmount, Received: ৳${event.amount})"
            )
        } else {
            val failureReasons = mutableListOf<String>()
            if (!senderMatches) failureReasons.add("Sender mismatch (Event: $normSender, Request: $candidateSender)")
            if (!methodMatches) failureReasons.add("Method mismatch (Event: $normMethod, Request: $candidateMethod)")
            if (!amountSufficient) failureReasons.add("Insufficient amount (Received: ৳${event.amount}, Expected: ৳$expectedAmount)")

            return BackendMatchResult(
                success = false,
                status = "rejected",
                transactionId = normTrxId,
                matchedRequestId = candidate.id,
                candidateCount = 1,
                candidateIds = listOf(candidate.id),
                message = "Rejected: " + failureReasons.joinToString(", ")
            )
        }
    }
}
