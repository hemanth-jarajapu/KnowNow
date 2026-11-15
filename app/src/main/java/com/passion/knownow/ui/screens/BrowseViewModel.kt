package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.DocumentEntity
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.data.db.ReleaseEntity
import com.passion.knownow.data.work.DownloadWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class BrowseUiState(
    val releases: List<ReleaseEntity> = emptyList(),
    val selectedRelease: String? = null,
    val publications: List<PublicationEntity> = emptyList(),
    val expandedPublicationId: Long? = null,
    val documents: List<DocumentEntity> = emptyList(),
    val isLoadingReleases: Boolean = false,
    val isLoadingPublications: Boolean = false,
    val downloadingPublicationId: Long? = null,
    val downloadProgress: Float = 0f,
    val error: String? = null,
)

class BrowseViewModel(
    private val owner: String = "ServiceNow",
    private val repo: String = "ServiceNowDocs",
) : ViewModel() {

    private val repository = ServiceLocator.docRepository
    private val db = ServiceLocator.database
    private val workManager = WorkManager.getInstance(ServiceLocator.context)
    private val _state = MutableStateFlow(BrowseUiState(isLoadingReleases = true))
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    private var pubsJob: Job? = null
    private var docsJob: Job? = null
    private val workJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            try {
                repository.syncReleases(owner, repo)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Failed to load releases")
            } finally {
                _state.value = _state.value.copy(isLoadingReleases = false)
            }
        }
        viewModelScope.launch {
            db.releaseDao().observeAll().collect { releases ->
                val selected = _state.value.selectedRelease?.takeIf { n -> releases.any { it.name == n } }
                    ?: releases.firstOrNull()?.name
                val prev = _state.value.selectedRelease
                _state.value = _state.value.copy(releases = releases, selectedRelease = selected, isLoadingReleases = false)
                if (selected != null && selected != prev) {
                    selectRelease(selected)
                } else if (selected != null && _state.value.publications.isEmpty()) {
                    selectRelease(selected)
                }
            }
        }
    }

    fun selectRelease(name: String) {
        _state.value = _state.value.copy(
            selectedRelease = name,
            isLoadingPublications = true,
            error = null,
            expandedPublicationId = null,
            documents = emptyList(),
        )
        pubsJob?.cancel()
        pubsJob = viewModelScope.launch {
            db.publicationDao().observeForRelease(name).collectLatest { pubs ->
                _state.value = _state.value.copy(publications = pubs)
            }
        }
        viewModelScope.launch {
            try {
                repository.fetchPublications(owner, repo, name)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Failed to load publications")
            } finally {
                _state.value = _state.value.copy(isLoadingPublications = false)
            }
        }
    }

    fun togglePublication(pub: PublicationEntity) {
        if (_state.value.expandedPublicationId == pub.id) {
            docsJob?.cancel()
            _state.value = _state.value.copy(expandedPublicationId = null, documents = emptyList())
            return
        }
        docsJob?.cancel()
        _state.value = _state.value.copy(expandedPublicationId = pub.id)
        docsJob = viewModelScope.launch {
            db.documentDao().observeForPublication(pub.id).collectLatest { docs ->
                _state.value = _state.value.copy(documents = docs)
            }
        }
    }

    fun downloadPublication(pub: PublicationEntity) {
        val tag = "download_${pub.id}"
        viewModelScope.launch {
            try {
                val allowMetered = ServiceLocator.allowMeteredDownloads()
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(if (allowMetered) NetworkType.CONNECTED else NetworkType.UNMETERED)
                    .build()
                val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setInputData(
                        workDataOf(
                            DownloadWorker.KEY_OWNER to owner,
                            DownloadWorker.KEY_REPO to repo,
                            DownloadWorker.KEY_RELEASE to pub.releaseName,
                            DownloadWorker.KEY_PUBLICATION_ID to pub.id,
                            DownloadWorker.KEY_SLUG to pub.slug,
                        )
                    )
                    .setConstraints(constraints)
                    .addTag(tag)
                    .build()
                workManager.enqueueUniqueWork(tag, ExistingWorkPolicy.KEEP, request)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Failed to start download")
            }
        }

        if (workJobs[tag]?.isActive == true) return
        workJobs[tag] = viewModelScope.launch {
            workManager.getWorkInfosByTagFlow(tag).collectLatest { infos ->
                val info = infos.firstOrNull() ?: return@collectLatest
                when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                        _state.value = _state.value.copy(downloadingPublicationId = pub.id, downloadProgress = 0f)
                    }
                    WorkInfo.State.RUNNING -> {
                        val progress = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                        val total = info.progress.getInt(DownloadWorker.KEY_TOTAL, 1).coerceAtLeast(1)
                        _state.value = _state.value.copy(
                            downloadingPublicationId = pub.id,
                            downloadProgress = progress.toFloat() / total,
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        _state.value = _state.value.copy(downloadingPublicationId = null, downloadProgress = 1f)
                        togglePublication(pub)
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        _state.value = _state.value.copy(downloadingPublicationId = null, error = "Download failed")
                    }
                    else -> Unit
                }
            }
        }
    }
}