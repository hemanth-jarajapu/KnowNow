package com.passion.knownow.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "releases")
data class ReleaseEntity(
    @PrimaryKey val name: String,
    val displayName: String,
    val commitSha: String? = null,
    val isLatest: Boolean = false,
    val syncedAt: Long = 0L,
)

@Entity(tableName = "publications")
data class PublicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val releaseName: String,
    val slug: String,
    val title: String,
    val docCount: Int = 0,
    val totalBytes: Long = 0L,
    val downloadedAt: Long? = null,
    val status: String = "NOT_DOWNLOADED",
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val documentId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val scrollOffset: Int = 0,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Soft reference: the document may be deleted later. We keep the note and
    // fall back to the snapshot fields below so it stays useful.
    val documentId: Long,
    val publicationId: Long,
    val releaseName: String = "",
    val documentTitleSnapshot: String = "",
    val publicationTitleSnapshot: String = "",
    val canonicalUrl: String? = null,
    val content: String,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isOrphaned: Boolean = false,
)

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val text: String,
    val color: Int = 0xFFFFEB3B.toInt(),
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val documentId: Long,
    val progressPercent: Float = 0f,
    val lastReadAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val publicationId: Long,
    val title: String,
    val description: String? = null,
    val relativePath: String,
    val localPath: String? = null,
    val canonicalUrl: String? = null,
    val lastUpdated: String? = null,
    val readingTimeMinutes: Int? = null,
    val topicType: String? = null,
    val sortOrder: Int = 0,
    val parentPath: String? = null,
    val contentHash: String? = null,
)