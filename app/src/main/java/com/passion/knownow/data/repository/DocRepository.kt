package com.passion.knownow.data.repository

import com.passion.knownow.data.db.KnowNowDatabase
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.data.db.ReleaseEntity
import com.passion.knownow.data.network.GitHubApi
import com.passion.knownow.data.network.RawApi
import com.passion.knownow.data.network.parseBranches
import com.passion.knownow.data.network.parseDefaultBranch
import kotlinx.coroutines.flow.firstOrNull

class DocRepository(
    private val api: GitHubApi,
    private val rawApi: RawApi,
    private val db: KnowNowDatabase,
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
}
