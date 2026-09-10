package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.models.CallStatus
import com.example.models.PhoneNumberEntry

@Entity(tableName = "calling_queue")
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalInput: String,
    val normalizedNumber: String,
    val formattedDisplay: String,
    val status: String,
    val isValid: Boolean,
    val validationMessage: String?,
    val orderIndex: Int,
    val callTimestamp: Long?,
    val notes: String?
) {
    fun toModel(): PhoneNumberEntry {
        val parsedStatus = try {
            CallStatus.valueOf(status)
        } catch (_: Exception) {
            CallStatus.PENDING
        }
        return PhoneNumberEntry(
            id = id,
            originalInput = originalInput,
            normalizedNumber = normalizedNumber,
            formattedDisplay = formattedDisplay,
            status = parsedStatus,
            isValid = isValid,
            validationMessage = validationMessage,
            orderIndex = orderIndex,
            callTimestamp = callTimestamp,
            notes = notes
        )
    }

    companion object {
        fun fromModel(model: PhoneNumberEntry): QueueItemEntity {
            return QueueItemEntity(
                id = model.id,
                originalInput = model.originalInput,
                normalizedNumber = model.normalizedNumber,
                formattedDisplay = model.formattedDisplay,
                status = model.status.name,
                isValid = model.isValid,
                validationMessage = model.validationMessage,
                orderIndex = model.orderIndex,
                callTimestamp = model.callTimestamp,
                notes = model.notes
            )
        }
    }
}
