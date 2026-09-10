package com.example.data

import com.example.models.CallStatus
import com.example.models.PhoneNumberEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class QueueRepository(private val queueDao: QueueDao) {

    val queueItems: Flow<List<PhoneNumberEntry>> = queueDao.getAllItems().map { list ->
        list.map { it.toModel() }
    }

    suspend fun getAllItemsSync(): List<PhoneNumberEntry> = withContext(Dispatchers.IO) {
        queueDao.getAllItemsSync().map { it.toModel() }
    }

    suspend fun getNextPending(): PhoneNumberEntry? = withContext(Dispatchers.IO) {
        queueDao.getNextPendingItem()?.toModel()
    }

    suspend fun insertEntries(entries: List<PhoneNumberEntry>) = withContext(Dispatchers.IO) {
        val entities = entries.map { QueueItemEntity.fromModel(it) }
        queueDao.insertAll(entities)
    }

    suspend fun updateStatus(id: Long, status: CallStatus, timestamp: Long? = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            queueDao.updateStatus(id, status.name, timestamp)
        }

    suspend fun resetStatus(id: Long) = withContext(Dispatchers.IO) {
        queueDao.resetStatus(id)
    }

    suspend fun deleteEntry(id: Long) = withContext(Dispatchers.IO) {
        queueDao.deleteById(id)
    }

    suspend fun clearQueue() = withContext(Dispatchers.IO) {
        queueDao.clearAll()
    }
}
