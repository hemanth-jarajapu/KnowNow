package com.passion.knownow.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE state IN ('QUEUED','RUNNING','PAUSED','FAILED') ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE state IN ('QUEUED','RUNNING','PAUSED','FAILED') ORDER BY createdAt ASC")
    suspend fun getActive(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE state = 'QUEUED' ORDER BY createdAt ASC")
    suspend fun getQueued(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getById(id: Long): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE publicationId = :publicationId LIMIT 1")
    suspend fun getForPublication(publicationId: Long): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(task: DownloadTaskEntity): Long

    @Query("UPDATE download_tasks SET state = :state, updatedAt = :at WHERE id = :id")
    suspend fun setState(id: Long, state: String, at: Long = System.currentTimeMillis())

    @Query("UPDATE download_tasks SET state = 'QUEUED', error = NULL, attempts = 0, updatedAt = :at WHERE id = :id")
    suspend fun resetForQueue(id: Long, at: Long = System.currentTimeMillis())

    @Query("UPDATE download_tasks SET state = 'QUEUED', completed = 0, total = 0, bytes = 0, error = NULL, attempts = 0, forceRefresh = 1, updatedAt = :at WHERE id = :id")
    suspend fun resetForRefresh(id: Long, at: Long = System.currentTimeMillis())

    @Query("UPDATE download_tasks SET forceRefresh = 0 WHERE id = :id")
    suspend fun clearForceRefresh(id: Long)

    @Query("UPDATE download_tasks SET total = :total, completed = :completed, bytes = :bytes, updatedAt = :at WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        total: Int,
        completed: Int,
        bytes: Long,
        at: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE download_tasks SET state = 'FAILED', error = :error, attempts = attempts + 1, updatedAt = :at WHERE id = :id")
    suspend fun markFailed(
        id: Long,
        error: String?,
        at: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM download_tasks WHERE state IN ('COMPLETED','CANCELLED')")
    suspend fun clearFinished()

    @Query("DELETE FROM download_tasks")
    suspend fun clearAll()
}
