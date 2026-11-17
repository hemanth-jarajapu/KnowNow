package com.passion.knownow.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.DownloadState
import com.passion.knownow.data.db.DownloadTaskEntity
import com.passion.knownow.data.db.KnowNowDatabase
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.data.network.HttpFetcher
import com.passion.knownow.data.work.DownloadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Owns the download queue. Tasks are persisted in Room; a small number of downloads are
 * handed to WorkManager at a time so a large batch does not flood the network or the device.
 */
class DownloadRepository(
    private val context: Context,
    private val db: KnowNowDatabase,
    private val workManager: WorkManager,
    private val fetcher: HttpFetcher,
) {
    val tasks: Flow<List<DownloadTaskEntity>> = db.downloadTaskDao().observeAll()
    val activeTasks: Flow<List<DownloadTaskEntity>> = db.downloadTaskDao().observeActive()

    private val mutex = Mutex()
    private val inFlight = mutableSetOf<Long>()

    suspend fun enqueue(publications: List<PublicationEntity>, refresh: Boolean = false) {
        // The queue is mutated from the UI and from the worker's completion callback at the
        // same time; without the lock two taps could both insert and trip the unique index
        // on publicationId, which used to crash the app mid-download.
        mutex.withLock {
            publications.forEach { pub ->
                val existing = db.downloadTaskDao().getForPublication(pub.id)
                if (existing == null) {
                    db.downloadTaskDao().insert(
                        DownloadTaskEntity(
                            publicationId = pub.id,
                            releaseName = pub.releaseName,
                            slug = pub.slug,
                            title = pub.title,
                            forceRefresh = refresh,
                        )
                    )
                } else {
                    when (existing.state) {
                        DownloadState.QUEUED, DownloadState.RUNNING -> Unit
                        DownloadState.PAUSED -> db.downloadTaskDao().setState(existing.id, DownloadState.QUEUED)
                        DownloadState.COMPLETED -> if (refresh) db.downloadTaskDao().resetForRefresh(existing.id)
                        else -> db.downloadTaskDao().resetForQueue(existing.id)
                    }
                }
            }
        }
        pump()
    }

    suspend fun refresh(pub: PublicationEntity) {
        val existing = db.downloadTaskDao().getForPublication(pub.id)
        if (existing == null) {
            enqueue(listOf(pub), refresh = true)
        } else {
            db.downloadTaskDao().resetForRefresh(existing.id)
            mutex.withLock {
                inFlight += existing.id
                enqueueTask(existing.copy(state = DownloadState.QUEUED), ExistingWorkPolicy.REPLACE)
            }
        }
    }

    suspend fun pause(id: Long) {
        val task = db.downloadTaskDao().getById(id) ?: return
        if (task.state == DownloadState.COMPLETED) return
        db.downloadTaskDao().setState(id, DownloadState.PAUSED)
        mutex.withLock { inFlight.remove(id) }
        workManager.cancelUniqueWork(tag(id))
        pump()
    }

    suspend fun resume(id: Long) {
        db.downloadTaskDao().getById(id) ?: return
        db.downloadTaskDao().setState(id, DownloadState.QUEUED)
        pump()
    }

    suspend fun cancel(id: Long) {
        val task = db.downloadTaskDao().getById(id)
        db.downloadTaskDao().setState(id, DownloadState.CANCELLED)
        mutex.withLock { inFlight.remove(id) }
        workManager.cancelUniqueWork(tag(id))
        task?.let { deletePartFiles(File(context.filesDir, "docs/${it.releaseName}/${it.slug}")) }
        db.downloadTaskDao().delete(id)
        pump()
    }

    suspend fun pauseAll() {
        db.downloadTaskDao().getActive()
            .filter { it.state == DownloadState.QUEUED || it.state == DownloadState.RUNNING }
            .forEach {
                db.downloadTaskDao().setState(it.id, DownloadState.PAUSED)
                workManager.cancelUniqueWork(tag(it.id))
            }
        mutex.withLock { inFlight.clear() }
    }

    suspend fun resumeAll() {
        db.downloadTaskDao().getActive()
            .filter { it.state == DownloadState.PAUSED || it.state == DownloadState.FAILED }
            .forEach { db.downloadTaskDao().setState(it.id, DownloadState.QUEUED) }
        pump()
    }

    suspend fun cancelAll() {
        db.downloadTaskDao().getActive().forEach { cancelTask(it) }
        mutex.withLock { inFlight.clear() }
    }

    suspend fun clearFinished() {
        db.downloadTaskDao().clearFinished()
    }

    /** Called by the worker whenever a task stops running, successfully or not. */
    suspend fun onTaskSettled(taskId: Long) {
        mutex.withLock { inFlight.remove(taskId) }
        pump()
    }

    private suspend fun pump() {
        mutex.withLock {
            val capacity = MAX_CONCURRENT - inFlight.size
            if (capacity <= 0) return
            db.downloadTaskDao().getQueued()
                .filter { it.id !in inFlight }
                .take(capacity)
                .forEach { task ->
                    inFlight += task.id
                    enqueueTask(task, ExistingWorkPolicy.REPLACE)
                }
        }
    }

    private suspend fun cancelTask(task: DownloadTaskEntity) {
        db.downloadTaskDao().setState(task.id, DownloadState.CANCELLED)
        workManager.cancelUniqueWork(tag(task.id))
        deletePartFiles(File(context.filesDir, "docs/${task.releaseName}/${task.slug}"))
        db.downloadTaskDao().delete(task.id)
    }

    private suspend fun enqueueTask(task: DownloadTaskEntity, policy: ExistingWorkPolicy) {
        val allowMetered = ServiceLocator.allowMeteredDownloads()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (allowMetered) NetworkType.CONNECTED else NetworkType.UNMETERED)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_TASK_ID to task.id,
                    DownloadWorker.KEY_OWNER to OWNER,
                    DownloadWorker.KEY_REPO to REPO,
                    DownloadWorker.KEY_RELEASE to task.releaseName,
                    DownloadWorker.KEY_PUBLICATION_ID to task.publicationId,
                    DownloadWorker.KEY_SLUG to task.slug,
                )
            )
            .setConstraints(constraints)
            .addTag(tag(task.id))
            .build()
        workManager.enqueueUniqueWork(tag(task.id), policy, request)
    }

    private fun deletePartFiles(dir: File) {
        if (!dir.exists()) return
        dir.walkTopDown().filter { it.isFile && it.name.endsWith(".part") }.forEach { it.delete() }
    }

    companion object {
        const val OWNER = "ServiceNow"
        const val REPO = "ServiceNowDocs"
        private const val MAX_CONCURRENT = 3
        fun tag(id: Long) = "download_task_$id"
    }
}
