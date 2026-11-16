package com.passion.knownow.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class RecentDocRow(
    val id: Long,
    val title: String,
    val localPath: String?,
    val publicationId: Long,
    val progressPercent: Float,
    val lastReadAt: Long,
    val canonicalUrl: String?,
)

@Dao
interface LibraryDao {
    @Query(
        """
        SELECT d.id AS id, d.title AS title, d.localPath AS localPath, d.publicationId AS publicationId,
               p.progressPercent AS progressPercent, p.lastReadAt AS lastReadAt, d.canonicalUrl AS canonicalUrl
        FROM reading_progress p
        INNER JOIN documents d ON d.id = p.documentId
        ORDER BY p.lastReadAt DESC
        LIMIT 12
        """
    )
    fun observeRecent(): Flow<List<RecentDocRow>>

    @Query("SELECT COUNT(*) FROM documents")
    fun observeDocumentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM publications WHERE status = 'READY'")
    fun observePublicationCount(): Flow<Int>

    @Query("SELECT documentId FROM bookmarks")
    fun observeBookmarkIds(): Flow<List<Long>>

    @Query("SELECT DISTINCT documentId FROM notes WHERE isOrphaned = 0")
    fun observeNoteDocumentIds(): Flow<List<Long>>
}
