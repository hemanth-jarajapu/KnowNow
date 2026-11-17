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
    /** Null when the page came from a topic that is no longer in the catalogue. */
    val topicTitle: String?,
)

data class BookmarkRow(
    val documentId: Long,
    val createdAt: Long,
    val title: String,
    val topicTitle: String,
    val progressPercent: Float,
)

data class OfflineDocRow(
    val id: Long,
    val title: String,
    val topicTitle: String,
    val topicId: Long,
    val readingTimeMinutes: Int?,
    val progressPercent: Float?,
    val lastReadAt: Long?,
)

data class SearchRow(
    val id: Long,
    val title: String,
    val topicTitle: String,
    val readingTimeMinutes: Int?,
    val description: String?,
)

@Dao
interface LibraryDao {
    @Query(
        """
        SELECT d.id AS id, d.title AS title, d.localPath AS localPath, d.publicationId AS publicationId,
               p.progressPercent AS progressPercent, p.lastReadAt AS lastReadAt, d.canonicalUrl AS canonicalUrl,
               pub.title AS topicTitle
        FROM reading_progress p
        INNER JOIN documents d ON d.id = p.documentId
        LEFT JOIN publications pub ON pub.id = d.publicationId
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

    /** Bookmarks joined with their document so the list can show real titles. */
    @Query(
        """
        SELECT b.documentId AS documentId, b.createdAt AS createdAt, d.title AS title,
               pub.title AS topicTitle, COALESCE(r.progressPercent, 0.0) AS progressPercent
        FROM bookmarks b
        INNER JOIN documents d ON d.id = b.documentId
        INNER JOIN publications pub ON pub.id = d.publicationId
        LEFT JOIN reading_progress r ON r.documentId = b.documentId
        ORDER BY b.createdAt DESC
        """
    )
    fun observeBookmarkRows(): Flow<List<BookmarkRow>>

    /** Every document that belongs to a topic that finished downloading. */
    @Query(
        """
        SELECT d.id AS id, d.title AS title, pub.title AS topicTitle, pub.id AS topicId,
               d.readingTimeMinutes AS readingTimeMinutes,
               r.progressPercent AS progressPercent, r.lastReadAt AS lastReadAt
        FROM documents d
        INNER JOIN publications pub ON pub.id = d.publicationId
        LEFT JOIN reading_progress r ON r.documentId = d.id
        WHERE pub.status = 'READY'
        ORDER BY pub.title ASC, d.sortOrder ASC
        """
    )
    fun observeOfflineDocs(): Flow<List<OfflineDocRow>>

    @Query("SELECT COUNT(*) FROM documents d INNER JOIN publications pub ON pub.id = d.publicationId WHERE pub.status = 'READY'")
    fun observeOfflineDocCount(): Flow<Int>

    /** Title/description search that carries the owning topic for context in results. */
    @Query(
        """
        SELECT d.id AS id, d.title AS title, pub.title AS topicTitle,
               d.readingTimeMinutes AS readingTimeMinutes, d.description AS description
        FROM documents d
        INNER JOIN publications pub ON pub.id = d.publicationId
        WHERE d.title LIKE '%' || :q || '%'
           OR IFNULL(d.description, '') LIKE '%' || :q || '%'
        ORDER BY CASE WHEN d.title LIKE :q || '%' THEN 0 ELSE 1 END, d.title ASC
        LIMIT 100
        """
    )
    fun searchDocs(q: String): Flow<List<SearchRow>>
}
