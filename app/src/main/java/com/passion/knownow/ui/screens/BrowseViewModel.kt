package com.passion.knownow.ui.screens

import android.content.Context
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
    val isLoadingReleases: Boolean = false,
    val isLoadingPublications: Boolean = false,
    val downloadingPublicationId: Long? = null,
    val downloadProgress: Float = 0f,
    val downloadedFirstDocumentId: Long? = null,
    val error: String? = null,
)

class BrowseViewModel(
    private val owner: String = "ServiceNow",
    private val repo: String = "ServiceNowDocs",
    private val context: Context = ServiceLocator.context,
) : ViewModel() {

    private val repository = ServiceLocator.docRepository
    private val workManager = WorkManager.getInstance(context)
    private val _state = MutableStateFlow(BrowseUiState(isLoadingReleases = true))
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.syncReleases(owner, repo)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoadingReleases = false)
            }
        }
        viewModelScope.launch {
            ServiceLocator.database.releaseDao().observeAll().collect { releases ->
                val selected = _state.value.selectedRelease ?: releases.firstOrNull()?.name
                _state.value = _state.value.copy(releases = releases, selectedRelease = selected, isLoadingReleases = false)
                if (_state.value.publications.isEmpty() && selected != null) {
                    loadPublications(selected)
                }
            }
        }
    }

    fun selectRelease(name: String) {
        _state.value = _state.value.copy(selectedRelease = name, publications = emptyList(), isLoadingPublications = true)
        viewModelScope.launch { loadPublications(name) }
    }

    private suspend fun loadPublications(release: String) {
        _state.value = _state.value.copy(isLoadingPublications = true, error = null)
        try {
            val pubs = repository.fetchPublications(owner, repo, release)
            _state.value = _state.value.copy(publications = pubs, isLoadingPublications = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message, isLoadingPublications = false)
        }
    }

    fun downloadPublication(pub: PublicationEntity) {
        val tag = "download_${pub.id}"
        viewModelScope.launch {
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
        }

        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow(tag).collectLatest { infos ->
                val info = infos.firstOrNull() ?: return@collectLatest
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                        val total = info.progress.getInt(DownloadWorker.KEY_TOTAL, 1).coerceAtLeast(1)
                        _state.value = _state.value.copy(
                            downloadingPublicationId = pub.id,
                            downloadProgress = progress.toFloat() / total,
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val list = ServiceLocator.database.documentDao().observeForPublication(pub.id).first()
                        val firstId = list.firstOrNull()?.id
                        _state.value = _state.value.copy(
                            downloadingPublicationId = null,
                            downloadProgress = 1f,
                            downloadedFirstDocumentId = firstId,
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        _state.value = _state.value.copy(downloadingPublicationId = null, error = "Download failed")
                    }
                    else -> Unit
                }
            }
        }
    }

    fun consumeDownloadedDocument() {
        _state.value = _state.value.copy(downloadedFirstDocumentId = null)
    }
}