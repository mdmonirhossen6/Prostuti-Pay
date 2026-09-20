package com.example.data

import android.util.Log
import com.example.models.BackendMatchResult
import com.example.models.PaymentConfig
import com.example.models.PaymentEvent
import com.example.models.PaymentParseResult
import com.example.services.FirebaseBackendService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single source of truth for payment events, offline queue synchronization,
 * duplicate detection, and configuration persistence.
 */
class PaymentRepository(
    private val eventDao: PaymentEventDao,
    private val configDao: PaymentConfigDao,
    private val backendService: FirebaseBackendService = FirebaseBackendService(),
    private val repositoryScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    companion object {
        private const val TAG = "PaymentRepository"
        // 9-step backoff intervals: 5s, 15s, 30s, 1m, 2m, 5m, 10m, 15m, 30m
        val RETRY_DELAYS_MS = listOf(
            5_000L,
            15_000L,
            30_000L,
            60_000L,
            120_000L,
            300_000L,
            600_000L,
            900_000L,
            1_800_000L
        )
    }

    val allEvents: Flow<List<PaymentEvent>> = eventDao.getAllEvents()

    val config: Flow<PaymentConfig> = configDao.getConfig().map { it ?: PaymentConfig() }

    val totalCount: Flow<Int> = eventDao.getTotalCount()
    val approvedCount: Flow<Int> = eventDao.getCountByStatus("approved")
    val unmatchedCount: Flow<Int> = eventDao.getCountByStatus("unmatched")
    val ambiguousCount: Flow<Int> = eventDao.getCountByStatus("ambiguous")
    val errorCount: Flow<Int> = eventDao.getCountByStatus("error")

    /**
     * Ingests a new parsed payment transaction.
     * Enforces local duplicate protection and triggers backend sync.
     */
    suspend fun processIncomingPayment(parseResult: PaymentParseResult): PaymentEvent = withContext(Dispatchers.IO) {
        val fingerprint = parseResult.createFingerprint()

        // 1. Check local processed-event cache by fingerprint
        val existingByFingerprint = eventDao.getByFingerprint(fingerprint)
        if (existingByFingerprint != null) {
            Log.i(TAG, "Duplicate payment event detected (fingerprint: $fingerprint). Skipping insert.")
            return@withContext existingByFingerprint
        }

        // Also check by Transaction ID
        val existingByTrx = eventDao.getByTransactionId(parseResult.transactionId)
        if (existingByTrx != null) {
            Log.i(TAG, "Transaction ID already recorded: ${parseResult.transactionId}. Skipping duplicate.")
            return@withContext existingByTrx
        }

        val initialAudit = org.json.JSONArray().apply {
            put(org.json.JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("action", "PARSED")
                put("details", "Ingested from ${parseResult.source}. Method: ${parseResult.method}, TrxID: ${parseResult.transactionId}, Amount: ${parseResult.amount}")
            })
        }

        // 2. Insert new event as "received"
        val newEvent = PaymentEvent(
            fingerprint = fingerprint,
            method = parseResult.method,
            transactionId = parseResult.transactionId,
            amount = parseResult.amount,
            sender = parseResult.sender,
            receiver = parseResult.receiver,
            reference = parseResult.reference,
            source = parseResult.source,
            rawText = parseResult.rawText,
            receivedAt = parseResult.timestamp,
            status = "received",
            syncStatus = "pending",
            backendStatus = "pending",
            auditLogJson = initialAudit.toString(),
            retryCount = 0
        )

        val insertedId = eventDao.insert(newEvent)
        val eventWithId = newEvent.copy(id = insertedId)

        // 3. Immediately attempt backend sync
        val currentConfig = configDao.getConfigSync() ?: PaymentConfig()
        if (currentConfig.autoSyncEnabled) {
            syncEventToBackend(eventWithId, currentConfig)
        }

        return@withContext eventDao.getById(insertedId) ?: eventWithId
    }

    /**
     * Submits an individual event to the backend and updates its status and tolerance audit trail.
     */
    suspend fun syncEventToBackend(event: PaymentEvent, config: PaymentConfig): BackendMatchResult = withContext(Dispatchers.IO) {
        val result = backendService.submitPaymentEvent(event, config)

        val auditArray = try {
            org.json.JSONArray(event.auditLogJson)
        } catch (_: Exception) {
            org.json.JSONArray()
        }

        auditArray.put(org.json.JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("action", "BACKEND_SYNC_ATTEMPT")
            put("success", result.success)
            put("status", result.status)
            put("message", result.message)
            if (result.expectedAmount != null) put("expectedAmount", result.expectedAmount)
            if (result.tolerance != null) put("tolerance", result.tolerance)
            if (result.amountDifference != null) put("amountDifference", result.amountDifference)
        })

        val now = System.currentTimeMillis()
        val maxRetries = config.retryMaxAttempts.coerceAtLeast(1)

        val updatedEvent = if (result.success) {
            event.copy(
                status = result.status,
                syncStatus = "synced",
                backendStatus = result.status,
                matchedPaymentRequestId = result.matchedRequestId,
                verificationDetails = result.message,
                errorMessage = null,
                expectedAmount = result.expectedAmount ?: event.expectedAmount,
                tolerance = result.tolerance ?: event.tolerance,
                minAcceptedAmount = result.minimumAcceptedAmount ?: event.minAcceptedAmount,
                maxAcceptedAmount = result.maximumAcceptedAmount ?: event.maxAcceptedAmount,
                amountDifference = result.amountDifference ?: event.amountDifference,
                backendMessage = result.message,
                updatedAt = now,
                auditLogJson = auditArray.toString()
            )
        } else {
            val newRetryCount = event.retryCount + 1
            val newStatus = if (newRetryCount >= maxRetries) "error" else "parsed"
            event.copy(
                status = newStatus,
                syncStatus = "failed",
                backendStatus = "error",
                retryCount = newRetryCount,
                lastRetryTimestamp = now,
                errorMessage = result.message,
                backendMessage = result.message,
                updatedAt = now,
                auditLogJson = auditArray.toString()
            )
        }

        eventDao.update(updatedEvent)
        return@withContext result
    }

    /**
     * Flushes and synchronizes all pending offline events to Firebase using structured backoff.
     */
    suspend fun syncPendingQueue(): Int = withContext(Dispatchers.IO) {
        val pendingEvents = eventDao.getPendingSyncEvents()
        if (pendingEvents.isEmpty()) return@withContext 0

        val currentConfig = configDao.getConfigSync() ?: PaymentConfig()
        var syncedCount = 0

        for (event in pendingEvents) {
            // Check structured backoff window
            val retryIdx = (event.retryCount - 1).coerceIn(0, RETRY_DELAYS_MS.lastIndex)
            val backoffMillis = if (event.retryCount > 0) RETRY_DELAYS_MS[retryIdx] else 0L
            val elapsed = System.currentTimeMillis() - event.lastRetryTimestamp
            if (event.retryCount > 0 && elapsed < backoffMillis) {
                continue // Skip until backoff window expires
            }

            val result = syncEventToBackend(event, currentConfig)
            if (result.success) {
                syncedCount++
            }
        }
        return@withContext syncedCount
    }

    suspend fun updateConfig(newConfig: PaymentConfig) = withContext(Dispatchers.IO) {
        configDao.insertOrUpdate(newConfig)
    }

    suspend fun resetConfigToDefaults() = withContext(Dispatchers.IO) {
        configDao.insertOrUpdate(PaymentConfig())
    }

    suspend fun clearAllEvents() = withContext(Dispatchers.IO) {
        eventDao.clearAll()
    }

    suspend fun deleteEvent(id: Long) = withContext(Dispatchers.IO) {
        eventDao.deleteById(id)
    }

    suspend fun manuallyUpdateStatus(id: Long, status: String, notes: String? = null) = withContext(Dispatchers.IO) {
        val event = eventDao.getById(id) ?: return@withContext
        val auditArray = try {
            org.json.JSONArray(event.auditLogJson)
        } catch (_: Exception) {
            org.json.JSONArray()
        }
        auditArray.put(org.json.JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("action", "MANUAL_STATUS_UPDATE")
            put("previousStatus", event.status)
            put("newStatus", status)
            put("notes", notes ?: "")
        })

        eventDao.update(
            event.copy(
                status = status,
                verificationDetails = notes ?: event.verificationDetails,
                backendStatus = status,
                updatedAt = System.currentTimeMillis(),
                auditLogJson = auditArray.toString()
            )
        )
    }
}
