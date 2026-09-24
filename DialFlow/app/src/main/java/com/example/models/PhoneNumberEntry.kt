package com.example.models

data class PhoneNumberEntry(
    val id: Long = 0,
    val originalInput: String,
    val normalizedNumber: String,
    val formattedDisplay: String,
    val status: CallStatus = CallStatus.PENDING,
    val isValid: Boolean = true,
    val validationMessage: String? = null,
    val orderIndex: Int = 0,
    val callTimestamp: Long? = null,
    val notes: String? = null
)

data class QueueSummary(
    val total: Int = 0,
    val completed: Int = 0,
    val remaining: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val currentNumber: PhoneNumberEntry? = null,
    val executionState: QueueExecutionState = QueueExecutionState.IDLE
)
