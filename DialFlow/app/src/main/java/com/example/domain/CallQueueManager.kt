package com.example.domain

import com.example.data.QueueRepository
import com.example.models.CallStatus
import com.example.models.CallingMode
import com.example.models.ObservedCallState
import com.example.models.PhoneNumberEntry
import com.example.models.PhoneNumberFormatPreference
import com.example.models.QueueExecutionState
import com.example.models.QueueSummary
import com.example.telephony.CallController
import com.example.telephony.CallInitiationResult
import com.example.telephony.CallStateObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class QueueEvent {
    data class CallPlaced(val number: String, val mode: String) : QueueEvent()
    data class PermissionNeeded(val permission: String) : QueueEvent()
    data class Error(val message: String) : QueueEvent()
    data class QueueFinished(val totalCalls: Int) : QueueEvent()
    data class AutoAdvanceCountdown(val secondsLeft: Int) : QueueEvent()
}

class CallQueueManager(
    private val repository: QueueRepository,
    private val callController: CallController,
    private val callStateObserver: CallStateObserver,
    private val scope: CoroutineScope,
    initialCallingMode: CallingMode = CallingMode.AUTO_CALL,
    initialDelaySeconds: Int = 3,
    initialNumberFormat: PhoneNumberFormatPreference = PhoneNumberFormatPreference.LOCAL
) {

    private val _executionState = MutableStateFlow(QueueExecutionState.IDLE)
    val executionState: StateFlow<QueueExecutionState> = _executionState.asStateFlow()

    private val _numberFormatPreference = MutableStateFlow(initialNumberFormat)
    val numberFormatPreference: StateFlow<PhoneNumberFormatPreference> = _numberFormatPreference.asStateFlow()

    fun setNumberFormatPreference(format: PhoneNumberFormatPreference) {
        _numberFormatPreference.value = format
    }

    private val _currentEntry = MutableStateFlow<PhoneNumberEntry?>(null)
    val currentEntry: StateFlow<PhoneNumberEntry?> = _currentEntry.asStateFlow()

    private val _manualCursorIndex = MutableStateFlow<Int?>(null)
    val manualCursorIndex: StateFlow<Int?> = _manualCursorIndex.asStateFlow()

    private val _events = MutableSharedFlow<QueueEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<QueueEvent> = _events.asSharedFlow()

    private val _callingMode = MutableStateFlow(initialCallingMode)
    val callingMode: StateFlow<CallingMode> = _callingMode.asStateFlow()

    private val _autoAdvanceDelaySeconds = MutableStateFlow(initialDelaySeconds.coerceIn(1, 30))
    val autoAdvanceDelaySeconds: StateFlow<Int> = _autoAdvanceDelaySeconds.asStateFlow()

    private val _countdownRemaining = MutableStateFlow<Int?>(null)
    val countdownRemaining: StateFlow<Int?> = _countdownRemaining.asStateFlow()

    private var autoAdvanceJob: Job? = null
    private var pausedCooldownSeconds: Int? = null
    private var callInProgress = false
    private var activeCallStartTime = 0L

    val queueItems: StateFlow<List<PhoneNumberEntry>> = repository.queueItems.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val queueSummary: StateFlow<QueueSummary> = combine(
        queueItems,
        _currentEntry,
        _executionState
    ) { items, current, execState ->
        val completed = items.count { it.status == CallStatus.COMPLETED }
        val failed = items.count { it.status == CallStatus.FAILED }
        val skipped = items.count { it.status == CallStatus.SKIPPED }
        val remaining = items.count { it.status == CallStatus.PENDING }
        QueueSummary(
            total = items.size,
            completed = completed,
            remaining = remaining,
            failed = failed,
            skipped = skipped,
            currentNumber = current,
            executionState = execState
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = QueueSummary()
    )

    init {
        // Observe telephony/InCallService call state transitions
        scope.launch {
            var lastState = ObservedCallState.IDLE
            callStateObserver.callState.collect { state ->
                handleCallStateTransition(oldState = lastState, newState = state)
                lastState = state
            }
        }

        // Observe queueItems to maintain cursor and current entry across both modes
        scope.launch {
            repository.queueItems.collect { items ->
                if (!callInProgress) {
                    syncManualCursorWithItems(items)
                }
            }
        }
    }

    private fun syncManualCursorWithItems(items: List<PhoneNumberEntry>) {
        if (items.isEmpty()) {
            _manualCursorIndex.value = null
            _currentEntry.value = null
            return
        }
        val currentTargetId = _currentEntry.value?.id
        if (currentTargetId != null) {
            val matchingIdx = items.indexOfFirst { it.id == currentTargetId }
            if (matchingIdx != -1) {
                _manualCursorIndex.value = matchingIdx
                _currentEntry.value = items[matchingIdx]
                return
            }
        }
        val currentIdx = _manualCursorIndex.value
        if (currentIdx == null) {
            // Automatically select the first pending number upon loading
            val firstPendingIdx = items.indexOfFirst { it.status == CallStatus.PENDING }
            val selectedIdx = if (firstPendingIdx != -1) firstPendingIdx else 0
            _manualCursorIndex.value = selectedIdx
            _currentEntry.value = items[selectedIdx]
        } else {
            // Retain the current position clamped to bounds and update data
            val clampedIdx = currentIdx.coerceIn(0, items.size - 1)
            _manualCursorIndex.value = clampedIdx
            _currentEntry.value = items[clampedIdx]
        }
    }

    fun setCallingMode(mode: CallingMode) {
        if (_callingMode.value == mode) return
        _callingMode.value = mode
        if (mode == CallingMode.MANUAL_NEXT) {
            autoAdvanceJob?.cancel()
            _countdownRemaining.value = null
            if (_executionState.value == QueueExecutionState.RUNNING && !callInProgress) {
                _executionState.value = QueueExecutionState.PAUSED
            }
            if (!callInProgress) {
                syncManualCursorWithItems(queueItems.value)
            }
        } else {
            // Switched to AUTO_CALL
            if (!callInProgress) {
                syncManualCursorWithItems(queueItems.value)
            }
        }
    }

    private fun handleCallStateTransition(oldState: ObservedCallState, newState: ObservedCallState) {
        // When call was ACTIVE or DIALING and becomes DISCONNECTED or IDLE:
        if (callInProgress && (oldState == ObservedCallState.ACTIVE || oldState == ObservedCallState.DIALING)) {
            if (newState == ObservedCallState.DISCONNECTED || newState == ObservedCallState.IDLE) {
                // Current call has ended
                callInProgress = false
                val current = _currentEntry.value
                if (current != null) {
                    scope.launch {
                        repository.updateStatus(current.id, CallStatus.COMPLETED)
                        if (_callingMode.value == CallingMode.AUTO_CALL) {
                            val items = queueItems.value
                            val next = repository.getNextPending()
                            if (next == null) {
                                // Queue completed, no remaining numbers
                                _executionState.value = QueueExecutionState.COMPLETED
                                _countdownRemaining.value = null
                                _currentEntry.value = current.copy(status = CallStatus.COMPLETED)
                                _events.emit(QueueEvent.QueueFinished(totalCalls = queueSummary.value.completed))
                            } else {
                                val nextIdx = items.indexOfFirst { it.id == next.id }
                                if (nextIdx != -1) {
                                    _manualCursorIndex.value = nextIdx
                                }
                                _currentEntry.value = next
                                if (_executionState.value != QueueExecutionState.STOPPED && _executionState.value != QueueExecutionState.PAUSED) {
                                    _executionState.value = QueueExecutionState.RUNNING
                                    startAutoCallCooldown()
                                }
                            }
                        } else {
                            // MANUAL_NEXT mode:
                            // Mark called number as DONE, advance CURRENT to the next appropriate queue item,
                            // update CURRENT TARGET accordingly, keep primary action ready.
                            val items = queueItems.value
                            val currentIdx = _manualCursorIndex.value ?: items.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
                            val nextIdx = currentIdx + 1
                            if (nextIdx < items.size) {
                                _manualCursorIndex.value = nextIdx
                                _currentEntry.value = items[nextIdx]
                                _executionState.value = QueueExecutionState.PAUSED
                            } else {
                                // Reached the end of the queue
                                _manualCursorIndex.value = currentIdx
                                _currentEntry.value = items[currentIdx].copy(status = CallStatus.COMPLETED)
                                val hasPending = items.any { it.id != current.id && it.status == CallStatus.PENDING }
                                if (!hasPending) {
                                    _executionState.value = QueueExecutionState.COMPLETED
                                    _events.emit(QueueEvent.QueueFinished(totalCalls = queueSummary.value.completed + 1))
                                } else {
                                    _executionState.value = QueueExecutionState.PAUSED
                                }
                            }
                            _countdownRemaining.value = null
                        }
                    }
                }
            }
        } else if (newState == ObservedCallState.ACTIVE || newState == ObservedCallState.DIALING) {
            callInProgress = true
            activeCallStartTime = System.currentTimeMillis()
        }
    }

    @Volatile
    private var isDialingNext = false

    fun startQueue(preferDirectCall: Boolean = true) {
        if (_callingMode.value == CallingMode.MANUAL_NEXT) {
            beginManualCall(preferDirectCall)
            return
        }
        if (callInProgress || isDialingNext) return
        val items = queueItems.value
        val hasPending = items.any { it.status == CallStatus.PENDING }
        if (!hasPending && _currentEntry.value == null) {
            _executionState.value = QueueExecutionState.COMPLETED
            return
        }
        val savedCooldown = pausedCooldownSeconds
        pausedCooldownSeconds = null
        _executionState.value = QueueExecutionState.RUNNING

        // If paused during cooldown countdown, safely resume cooldown
        if (savedCooldown != null && savedCooldown > 0) {
            startAutoCallCooldown(initialSeconds = savedCooldown)
            return
        }

        isDialingNext = true
        autoAdvanceJob?.cancel()
        _countdownRemaining.value = null

        scope.launch {
            try {
                val currentIdx = _manualCursorIndex.value
                val current = _currentEntry.value
                    ?: (if (currentIdx != null && currentIdx in items.indices) items[currentIdx] else null)
                    ?: repository.getNextPending()
                    ?: items.firstOrNull()
                if (current != null) {
                    dialEntry(current, preferDirectCall)
                } else {
                    _executionState.value = QueueExecutionState.COMPLETED
                    _currentEntry.value = items.lastOrNull()?.copy(status = CallStatus.COMPLETED)
                    _events.emit(QueueEvent.QueueFinished(totalCalls = queueSummary.value.completed))
                }
            } finally {
                isDialingNext = false
            }
        }
    }

    fun beginManualCall(preferDirectCall: Boolean = true) {
        if (callInProgress || isDialingNext) return
        val items = queueItems.value
        if (items.isEmpty()) return

        // Resolve target: the currently selected/displayed target has highest priority,
        // regardless of its status (PENDING, CALLED, etc.)
        val currentIdx = _manualCursorIndex.value
        val current = _currentEntry.value
            ?: (if (currentIdx != null && currentIdx in items.indices) items[currentIdx] else null)
            ?: items.firstOrNull { it.status == CallStatus.PENDING }
            ?: items.firstOrNull()

        if (current == null) {
            // Queue is exhausted: no remaining callable numbers
            _executionState.value = QueueExecutionState.COMPLETED
            return
        }

        val target = current
        val targetIdx = items.indexOfFirst { it.id == target.id }
        if (targetIdx != -1) {
            _manualCursorIndex.value = targetIdx
            _currentEntry.value = target
        }

        isDialingNext = true
        autoAdvanceJob?.cancel()
        _countdownRemaining.value = null
        _executionState.value = QueueExecutionState.RUNNING

        scope.launch {
            try {
                dialEntry(target, preferDirectCall)
            } finally {
                isDialingNext = false
            }
        }
    }

    fun navigateReturn() {
        if (callInProgress) return
        val items = queueItems.value
        if (items.isEmpty()) return
        val currentIdx = _manualCursorIndex.value ?: 0
        if (currentIdx > 0) {
            val newIdx = currentIdx - 1
            val prevItem = items[newIdx]
            _manualCursorIndex.value = newIdx

            if (_executionState.value == QueueExecutionState.COMPLETED) {
                _executionState.value = QueueExecutionState.PAUSED
            }

            if (prevItem.status == CallStatus.SKIPPED) {
                // Restore SKIPPED to PENDING in repository
                scope.launch {
                    repository.resetStatus(prevItem.id)
                }
                _currentEntry.value = prevItem.copy(status = CallStatus.PENDING)
            } else {
                // DONE remains DONE, PENDING remains PENDING
                _currentEntry.value = prevItem
            }

            if (_callingMode.value == CallingMode.AUTO_CALL && _executionState.value == QueueExecutionState.RUNNING) {
                startAutoCallCooldown()
            }
        }
    }

    fun navigateBack() {
        navigateReturn()
    }

    fun callNext(preferDirectCall: Boolean = true) {
        if (_executionState.value == QueueExecutionState.STOPPED) {
            return
        }
        if (callInProgress || isDialingNext) {
            return
        }
        val items = queueItems.value
        if (items.isEmpty()) return
        if (_callingMode.value == CallingMode.MANUAL_NEXT) {
            beginManualCall(preferDirectCall)
            return
        }

        val currentIdx = _manualCursorIndex.value
        val target = _currentEntry.value
            ?: (if (currentIdx != null && currentIdx in items.indices) items[currentIdx] else null)
            ?: items.firstOrNull { it.status == CallStatus.PENDING }
            ?: items.firstOrNull()

        if (target == null) {
            _executionState.value = QueueExecutionState.COMPLETED
            return
        }

        val targetIdx = items.indexOfFirst { it.id == target.id }
        if (targetIdx != -1) {
            _manualCursorIndex.value = targetIdx
            _currentEntry.value = target
        }

        isDialingNext = true
        autoAdvanceJob?.cancel()
        _countdownRemaining.value = null
        _executionState.value = QueueExecutionState.RUNNING

        scope.launch {
            try {
                dialEntry(target, preferDirectCall)
            } finally {
                isDialingNext = false
            }
        }
    }

    fun pauseQueue() {
        pausedCooldownSeconds = _countdownRemaining.value
        autoAdvanceJob?.cancel()
        _countdownRemaining.value = null
        _executionState.value = QueueExecutionState.PAUSED
    }

    fun stopQueue() {
        autoAdvanceJob?.cancel()
        _countdownRemaining.value = null
        _executionState.value = QueueExecutionState.STOPPED
        val current = _currentEntry.value
        if (current != null && current.status == CallStatus.CALLING) {
            scope.launch {
                repository.resetStatus(current.id)
            }
        }
        if (_callingMode.value == CallingMode.MANUAL_NEXT) {
            syncManualCursorWithItems(queueItems.value)
        } else {
            _currentEntry.value = null
        }
        callInProgress = false
        isDialingNext = false
    }

    fun skipCurrent(preferDirectCall: Boolean = true) {
        if (callInProgress) return
        val items = queueItems.value
        if (items.isEmpty()) return
        val currentIdx = _manualCursorIndex.value ?: 0
        val target = items.getOrNull(currentIdx) ?: return

        scope.launch {
            if (target.status != CallStatus.COMPLETED) {
                repository.updateStatus(target.id, CallStatus.SKIPPED)
            }
            val nextIdx = currentIdx + 1
            if (nextIdx < items.size) {
                _manualCursorIndex.value = nextIdx
                _currentEntry.value = items[nextIdx]
                if (_callingMode.value == CallingMode.AUTO_CALL) {
                    if (_executionState.value == QueueExecutionState.RUNNING) {
                        startAutoCallCooldown()
                    }
                } else {
                    _executionState.value = QueueExecutionState.PAUSED
                }
            } else {
                // Reached the end of the queue
                _manualCursorIndex.value = currentIdx
                val finalStatus = if (target.status == CallStatus.COMPLETED) CallStatus.COMPLETED else CallStatus.SKIPPED
                _currentEntry.value = target.copy(status = finalStatus)
                val hasPending = items.any { it.id != target.id && it.status == CallStatus.PENDING }
                if (!hasPending) {
                    _executionState.value = QueueExecutionState.COMPLETED
                    autoAdvanceJob?.cancel()
                    _countdownRemaining.value = null
                    _events.emit(QueueEvent.QueueFinished(totalCalls = queueSummary.value.completed))
                } else {
                    _executionState.value = QueueExecutionState.PAUSED
                }
            }
        }
    }

    fun advanceNext(success: Boolean = true, preferDirectCall: Boolean = true) {
        if (_callingMode.value == CallingMode.MANUAL_NEXT) {
            autoAdvanceJob?.cancel()
            _countdownRemaining.value = null
            val items = queueItems.value
            if (items.isEmpty()) return
            val currentIdx = _manualCursorIndex.value ?: 0
            val current = items.getOrNull(currentIdx)
            scope.launch {
                if (current != null) {
                    val newStatus = if (success) CallStatus.COMPLETED else CallStatus.FAILED
                    repository.updateStatus(current.id, newStatus)
                }
                val nextIdx = currentIdx + 1
                if (nextIdx < items.size) {
                    _manualCursorIndex.value = nextIdx
                    _currentEntry.value = items[nextIdx]
                    _executionState.value = QueueExecutionState.PAUSED
                } else {
                    _manualCursorIndex.value = currentIdx
                    _executionState.value = QueueExecutionState.COMPLETED
                    _events.emit(QueueEvent.QueueFinished(totalCalls = queueSummary.value.completed))
                }
            }
            return
        }

        autoAdvanceJob?.cancel()
        _countdownRemaining.value = null
        val current = _currentEntry.value
        scope.launch {
            if (current != null) {
                val newStatus = if (success) CallStatus.COMPLETED else CallStatus.FAILED
                repository.updateStatus(current.id, newStatus)
            }
            val next = repository.getNextPending()
            if (next != null) {
                if (_executionState.value == QueueExecutionState.RUNNING) {
                    dialEntry(next, preferDirectCall)
                } else {
                    _currentEntry.value = next
                }
            } else {
                _executionState.value = QueueExecutionState.COMPLETED
                _currentEntry.value = null
                _events.emit(QueueEvent.QueueFinished(totalCalls = queueSummary.value.completed))
            }
        }
    }

    fun setCurrentTarget(entry: PhoneNumberEntry) {
        val items = queueItems.value
        val idx = items.indexOfFirst { it.id == entry.id }
        if (idx != -1) {
            val target = items[idx]
            _manualCursorIndex.value = idx
            _currentEntry.value = target
            if (_executionState.value == QueueExecutionState.COMPLETED) {
                _executionState.value = QueueExecutionState.PAUSED
            }
            autoAdvanceJob?.cancel()
            _countdownRemaining.value = null
            if (target.status != CallStatus.PENDING) {
                // When selecting a non-pending number (e.g. Called), do not auto-call; wait for manual user action
                if (_executionState.value == QueueExecutionState.RUNNING) {
                    _executionState.value = QueueExecutionState.PAUSED
                }
            } else if (_callingMode.value == CallingMode.AUTO_CALL && _executionState.value == QueueExecutionState.RUNNING && !callInProgress) {
                startAutoCallCooldown()
            }
        }
    }

    fun callSpecificEntry(entry: PhoneNumberEntry, preferDirectCall: Boolean = true) {
        autoAdvanceJob?.cancel()
        _countdownRemaining.value = null
        if (_callingMode.value == CallingMode.MANUAL_NEXT) {
            val idx = queueItems.value.indexOfFirst { it.id == entry.id }
            if (idx != -1) {
                _manualCursorIndex.value = idx
            }
        }
        _executionState.value = QueueExecutionState.RUNNING
        scope.launch {
            dialEntry(entry, preferDirectCall)
        }
    }

    private suspend fun dialEntry(entry: PhoneNumberEntry, preferDirectCall: Boolean) {
        _currentEntry.value = entry
        val idx = queueItems.value.indexOfFirst { it.id == entry.id }
        if (idx != -1) {
            _manualCursorIndex.value = idx
        }
        repository.updateStatus(entry.id, CallStatus.CALLING)

        val dialTarget = PhoneNumberNormalizer.getDialTarget(entry, _numberFormatPreference.value)
        when (val result = callController.initiateCall(dialTarget, preferDirectCall)) {
            is CallInitiationResult.Success -> {
                callInProgress = true
                activeCallStartTime = System.currentTimeMillis()
                _events.emit(QueueEvent.CallPlaced(dialTarget, result.mode))
            }
            is CallInitiationResult.PermissionRequired -> {
                callInProgress = false
                _events.emit(QueueEvent.PermissionNeeded(result.permission))
            }
            is CallInitiationResult.Error -> {
                callInProgress = false
                repository.updateStatus(entry.id, CallStatus.FAILED)
                _events.emit(QueueEvent.Error(result.message))
            }
        }
    }

    private fun startAutoCallCooldown(initialSeconds: Int? = null) {
        autoAdvanceJob?.cancel()
        val delaySeconds = initialSeconds ?: _autoAdvanceDelaySeconds.value
        if (delaySeconds <= 0) {
            _countdownRemaining.value = null
            triggerAutoCallNext()
            return
        }
        autoAdvanceJob = scope.launch {
            for (i in delaySeconds downTo 1) {
                if (_executionState.value != QueueExecutionState.RUNNING || _callingMode.value != CallingMode.AUTO_CALL) {
                    _countdownRemaining.value = null
                    return@launch
                }
                _countdownRemaining.value = i
                _events.emit(QueueEvent.AutoAdvanceCountdown(i))
                delay(1000)
            }
            _countdownRemaining.value = null

            if (_executionState.value == QueueExecutionState.RUNNING && _callingMode.value == CallingMode.AUTO_CALL) {
                triggerAutoCallNext()
            }
        }
    }

    private fun triggerAutoCallNext(preferDirectCall: Boolean = true) {
        if (callInProgress || isDialingNext) return
        val items = queueItems.value
        if (items.isEmpty()) return
        val hasPending = items.any { it.status == CallStatus.PENDING }
        if (!hasPending && _currentEntry.value == null) {
            _executionState.value = QueueExecutionState.COMPLETED
            return
        }
        isDialingNext = true
        scope.launch {
            try {
                val currentIdx = _manualCursorIndex.value
                val current = _currentEntry.value
                    ?: (if (currentIdx != null && currentIdx in items.indices) items[currentIdx] else null)
                    ?: repository.getNextPending()
                    ?: items.firstOrNull()
                if (current != null) {
                    dialEntry(current, preferDirectCall)
                } else {
                    _executionState.value = QueueExecutionState.COMPLETED
                    _currentEntry.value = items.lastOrNull()?.copy(status = CallStatus.COMPLETED)
                    _events.emit(QueueEvent.QueueFinished(totalCalls = queueSummary.value.completed))
                }
            } finally {
                isDialingNext = false
            }
        }
    }

    fun setAutoAdvanceDelay(seconds: Int) {
        _autoAdvanceDelaySeconds.value = seconds.coerceIn(1, 30)
    }

    fun clearQueue() {
        autoAdvanceJob?.cancel()
        _countdownRemaining.value = null
        _executionState.value = QueueExecutionState.IDLE
        _manualCursorIndex.value = null
        _currentEntry.value = null
        callInProgress = false
        scope.launch {
            repository.clearQueue()
        }
    }

    fun deleteEntry(id: Long) {
        scope.launch {
            val items = queueItems.value
            val currentIdx = _manualCursorIndex.value
            if (_currentEntry.value?.id == id) {
                val remainingItems = items.filter { it.id != id }
                if (remainingItems.isEmpty()) {
                    _manualCursorIndex.value = null
                    _currentEntry.value = null
                } else {
                    val newIdx = (currentIdx ?: 0).coerceAtMost(remainingItems.size - 1)
                    _manualCursorIndex.value = newIdx
                    _currentEntry.value = remainingItems[newIdx]
                }
            } else if (currentIdx != null) {
                val deletedIdx = items.indexOfFirst { it.id == id }
                if (deletedIdx != -1 && deletedIdx < currentIdx) {
                    _manualCursorIndex.value = (currentIdx - 1).coerceAtLeast(0)
                }
            }
            repository.deleteEntry(id)
        }
    }

    fun retryEntry(id: Long) {
        scope.launch {
            repository.resetStatus(id)
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val items = queueItems.value.toMutableList()
        if (fromIndex !in items.indices || toIndex !in items.indices) return

        val movedItem = items.removeAt(fromIndex)
        // Rule: When a Skipped item is manually repositioned using drag-and-drop, change its status to Pending
        val itemToInsert = if (movedItem.status == CallStatus.SKIPPED) {
            movedItem.copy(status = CallStatus.PENDING, callTimestamp = null)
        } else {
            movedItem
        }
        items.add(toIndex, itemToInsert)

        // Assign updated sequential orderIndex to all items based on new positions
        val reordered = items.mapIndexed { index, entry ->
            entry.copy(orderIndex = index)
        }

        // Update currentEntry if the moved item was the current target
        val current = _currentEntry.value
        if (current != null && current.id == movedItem.id) {
            _currentEntry.value = itemToInsert
            _manualCursorIndex.value = toIndex
        } else if (current != null) {
            val newIdx = reordered.indexOfFirst { it.id == current.id }
            if (newIdx != -1) {
                _manualCursorIndex.value = newIdx
            }
        }

        scope.launch {
            repository.updateAll(reordered)
        }
    }
}
