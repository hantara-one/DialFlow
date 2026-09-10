package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {

    @Query("SELECT * FROM calling_queue ORDER BY orderIndex ASC, id ASC")
    fun getAllItems(): Flow<List<QueueItemEntity>>

    @Query("SELECT * FROM calling_queue ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllItemsSync(): List<QueueItemEntity>

    @Query("SELECT * FROM calling_queue WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): QueueItemEntity?

    @Query("SELECT * FROM calling_queue WHERE status = 'PENDING' AND isValid = 1 ORDER BY orderIndex ASC, id ASC LIMIT 1")
    suspend fun getNextPendingItem(): QueueItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QueueItemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QueueItemEntity): Long

    @Update
    suspend fun update(item: QueueItemEntity)

    @Query("UPDATE calling_queue SET status = :status, callTimestamp = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, timestamp: Long?)

    @Query("UPDATE calling_queue SET status = 'PENDING', callTimestamp = NULL WHERE id = :id")
    suspend fun resetStatus(id: Long)

    @Query("DELETE FROM calling_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calling_queue")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM calling_queue")
    fun getCount(): Flow<Int>
}
