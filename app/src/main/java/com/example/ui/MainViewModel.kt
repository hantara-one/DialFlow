package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DialerPreferences
import com.example.data.QueueRepository
import com.example.domain.CallQueueManager
import com.example.domain.NumberParser
import com.example.domain.QueueEvent
import com.example.models.CallingMode
import com.example.models.ObservedCallState
import com.example.models.PhoneNumberEntry
import com.example.models.QueueExecutionState
import com.example.models.QueueSummary
import com.example.telephony.CallObserverSource
import com.example.telephony.CallStateObserver
import com.example.telephony.DialerRoleHelper
import com.example.telephony.TelecomCallController
import com.example.telephony.TelephonyCallStateObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val preferences = DialerPreferences(context)
    private val database = AppDatabase.getInstance(context)
    private val repository = QueueRepository(database.queueDao())
    private val callController = TelecomCallController(context)
    private val callStateObserver: CallStateObserver =
        TelephonyCallStateObserver(context, viewModelScope)

    val queueManager = CallQueueManager(
        repository = repository,
        callController = callController,
        callStateObserver = callStateObserver,
        scope = viewModelScope,
        initialCallingMode = preferences.callingMode,
        initialDelaySeconds = preferences.autoAdvanceDelaySeconds
    )

    val queueItems: StateFlow<List<PhoneNumberEntry>> = queueManager.queueItems
    val queueSummary: StateFlow<QueueSummary> = queueManager.queueSummary
    val currentEntry: StateFlow<PhoneNumberEntry?> = queueManager.currentEntry
    val manualCursorIndex: StateFlow<Int?> = queueManager.manualCursorIndex
    val executionState: StateFlow<QueueExecutionState> = queueManager.executionState
    val observedCallState: StateFlow<ObservedCallState> = callStateObserver.callState
    val observerSource: StateFlow<CallObserverSource> = callStateObserver.observerSource
    val callingMode: StateFlow<CallingMode> = queueManager.callingMode
    val autoAdvanceDelaySeconds: StateFlow<Int> = queueManager.autoAdvanceDelaySeconds
    val countdownRemaining: StateFlow<Int?> = queueManager.countdownRemaining
    val events: SharedFlow<QueueEvent> = queueManager.events

    private val _isDefaultDialer = MutableStateFlow(false)
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    private val _hasCallPhonePermission = MutableStateFlow(false)
    val hasCallPhonePermission: StateFlow<Boolean> = _hasCallPhonePermission.asStateFlow()

    private val _hasReadPhoneStatePermission = MutableStateFlow(false)
    val hasReadPhoneStatePermission: StateFlow<Boolean> = _hasReadPhoneStatePermission.asStateFlow()

    private val _rawInputText = MutableStateFlow("")
    val rawInputText: StateFlow<String> = _rawInputText.asStateFlow()

    private val _lastImportStats = MutableStateFlow<String?>(null)
    val lastImportStats: StateFlow<String?> = _lastImportStats.asStateFlow()

    init {
        refreshTelephonyAndPermissionState()
        callStateObserver.startObserving()
    }

    override fun onCleared() {
        super.onCleared()
        callStateObserver.stopObserving()
    }

    fun updateRawInputText(text: String) {
        _rawInputText.value = text
    }

    fun refreshTelephonyAndPermissionState() {
        _isDefaultDialer.value = DialerRoleHelper.isDefaultDialer(context)
        _hasCallPhonePermission.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        _hasReadPhoneStatePermission.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun importNumbers(rawText: String? = null) {
        val textToParse = rawText ?: _rawInputText.value
        if (textToParse.isBlank()) return

        val currentCount = queueItems.value.size
        val parseResult = NumberParser.parse(textToParse, startIndex = currentCount)

        if (parseResult.entries.isNotEmpty()) {
            viewModelScope.launch {
                repository.insertEntries(parseResult.entries)
                _rawInputText.value = ""
                _lastImportStats.value = "${parseResult.totalCount} numbers imported (${parseResult.validCount} valid, ${parseResult.invalidCount} flagged)"
            }
        }
    }

    fun loadSampleIndonesianNumbers() {
        _rawInputText.value = """
            081234567890
            082345678901
            +628567890123
            0812-9999-8888
        """.trimIndent()
    }

    fun dismissImportStats() {
        _lastImportStats.value = null
    }

    fun startQueue(preferDirectCall: Boolean = true) {
        refreshTelephonyAndPermissionState()
        queueManager.startQueue(preferDirectCall)
    }

    fun pauseQueue() {
        queueManager.pauseQueue()
    }

    fun stopQueue() {
        queueManager.stopQueue()
    }

    fun skipCurrent(preferDirectCall: Boolean = true) {
        queueManager.skipCurrent(preferDirectCall)
    }

    fun advanceNext(success: Boolean = true, preferDirectCall: Boolean = true) {
        queueManager.advanceNext(success, preferDirectCall)
    }

    fun navigateReturn() {
        queueManager.navigateReturn()
    }

    fun navigateBack() {
        queueManager.navigateReturn()
    }

    fun beginCall(preferDirectCall: Boolean = true) {
        refreshTelephonyAndPermissionState()
        queueManager.beginManualCall(preferDirectCall)
    }

    fun callNext(preferDirectCall: Boolean = true) {
        refreshTelephonyAndPermissionState()
        if (callingMode.value == CallingMode.MANUAL_NEXT) {
            queueManager.beginManualCall(preferDirectCall)
        } else {
            queueManager.callNext(preferDirectCall)
        }
    }

    fun callSpecificEntry(entry: PhoneNumberEntry, preferDirectCall: Boolean = true) {
        queueManager.callSpecificEntry(entry, preferDirectCall)
    }

    fun clearQueue() {
        queueManager.clearQueue()
    }

    fun deleteEntry(id: Long) {
        queueManager.deleteEntry(id)
    }

    fun retryEntry(id: Long) {
        queueManager.retryEntry(id)
    }

    fun setCallingMode(mode: CallingMode) {
        preferences.callingMode = mode
        queueManager.setCallingMode(mode)
    }

    fun setAutoAdvanceDelay(seconds: Int) {
        preferences.autoAdvanceDelaySeconds = seconds
        queueManager.setAutoAdvanceDelay(seconds)
    }
}
