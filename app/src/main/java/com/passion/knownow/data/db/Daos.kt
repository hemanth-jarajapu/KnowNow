package com.passion.knownow.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseDao {
    @Query("SELECT * FROM releases ORDER BY isLatest DESC, name ASC")
    fun observeAll(): Flow<List<ReleaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ReleaseEntity>)
}

@Dao
interface PublicationDao {
    @Query("SELECT * FROM publications WHERE releaseName = :release ORDER BY title ASC")
    fun observeForRelease(release: String): Flow<List<PublicationEntity>>

    @Query("SELECT * FROM publications WHERE id = :id")
    suspend fun getById(id: Long): PublicationEntity?

    @Query("UPDATE publications SET status = :status, downloadedAt = :at, docCount = :count WHERE id = :id")
    suspend fun markReady(id: Long, at: Long, count: Int, status: String = "READY")

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PublicationEntity>)

    @Query("DELETE FROM publications WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE publicationId = :publicationId ORDER BY sortOrder ASC")
    fun observeForPublication(publicationId: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :q || '%' ORDER BY title ASC LIMIT 50")
    fun search(q: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY title ASC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DocumentEntity>)

    @Query("DELETE FROM documents WHERE publicationId = :publicationId")
    suspend fun deleteForPublication(publicationId: Long)
}