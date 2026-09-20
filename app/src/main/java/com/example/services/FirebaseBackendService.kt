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
 * Diagnostic result for backend health tests.
 */
data class BackendHealthResult(
    val status: String, // "PASS", "FAIL", "WARNING"
    val reachable: Boolean,
    val httpCode: Int,
    val responseTimeMs: Long,
    val message: String,
    val details: String = ""
)

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
            put("deviceId", config.deviceId)
            put("environment", config.environment)
            // Informational only; server enforces authoritative tolerance
            put("clientConfigTolerance", config.globalTolerance)
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

                    // Parse server-side tolerance comparison breakdown if provided
                    val expectedAmount = if (respJson.has("expectedAmount")) respJson.optDouble("expectedAmount") else null
                    val receivedAmount = if (respJson.has("receivedAmount")) respJson.optDouble("receivedAmount") else event.amount
                    val tolerance = if (respJson.has("tolerance")) respJson.optDouble("tolerance") else null
                    val minimumAcceptedAmount = if (respJson.has("minimumAcceptedAmount")) respJson.optDouble("minimumAcceptedAmount") else null
                    val maximumAcceptedAmount = if (respJson.has("maximumAcceptedAmount")) respJson.optDouble("maximumAcceptedAmount") else null
                    val amountDifference = if (respJson.has("amountDifference")) respJson.optDouble("amountDifference") else null

                    return@withContext BackendMatchResult(
                        success = success,
                        status = status,
                        transactionId = trxId,
                        matchedRequestId = matchedId,
                        candidateCount = candidateCount,
                        candidateIds = candidateIds,
                        message = message,
                        expectedAmount = expectedAmount,
                        receivedAmount = receivedAmount,
                        tolerance = tolerance,
                        minimumAcceptedAmount = minimumAcceptedAmount,
                        maximumAcceptedAmount = maximumAcceptedAmount,
                        amountDifference = amountDifference
                    )
                } else {
                    // Non-200 response or server unreachable
                    Log.w(TAG, "Server returned error: ${response.code} $responseBody")
                    val errorMsg = when (response.code) {
                        401 -> "Unauthorized listener device. Please verify your API Key in Settings."
                        404 -> "Cloud Function endpoint not found. Verify the backend URL in Settings."
                        500 -> "Firebase Cloud Function encountered an internal error. Check Cloud Function logs."
                        503 -> "Firebase service unavailable. Event queued for offline retry."
                        else -> "HTTP ${response.code}: ${response.message}"
                    }
                    return@withContext BackendMatchResult(
                        success = false,
                        status = "error",
                        transactionId = event.transactionId,
                        message = errorMsg
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network failure contacting backend: ${e.message}")
            return@withContext BackendMatchResult(
                success = false,
                status = "error",
                transactionId = event.transactionId,
                message = "Network unavailable: The transaction has been saved locally and will retry automatically."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error processing response: ${e.message}", e)
            return@withContext BackendMatchResult(
                success = false,
                status = "error",
                transactionId = event.transactionId,
                message = "Backend communication error: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Diagnostic sandbox matcher for Test Mode: performs full matching simulation
     * against pending test requests with configurable tolerance without real money or mutations.
     */
    fun simulateBackendMatch(
        event: PaymentEvent,
        pendingRequests: List<com.example.models.PaymentRequest>,
        config: PaymentConfig = PaymentConfig()
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

        // Tolerance calculation
        val tolerance = config.resolveTolerance(candidate.plan, candidate.method)
        val (minAccepted, maxAccepted) = config.calculateAcceptedRange(expectedAmount, tolerance)
        val amountWithinTolerance = event.amount in minAccepted..maxAccepted
        val amountDifference = kotlin.math.abs(event.amount - expectedAmount)

        // Validate criteria: sender number, method, and amount within tolerance range
        val senderMatches = normSender.isBlank() || candidateSender.isBlank() || normSender == candidateSender
        val methodMatches = normMethod.equals(candidateMethod, ignoreCase = true)

        if (senderMatches && methodMatches && amountWithinTolerance) {
            val tolNote = if (tolerance > 0.0) {
                " (Tolerance: ±৳$tolerance | Accepted range: ৳${minAccepted.toInt()} – ৳${maxAccepted.toInt()})"
            } else {
                " (Exact matching)"
            }
            return BackendMatchResult(
                success = true,
                status = "approved",
                transactionId = normTrxId,
                matchedRequestId = candidate.id,
                candidateCount = 1,
                candidateIds = listOf(candidate.id),
                message = "SIMULATION: Auto-approved candidate for ${candidate.userEmail.ifBlank { candidate.userId }}! Received ৳${event.amount}, Expected ৳$expectedAmount$tolNote",
                expectedAmount = expectedAmount,
                receivedAmount = event.amount,
                tolerance = tolerance,
                minimumAcceptedAmount = minAccepted,
                maximumAcceptedAmount = maxAccepted,
                amountDifference = amountDifference
            )
        } else {
            val failureReasons = mutableListOf<String>()
            if (!senderMatches) failureReasons.add("Sender mismatch (Event: $normSender, Request: $candidateSender)")
            if (!methodMatches) failureReasons.add("Method mismatch (Event: $normMethod, Request: $candidateMethod)")
            if (!amountWithinTolerance) {
                if (event.amount < minAccepted) {
                    failureReasons.add("Amount ৳${event.amount} is below minimum accepted ৳${minAccepted} (Expected: ৳$expectedAmount, Tolerance: ৳$tolerance)")
                } else {
                    failureReasons.add("Amount ৳${event.amount} exceeds maximum accepted ৳${maxAccepted} (Expected: ৳$expectedAmount, Tolerance: ৳$tolerance)")
                }
            }

            return BackendMatchResult(
                success = false,
                status = "rejected",
                transactionId = normTrxId,
                matchedRequestId = candidate.id,
                candidateCount = 1,
                candidateIds = listOf(candidate.id),
                message = "SIMULATION: Rejected: " + failureReasons.joinToString(", "),
                expectedAmount = expectedAmount,
                receivedAmount = event.amount,
                tolerance = tolerance,
                minimumAcceptedAmount = minAccepted,
                maximumAcceptedAmount = maxAccepted,
                amountDifference = amountDifference
            )
        }
    }

    /**
     * Diagnostic backend connectivity and health check.
     */
    suspend fun checkBackendHealth(config: PaymentConfig): BackendHealthResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (config.backendUrl.isBlank()) {
            return@withContext BackendHealthResult(
                status = "FAIL",
                reachable = false,
                httpCode = 0,
                responseTimeMs = 0,
                message = "Backend URL is empty. Please enter a valid HTTPS URL in Settings."
            )
        }

        if (!config.backendUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext BackendHealthResult(
                status = "WARNING",
                reachable = false,
                httpCode = 0,
                responseTimeMs = 0,
                message = "Production security policy requires HTTPS endpoints. Current: ${config.backendUrl}"
            )
        }

        val testPayload = JSONObject().apply {
            put("action", "ping")
            put("testMode", true)
            put("deviceId", config.deviceId)
            put("timestamp", System.currentTimeMillis())
        }

        val requestBody = testPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(config.backendUrl)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .header("X-API-Key", config.apiKey)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                val code = response.code
                val body = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    return@withContext BackendHealthResult(
                        status = "PASS",
                        reachable = true,
                        httpCode = code,
                        responseTimeMs = elapsed,
                        message = "Connected to Firebase Cloud Function (${elapsed}ms). Ready for production sync.",
                        details = body.take(200)
                    )
                } else if (code == 401) {
                    return@withContext BackendHealthResult(
                        status = "FAIL",
                        reachable = true,
                        httpCode = code,
                        responseTimeMs = elapsed,
                        message = "Endpoint reached, but authentication failed (401 Unauthorized). Verify API Key."
                    )
                } else {
                    return@withContext BackendHealthResult(
                        status = "WARNING",
                        reachable = true,
                        httpCode = code,
                        responseTimeMs = elapsed,
                        message = "Endpoint reached with HTTP $code. Ensure the processPaymentListenerEvent Cloud Function is deployed.",
                        details = body.take(200)
                    )
                }
            }
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - startTime
            return@withContext BackendHealthResult(
                status = "FAIL",
                reachable = false,
                httpCode = 0,
                responseTimeMs = elapsed,
                message = "Connection failed: ${e.localizedMessage ?: "Network error"}. Check internet or endpoint domain."
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            return@withContext BackendHealthResult(
                status = "FAIL",
                reachable = false,
                httpCode = 0,
                responseTimeMs = elapsed,
                message = "Diagnostic error: ${e.localizedMessage}"
            )
        }
    }
}
