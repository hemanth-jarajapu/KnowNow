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

    @Query("SELECT * FROM publications WHERE status = 'READY' ORDER BY downloadedAt DESC")
    fun observeDownloaded(): Flow<List<PublicationEntity>>

    @Query("SELECT * FROM publications ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<PublicationEntity>>

    @Query("SELECT * FROM publications WHERE releaseName = :release ORDER BY title ASC")
    suspend fun getForRelease(release: String): List<PublicationEntity>

    @Query("SELECT * FROM publications WHERE id = :id")
    suspend fun getById(id: Long): PublicationEntity?

    @Query("SELECT * FROM publications WHERE releaseName = :releaseName AND slug = :slug LIMIT 1")
    suspend fun getByReleaseAndSlug(releaseName: String, slug: String): PublicationEntity?

    /** Cross-topic link support: a link names a topicslug, not a release. */
    @Query("SELECT * FROM publications WHERE slug = :slug ORDER BY downloadedAt DESC LIMIT 1")
    suspend fun getBySlug(slug: String): PublicationEntity?

    @Query("UPDATE publications SET status = :status, downloadedAt = :at, docCount = :count WHERE id = :id")
    suspend fun markReady(id: Long, at: Long, count: Int, status: String = "READY")

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<PublicationEntity>): List<Long>

    @Query("UPDATE publications SET title = :title WHERE releaseName = :releaseName AND slug = :slug")
    suspend fun updateTitle(releaseName: String, slug: String, title: String)

    @Query("DELETE FROM publications WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE publicationId = :publicationId ORDER BY sortOrder ASC")
    fun observeForPublication(publicationId: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE publicationId = :publicationId AND relativePath = :relativePath LIMIT 1")
    suspend fun getByPath(publicationId: Long, relativePath: String): DocumentEntity?

    @Query(
        """
        SELECT * FROM documents
        WHERE title LIKE '%' || :q || '%'
           OR IFNULL(description, '') LIKE '%' || :q || '%'
           OR relativePath LIKE '%' || :q || '%'
        ORDER BY CASE WHEN title LIKE :q || '%' THEN 0 ELSE 1 END, title ASC
        LIMIT 100
        """
    )
    fun search(q: String): Flow<List<DocumentEntity>>

    /** Cross-topic link support: resolve a topic slug + page path to a local document. */
    @Query(
        """
        SELECT d.* FROM documents d
        INNER JOIN publications p ON p.id = d.publicationId
        WHERE p.slug = :slug AND d.relativePath = :relativePath
        ORDER BY d.id ASC LIMIT 1
        """
    )
    suspend fun findBySlugAndPath(slug: String, relativePath: String): DocumentEntity?

    /** Fallback used when a link points at a page we know by file name only. */
    @Query("SELECT * FROM documents WHERE relativePath LIKE '%' || :suffix ORDER BY id ASC LIMIT 1")
    suspend fun findByPathSuffix(suffix: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE publicationId = :publicationId")
    suspend fun getAllForPublication(publicationId: Long): List<DocumentEntity>

    @Query("SELECT * FROM documents ORDER BY title ASC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DocumentEntity>)

    @Query("DELETE FROM documents WHERE publicationId = :publicationId")
    suspend fun deleteForPublication(publicationId: Long)
}