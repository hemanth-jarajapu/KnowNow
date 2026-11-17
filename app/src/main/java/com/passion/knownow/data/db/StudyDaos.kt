package com.passion.knownow.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE documentId = :id")
    suspend fun get(id: Long): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE documentId = :id")
    suspend fun delete(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE documentId = :id)")
    fun isBookmarked(id: Long): Flow<Boolean>
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isOrphaned = 0 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isOrphaned = 1 ORDER BY updatedAt DESC")
    fun observeOrphaned(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE documentId = :documentId ORDER BY updatedAt DESC")
    fun observeForDocument(documentId: Long): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE content LIKE '%' || :q || '%'
           OR tags LIKE '%' || :q || '%'
           OR documentTitleSnapshot LIKE '%' || :q || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun search(q: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: NoteEntity): Long

    @Delete
    suspend fun delete(item: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE notes SET isOrphaned = 1 WHERE documentId = :documentId")
    suspend fun markOrphaned(documentId: Long)

    @Query("UPDATE notes SET isOrphaned = 0, documentId = :newDocumentId WHERE id = :noteId")
    suspend fun reattach(noteId: Long, newDocumentId: Long)
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE documentId = :id")
    suspend fun get(id: Long): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReadingProgressEntity)
}