package com.passion.knownow.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.passion.knownow.core.parsing.FrontmatterParser
import com.passion.knownow.core.parsing.TocParser
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.DocumentEntity
import com.passion.knownow.data.db.DownloadState
import com.passion.knownow.data.db.DownloadTaskDao
import com.passion.knownow.data.db.DownloadTaskEntity
import com.passion.knownow.data.network.HttpStatusException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

class DownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private val db get() = ServiceLocator.database
    private val taskDao: DownloadTaskDao get() = db.downloadTaskDao()
    private val fetcher get() = ServiceLocator.fetcher

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, 0L)
        try {
            if (taskId <= 0L) return Result.failure(workDataOf(KEY_ERROR to "Missing task id"))

            val task = taskDao.getById(taskId) ?: return Result.success()
            if (task.state == DownloadState.PAUSED || task.state == DownloadState.CANCELLED) {
                return Result.success()
            }
            taskDao.setState(taskId, DownloadState.RUNNING)

            return try {
                download(task)
            } catch (e: CancellationException) {
                // Cancelled by pause()/cancel(): the persisted state already reflects intent.
                throw e
            } catch (e: PauseSignal) {
                Result.success(workDataOf(KEY_PROGRESS to e.completed, KEY_TOTAL to e.total))
            } catch (e: Exception) {
                val message = e.message ?: "Download failed"
                taskDao.markFailed(taskId, message)
                val attempts = (taskDao.getById(taskId)?.attempts ?: 1)
                if (attempts < MAX_ATTEMPTS) {
                    taskDao.setState(taskId, DownloadState.QUEUED)
                    Result.retry()
                } else {
                    Result.failure(workDataOf(KEY_ERROR to message))
                }
            }
        } finally {
            runCatching { ServiceLocator.downloadRepository.onTaskSettled(taskId) }
        }
    }

    private suspend fun download(task: DownloadTaskEntity): Result {
        val indexPath = "$RAW_BASE/${OWNER}/${REPO}/${task.releaseName}/markdown/${task.slug}/index.md"
        val indexText = fetcher.get(indexPath, forceRefresh = task.forceRefresh)
        // TOC links can carry anchors or query strings and can repeat; normalise before
        // downloading so two entries never fight over one file (which used to skew counts).
        val entries = TocParser.parse(indexText)
            .map { it.copy(url = it.url.substringBefore('#').substringBefore('?')) }
            .filter { it.url.contains("/markdown/${task.slug}/") }
            .distinctBy { it.url }
        if (entries.isEmpty()) {
            throw IllegalStateException("Empty table of contents for ${task.slug}")
        }

        val total = entries.size
        taskDao.updateProgress(task.id, total, task.completed.coerceAtMost(total), task.bytes)

        val baseDir = File(applicationContext.filesDir, "docs/${task.releaseName}/${task.slug}")
        val semaphore = Semaphore(PARALLELISM)
        val completed = AtomicInteger(0)
        val bytes = AtomicLong(0L)
        val skipped = AtomicInteger(0)

        val docs = coroutineScope {
            entries.mapIndexed { index, entry ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        coroutineContext.ensureActive()
                        checkState(task.id, completed.get(), total)

                        val relative = entry.url.substringAfter("/markdown/${task.slug}/")
                        val target = File(baseDir, relative)
                        val exists = target.exists() && target.length() > 0L
                        val fetchedBytes = if (!exists || task.forceRefresh) {
                            fetchDocument(entry.url, target, task.forceRefresh)
                        } else {
                            0L
                        }
                        if (fetchedBytes > 0L) bytes.addAndGet(fetchedBytes)

                        val entity = if (fetchedBytes < 0L) {
                            // The catalogue lists pages that are published in the TOC but
                            // missing from the release branch. Drop stale copies and move on
                            // instead of failing the whole topic.
                            skipped.incrementAndGet()
                            if (target.exists()) target.delete()
                            null
                        } else {
                            val content = target.readText()
                            val (fm, body) = FrontmatterParser.parse(content)
                            val hash = sha256(body)
                            val existing = db.documentDao().getByPath(task.publicationId, relative)

                            DocumentEntity(
                                id = existing?.id ?: 0L,
                                publicationId = task.publicationId,
                                title = fm["title"] ?: entry.title.ifBlank { relative.substringBeforeLast('.') },
                                description = fm["description"] ?: entry.description,
                                relativePath = relative,
                                localPath = target.absolutePath,
                                canonicalUrl = fm["canonical_url"],
                                lastUpdated = fm["last_updated"],
                                readingTimeMinutes = fm["reading_time_minutes"]?.toIntOrNull(),
                                topicType = fm["topic_type"],
                                sortOrder = index,
                                contentHash = hash,
                            )
                        }

                        val done = completed.incrementAndGet()
                        taskDao.updateProgress(task.id, total, done, bytes.get())
                        setProgress(
                            workDataOf(
                                KEY_PROGRESS to done,
                                KEY_TOTAL to total,
                                KEY_SKIPPED to skipped.get(),
                            )
                        )
                        entity
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (docs.isEmpty()) {
            throw IllegalStateException("No documents could be downloaded for ${task.slug}")
        }

        val uniqueDocs = docs.distinctBy { it.relativePath }
        db.documentDao().upsertAll(uniqueDocs)
        db.publicationDao().markReady(task.publicationId, System.currentTimeMillis(), uniqueDocs.size)
        taskDao.clearForceRefresh(task.id)
        taskDao.setState(task.id, DownloadState.COMPLETED)
        return Result.success(workDataOf(KEY_PROGRESS to uniqueDocs.size, KEY_TOTAL to total))
    }

    /**
     * Downloads one page. Returns bytes written, or -1 when the page is gone (404/410) or
     * keeps failing after [DOC_ATTEMPTS]. Page-level problems must never abort the topic.
     */
    private suspend fun fetchDocument(url: String, target: File, forceRefresh: Boolean): Long {
        var attempt = 0
        while (true) {
            try {
                return fetcher.downloadToFile(url, target, forceRefresh)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpStatusException) {
                // 404/410: page retired upstream. Any other 4xx: unreadable, skip.
                if (e.code in 400..499) return -1L
                if (++attempt >= DOC_ATTEMPTS) return -1L
            } catch (e: IOException) {
                if (++attempt >= DOC_ATTEMPTS) return -1L
            }
            delay(400L * attempt)
        }
    }

    private suspend fun checkState(taskId: Long, completed: Int, total: Int) {
        val state = taskDao.getById(taskId)?.state
        if (state == DownloadState.PAUSED || state == DownloadState.CANCELLED) {
            throw PauseSignal(completed, total)
        }
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private class PauseSignal(val completed: Int, val total: Int) : Exception("Paused")

    companion object {
        const val KEY_TASK_ID = "taskId"
        const val KEY_OWNER = "owner"
        const val KEY_REPO = "repo"
        const val KEY_RELEASE = "release"
        const val KEY_PUBLICATION_ID = "publicationId"
        const val KEY_SLUG = "slug"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val KEY_SKIPPED = "skipped"
        const val KEY_ERROR = "error"

        private const val OWNER = "ServiceNow"
        private const val REPO = "ServiceNowDocs"
        private const val RAW_BASE = "https://raw.githubusercontent.com"
        private const val PARALLELISM = 8
        private const val DOC_ATTEMPTS = 3
        private const val MAX_ATTEMPTS = 3
    }
}
