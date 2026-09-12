package com.example.ui

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.PaymentListenerApplication
import com.example.models.BackendMatchResult
import com.example.models.PaymentConfig
import com.example.models.PaymentEvent
import com.example.models.PaymentParseResult
import com.example.models.PaymentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = PaymentListenerApplication.instance.repository
    private val parserEngine = PaymentListenerApplication.instance.parserEngine
    private val backendService = PaymentListenerApplication.instance.backendService

    // Navigation & Filters
    private val _selectedTab = MutableStateFlow(0) // 0: Dashboard, 1: Transactions, 2: Test Mode, 3: Settings
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _filterStatus = MutableStateFlow<String?>(null) // null for all
    val filterStatus: StateFlow<String?> = _filterStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Config state
    val config: StateFlow<PaymentConfig> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaymentConfig())

    // All events raw
    private val rawEvents: StateFlow<List<PaymentEvent>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered events
    val filteredEvents: StateFlow<List<PaymentEvent>> = combine(
        rawEvents,
        _filterStatus,
        _searchQuery
    ) { events, filter, query ->
        events.filter { event ->
            val statusMatches = filter == null || event.status.equals(filter, ignoreCase = true)
            val queryMatches = query.isBlank() ||
                    event.transactionId.contains(query, ignoreCase = true) ||
                    event.sender.contains(query, ignoreCase = true) ||
                    event.method.contains(query, ignoreCase = true)
            statusMatches && queryMatches
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregates
    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val approvedCount: StateFlow<Int> = repository.approvedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val unmatchedCount: StateFlow<Int> = repository.unmatchedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val ambiguousCount: StateFlow<Int> = repository.ambiguousCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val errorCount: StateFlow<Int> = repository.errorCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    // Detail dialog state
    private val _selectedEvent = MutableStateFlow<PaymentEvent?>(null)
    val selectedEvent: StateFlow<PaymentEvent?> = _selectedEvent.asStateFlow()

    // Test Sandbox state
    private val _testInput = MutableStateFlow(
        "You have received Tk 500.00 from 01712345678. Fee Tk 0.00. Balance Tk 1,500.00. TrxID BKA8921XYZ at 12/09/2026 14:30"
    )
    val testInput: StateFlow<String> = _testInput.asStateFlow()

    private val _testParseResult = MutableStateFlow<PaymentParseResult?>(null)
    val testParseResult: StateFlow<PaymentParseResult?> = _testParseResult.asStateFlow()

    private val _testMatchResult = MutableStateFlow<BackendMatchResult?>(null)
    val testMatchResult: StateFlow<BackendMatchResult?> = _testMatchResult.asStateFlow()

    // Sample pending requests for simulation sandbox
    private val samplePendingRequests = listOf(
        PaymentRequest(
            id = "req_hsc_01",
            userId = "usr_rahim_99",
            userEmail = "rahim@example.com",
            fromNumber = "01712345678",
            transactionId = "BKA8921XYZ",
            method = "bKash",
            plan = "HSC Exam Prep Complete",
            price = 500.0,
            finalPrice = 500.0,
            status = "pending",
            orderId = "ORD-2026-901"
        ),
        PaymentRequest(
            id = "req_admission_02",
            userId = "usr_karim_44",
            userEmail = "karim@example.com",
            fromNumber = "01812345678",
            transactionId = "72N0ABCD",
            method = "Nagad",
            plan = "Medical Admission Bundle",
            price = 650.0,
            finalPrice = 650.0,
            status = "pending",
            orderId = "ORD-2026-902"
        ),
        PaymentRequest(
            id = "req_ambiguous_03a",
            userId = "usr_fahim_1",
            userEmail = "fahim1@example.com",
            fromNumber = "01912345678",
            transactionId = "DUPLICATE_TRX_999",
            method = "Rocket",
            plan = "General Knowledge Master",
            price = 300.0,
            finalPrice = 300.0,
            status = "pending",
            orderId = "ORD-2026-903"
        ),
        PaymentRequest(
            id = "req_ambiguous_03b",
            userId = "usr_fahim_2",
            userEmail = "fahim2@example.com",
            fromNumber = "01912345678",
            transactionId = "DUPLICATE_TRX_999",
            method = "Rocket",
            plan = "General Knowledge Master",
            price = 300.0,
            finalPrice = 300.0,
            status = "pending",
            orderId = "ORD-2026-904"
        )
    )

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setFilterStatus(status: String?) {
        _filterStatus.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectEvent(event: PaymentEvent?) {
        _selectedEvent.value = event
    }

    fun toggleMethod(method: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = config.value
            val updated = when (method.lowercase()) {
                "bkash" -> current.copy(bkashEnabled = enabled)
                "nagad" -> current.copy(nagadEnabled = enabled)
                "rocket" -> current.copy(rocketEnabled = enabled)
                "upay" -> current.copy(upayEnabled = enabled)
                else -> current
            }
            repository.updateConfig(updated)
        }
    }

    fun saveConfig(newConfig: PaymentConfig) {
        viewModelScope.launch {
            repository.updateConfig(newConfig)
        }
    }

    fun syncQueue() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncPendingQueue()
                _lastSyncTimestamp.value = System.currentTimeMillis()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun setTestInput(text: String) {
        _testInput.value = text
    }

    fun runTestParse() {
        val input = _testInput.value
        val result = parserEngine.testParse(input, source = "test_mode")
        _testParseResult.value = result
        _testMatchResult.value = null
    }

    fun runTestBackendMatch() {
        val parsed = _testParseResult.value ?: return
        val dummyEvent = PaymentEvent(
            id = 9999,
            fingerprint = parsed.createFingerprint(),
            method = parsed.method,
            transactionId = parsed.transactionId,
            amount = parsed.amount,
            sender = parsed.sender,
            receiver = parsed.receiver,
            reference = parsed.reference,
            source = "test_mode",
            rawText = parsed.rawText,
            receivedAt = parsed.timestamp,
            status = "parsed"
        )
        val matchResult = backendService.simulateBackendMatch(dummyEvent, samplePendingRequests)
        _testMatchResult.value = matchResult
    }

    fun saveTestEventToQueue() {
        val parsed = _testParseResult.value ?: return
        viewModelScope.launch {
            repository.processIncomingPayment(parsed)
            _selectedTab.value = 1 // Switch to Transactions view
        }
    }

    fun clearAllEvents() {
        viewModelScope.launch {
            repository.clearAllEvents()
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteEvent(id)
            if (_selectedEvent.value?.id == id) {
                _selectedEvent.value = null
            }
        }
    }

    fun manuallyMarkStatus(id: Long, status: String, notes: String) {
        viewModelScope.launch {
            repository.manuallyUpdateStatus(id, status, notes)
            val updated = rawEvents.value.find { it.id == id }?.copy(status = status, verificationDetails = notes)
            _selectedEvent.value = updated
        }
    }

    fun checkNotificationAccess(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }
}
