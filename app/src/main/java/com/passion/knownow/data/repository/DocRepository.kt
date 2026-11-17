package com.passion.knownow.data.repository

import com.passion.knownow.core.parsing.TopicIndexParser
import com.passion.knownow.core.parsing.TopicPreview
import com.passion.knownow.data.db.KnowNowDatabase
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.data.db.ReleaseEntity
import com.passion.knownow.data.network.GitHubApi
import com.passion.knownow.data.network.RawApi
import com.passion.knownow.data.network.parseBranches
import com.passion.knownow.data.network.parseDefaultBranch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

class DocRepository(
    private val api: GitHubApi,
    private val rawApi: RawApi,
    private val db: KnowNowDatabase,
    private val previewDir: File,
) {
    suspend fun syncReleases(owner: String, repo: String) {
        val defaultBranch = runCatching {
            parseDefaultBranch(api.repoRaw(owner, repo))
        }.getOrNull()

        val branches = mutableListOf<com.passion.knownow.data.network.BranchDto>()
        var page = 1
        while (true) {
            val batch = parseBranches(api.branchesRaw(owner, repo, perPage = 100, page = page))
            branches += batch
            if (batch.size < 100) break
            page++
        }

        val entities = branches.map { b ->
            ReleaseEntity(
                name = b.name,
                displayName = b.name.replaceFirstChar { it.uppercase() },
                commitSha = b.sha,
                isLatest = b.name == defaultBranch,
                syncedAt = System.currentTimeMillis(),
            )
        }
        db.releaseDao().upsertAll(entities)
    }

    suspend fun deletePublication(pub: PublicationEntity) {
        val docs = db.documentDao().observeForPublication(pub.id).firstOrNull() ?: emptyList()
        docs.forEach { db.noteDao().markOrphaned(it.id) }
        docs.mapNotNull { it.localPath }.forEach { path ->
            runCatching { java.io.File(path).parentFile?.deleteRecursively() }
        }
        db.downloadTaskDao().getForPublication(pub.id)?.let { db.downloadTaskDao().delete(it.id) }
        db.documentDao().deleteForPublication(pub.id)
        db.publicationDao().delete(pub.id)
    }

    suspend fun fetchPublications(owner: String, repo: String, release: String): List<PublicationEntity> {
        val text = rawApi.getText("$owner/$repo/$release/llms.txt")
        val regex = Regex("""- \[(.+?)]\((https://raw\.githubusercontent\.com/[^)]+/markdown/([^/]+)/index\.md)\)""")
        val parsed = linkedMapOf<String, String>()
        for (m in regex.findAll(text)) {
            parsed[m.groupValues[3]] = m.groupValues[1]
        }

        val dao = db.publicationDao()
        dao.insertAll(parsed.map { (slug, title) ->
            PublicationEntity(releaseName = release, slug = slug, title = title)
        })
        parsed.forEach { (slug, title) -> dao.updateTitle(release, slug, title) }

        // Prune publications that no longer exist in the source index.
        val existing = dao.getForRelease(release)
        existing.filter { it.slug !in parsed.keys }.forEach { deletePublication(it) }

        return dao.getForRelease(release)
    }

    /**
     * Reads a publication's table of contents without downloading it.
     *
     * `markdown/<slug>/index.md` is a nested list of every page with a one-line description,
     * a few hundred kilobytes at most. Fetching it once and keeping a copy in the cache means
     * Discover can describe a topic and list its contents instantly after the first look,
     * instead of the user having to commit to a multi-megabyte download to find out what is in
     * it.
     */
    suspend fun previewIndex(
        owner: String,
        repo: String,
        release: String,
        slug: String,
        maxAgeMs: Long = PREVIEW_TTL_MS,
    ): TopicPreview? = withContext(Dispatchers.IO) {
        val cacheFile = File(previewDir, "${release}__$slug.md")
        val cached = runCatching { if (cacheFile.exists()) cacheFile.readText() else null }.getOrNull()
        val fresh = cached != null && System.currentTimeMillis() - cacheFile.lastModified() < maxAgeMs
        if (cached != null && fresh) return@withContext TopicIndexParser.parse(slug, cached)

        val text = runCatching { rawApi.getText("$owner/$repo/$release/markdown/$slug/index.md") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (text == null) {
            return@withContext cached?.let { TopicIndexParser.parse(slug, it) }
        }
        runCatching {
            previewDir.mkdirs()
            cacheFile.writeText(text)
        }
        TopicIndexParser.parse(slug, text)
    }

    private companion object {
        /** A publication's outline changes at most with a release, so a day is plenty. */
        const val PREVIEW_TTL_MS = 24 * 60 * 60 * 1000L
    }
}
