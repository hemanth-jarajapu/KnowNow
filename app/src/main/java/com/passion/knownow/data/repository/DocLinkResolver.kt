package com.passion.knownow.data.repository

import com.passion.knownow.core.parsing.DocNormalizer
import com.passion.knownow.data.db.DocumentEntity
import com.passion.knownow.data.db.KnowNowDatabase
import com.passion.knownow.data.db.PublicationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder

/** What a tapped link in a document turned out to be. */
sealed interface DocLinkTarget {
    /** The page is on this device; open it. */
    data class Local(val documentId: Long) : DocLinkTarget

    /**
     * The page belongs to a topic that is not downloaded. The reader offers to fetch that
     * topic instead of silently leaving the app.
     */
    data class NeedsTopic(
        val publication: PublicationEntity,
        val pagePath: String,
        val webUrl: String,
    ) : DocLinkTarget

    /** The topic is downloaded but this page isn't part of it (e.g. an older copy). */
    data class Unavailable(val topicTitle: String, val webUrl: String) : DocLinkTarget

    /** Anything else: a community post, a support case, a page from an unknown topic. */
    data class External(val url: String) : DocLinkTarget
}

/**
 * Resolves links found inside documentation.
 *
 * ServiceNow pages link to each other in three ways: `knn://doc/<slug>/<path>` (already
 * rewritten when the page was opened), raw repository URLs, and the published
 * `servicenow.com/docs` site URLs. All of them name the same markdown file, so all of them
 * can be answered from the local database — and when the topic isn't there yet, the reader
 * can say so and offer the download rather than dumping the user into a browser.
 */
class DocLinkResolver(private val db: KnowNowDatabase) {

    suspend fun resolve(
        uri: String,
        currentSlug: String?,
        currentPagePath: String?,
    ): DocLinkTarget = withContext(Dispatchers.IO) {
        val target = uri.trim()
        if (target.isEmpty()) return@withContext DocLinkTarget.External(uri)

        val reference = DocNormalizer.parseReference(target)
            ?: relativeReference(target, currentSlug, currentPagePath)
            ?: return@withContext DocLinkTarget.External(target)

        val slug = reference.first
        val path = decode(reference.second)

        val document = findDocument(slug, path)
        if (document != null) return@withContext DocLinkTarget.Local(document.id)

        val publication = runCatching { db.publicationDao().getBySlug(slug) }.getOrNull()
            ?: return@withContext DocLinkTarget.External(siteUrl(target, slug, path))

        if (publication.status != "READY") {
            DocLinkTarget.NeedsTopic(publication, path, siteUrl(target, slug, path))
        } else {
            DocLinkTarget.Unavailable(publication.title, siteUrl(target, slug, path))
        }
    }

    private fun relativeReference(
        target: String,
        currentSlug: String?,
        currentPagePath: String?,
    ): Pair<String, String>? {
        if (currentSlug == null) return null
        if (target.contains("://") || target.startsWith("mailto:") || target.startsWith("#")) return null
        val clean = target.substringBefore('#').substringBefore('?')
        if (clean.isBlank()) return null
        val resolved = DocNormalizer.resolveRelative(currentPagePath, clean) ?: return null
        return currentSlug to resolved
    }

    private suspend fun findDocument(slug: String, path: String): DocumentEntity? {
        val dao = db.documentDao()
        val fileName = path.substringAfterLast('/')
        // The stored path is relative to the publication, so try the topic-scoped lookup
        // before falling back to file names, which can repeat across topics.
        val publication = runCatching { db.publicationDao().getBySlug(slug) }.getOrNull()
        if (publication != null) {
            listOf(path, "$path.md", fileName, "$fileName.md").forEach { candidate ->
                runCatching { dao.getByPath(publication.id, candidate) }.getOrNull()?.let { return it }
            }
        }
        runCatching { dao.findBySlugAndPath(slug, path) }.getOrNull()?.let { return it }
        runCatching { dao.findBySlugAndPath(slug, "$path.md") }.getOrNull()?.let { return it }
        return runCatching { dao.findByPathSuffix(fileName) }.getOrNull()
    }

    /** The published web page for a document, used when it can't be served offline. */
    private fun siteUrl(original: String, slug: String, path: String): String {
        if (original.startsWith("http://") || original.startsWith("https://")) {
            if (DocNormalizer.webDocReference(original) != null) return original
        }
        val page = path.removeSuffix(".md").removeSuffix(".html")
        return "https://www.servicenow.com/docs/r/$slug/$page.html"
    }

    private fun decode(path: String): String =
        runCatching { URLDecoder.decode(path, "UTF-8") }.getOrDefault(path)
}
