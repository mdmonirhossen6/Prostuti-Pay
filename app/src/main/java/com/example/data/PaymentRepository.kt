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
        private const val MAX_RETRIES = 5
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
     * Submits an individual event to the backend and updates its status.
     */
    suspend fun syncEventToBackend(event: PaymentEvent, config: PaymentConfig): BackendMatchResult = withContext(Dispatchers.IO) {
        val result = backendService.submitPaymentEvent(event, config)

        val updatedEvent = if (result.success) {
            event.copy(
                status = result.status,
                matchedPaymentRequestId = result.matchedRequestId,
                verificationDetails = result.message,
                errorMessage = null
            )
        } else {
            // Failed network request: keep in offline queue with incremented retry
            val newRetryCount = event.retryCount + 1
            val newStatus = if (newRetryCount >= MAX_RETRIES) "error" else "parsed"
            event.copy(
                status = newStatus,
                retryCount = newRetryCount,
                lastRetryTimestamp = System.currentTimeMillis(),
                errorMessage = result.message
            )
        }

        eventDao.update(updatedEvent)
        return@withContext result
    }

    /**
     * Flushes and synchronizes all pending offline events to Firebase.
     */
    suspend fun syncPendingQueue(): Int = withContext(Dispatchers.IO) {
        val pendingEvents = eventDao.getPendingSyncEvents()
        if (pendingEvents.isEmpty()) return@withContext 0

        val currentConfig = configDao.getConfigSync() ?: PaymentConfig()
        var syncedCount = 0

        for (event in pendingEvents) {
            // Exponential backoff check: don't retry too quickly if failed recently
            val backoffMillis = (1L shl event.retryCount.coerceAtMost(6)) * 2000L
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

    suspend fun clearAllEvents() = withContext(Dispatchers.IO) {
        eventDao.clearAll()
    }

    suspend fun deleteEvent(id: Long) = withContext(Dispatchers.IO) {
        eventDao.deleteById(id)
    }

    suspend fun manuallyUpdateStatus(id: Long, status: String, notes: String? = null) = withContext(Dispatchers.IO) {
        val event = eventDao.getById(id) ?: return@withContext
        eventDao.update(
            event.copy(
                status = status,
                verificationDetails = notes ?: event.verificationDetails
            )
        )
    }
}
