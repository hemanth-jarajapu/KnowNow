package com.passion.knownow.data.repository

import com.passion.knownow.data.db.KnowNowDatabase
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.data.db.ReleaseEntity
import com.passion.knownow.data.network.GitHubApi
import com.passion.knownow.data.network.RawApi
import com.passion.knownow.data.network.parseBranches
import kotlinx.coroutines.flow.firstOrNull

class DocRepository(
    private val api: GitHubApi,
    private val rawApi: RawApi,
    private val db: KnowNowDatabase,
) {
    suspend fun syncReleases(owner: String, repo: String) {
        val branches = parseBranches(api.branchesRaw(owner, repo))
        val entities = branches.map { b ->
            ReleaseEntity(
                name = b.name,
                displayName = b.name.replaceFirstChar { it.uppercase() },
                commitSha = b.sha,
                isLatest = b.name == "brazil",
                syncedAt = System.currentTimeMillis(),
            )
        }
        db.releaseDao().upsertAll(entities)
    }

    suspend fun deletePublication(pub: PublicationEntity) {
        // Mark notes as orphaned (keep them), then remove docs + publication + files.
        val docs = db.documentDao().observeForPublication(pub.id).firstOrNull() ?: emptyList()
        docs.forEach { db.noteDao().markOrphaned(it.id) }
        docs.mapNotNull { it.localPath }.forEach { path ->
            runCatching { java.io.File(path).parentFile?.deleteRecursively() }
        }
        db.documentDao().deleteForPublication(pub.id)
        db.publicationDao().delete(pub.id)
    }

    suspend fun fetchPublications(owner: String, repo: String, release: String): List<PublicationEntity> {
        val text = rawApi.getText("$owner/$repo/$release/llms.txt")
        val regex = Regex("""- \[(.+?)]\((https://raw\.githubusercontent\.com/[^)]+/markdown/([^/]+)/index\.md)\)""")
        val parsed = linkedMapOf<String, String>() // slug -> title
        for (m in regex.findAll(text)) {
            val title = m.groupValues[1]
            val slug = m.groupValues[3]
            parsed[slug] = title
        }
        val dao = db.publicationDao()
        val toInsert = parsed.map { (slug, title) ->
            PublicationEntity(releaseName = release, slug = slug, title = title)
        }
        dao.insertAll(toInsert)
        parsed.forEach { (slug, title) -> dao.updateTitle(release, slug, title) }
        return dao.getForRelease(release)
    }
}