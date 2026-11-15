package com.passion.knownow.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.passion.knownow.core.parsing.FrontmatterParser
import com.passion.knownow.core.parsing.TocParser
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

class DownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val owner = inputData.getString(KEY_OWNER) ?: return Result.failure()
        val repo = inputData.getString(KEY_REPO) ?: return Result.failure()
        val release = inputData.getString(KEY_RELEASE) ?: return Result.failure()
        val publicationId = inputData.getLong(KEY_PUBLICATION_ID, 0L)
        val slug = inputData.getString(KEY_SLUG) ?: return Result.failure()

        val db = ServiceLocator.database
        val rawApi = ServiceLocator.rawApi
        val filesDir = applicationContext.filesDir

        return try {
            val indexText = rawApi.getText("$owner/$repo/$release/markdown/$slug/index.md")
            val entries = TocParser.parse(indexText)
            val urls = entries.map { it.url }.distinct()
            val total = urls.size.coerceAtLeast(1)
            val done = AtomicInteger(0)
            val semaphore = Semaphore(8)

            val docs = coroutineScope {
                urls.mapIndexed { index, url ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val relative = url.substringAfter("/markdown/$slug/")
                            val target = File(filesDir, "docs/$release/$slug/$relative")
                            target.parentFile?.mkdirs()
                            if (!target.exists() || target.length() == 0L) {
                                val text = rawApi.getText(url.removePrefix("https://raw.githubusercontent.com/"))
                                target.writeText(text)
                            }
                            val content = target.readText()
                            val (fm, body) = FrontmatterParser.parse(content)
                            val hash = sha256(body)
                            val existing = db.documentDao().getByPath(publicationId, relative)
                            val doc = DocumentEntity(
                                id = existing?.id ?: 0,
                                publicationId = publicationId,
                                title = fm["title"] ?: relative.substringBeforeLast('.'),
                                description = fm["description"],
                                relativePath = relative,
                                localPath = target.absolutePath,
                                canonicalUrl = fm["canonical_url"],
                                lastUpdated = fm["last_updated"],
                                readingTimeMinutes = fm["reading_time_minutes"]?.toIntOrNull(),
                                topicType = fm["topic_type"],
                                sortOrder = index,
                                contentHash = hash,
                            )
                            val completed = done.incrementAndGet()
                            setProgress(workDataOf(KEY_PROGRESS to completed, KEY_TOTAL to total))
                            doc
                        }
                    }
                }.awaitAll()
            }

            db.documentDao().upsertAll(docs)
            db.publicationDao().markReady(publicationId, System.currentTimeMillis(), docs.size)
            Result.success(workDataOf(KEY_PROGRESS to docs.size, KEY_TOTAL to total))
        } catch (e: Exception) {
            return Result.failure(workDataOf("error" to (e.message ?: "Download failed")))
        }
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val KEY_OWNER = "owner"
        const val KEY_REPO = "repo"
        const val KEY_RELEASE = "release"
        const val KEY_PUBLICATION_ID = "publicationId"
        const val KEY_SLUG = "slug"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
    }
}