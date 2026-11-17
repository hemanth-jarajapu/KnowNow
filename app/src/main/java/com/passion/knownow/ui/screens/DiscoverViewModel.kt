package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.core.parsing.TopicPreview
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.DocumentEntity
import com.passion.knownow.data.db.DownloadTaskEntity
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.data.db.ReleaseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val releases: List<ReleaseEntity> = emptyList(),
    val selectedRelease: String? = null,
    val publications: List<PublicationEntity> = emptyList(),
    val tasks: Map<Long, DownloadTaskEntity> = emptyMap(),
    val selection: Set<Long> = emptySet(),
    val syncing: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val detailPublication: PublicationEntity? = null,
    val detailDocuments: List<DocumentEntity> = emptyList(),
    val preview: TopicPreview? = null,
    val previewLoading: Boolean = false,
    val previewError: String? = null,
    /** One-line "what is this about" hints, filled as topics are previewed. */
    val hints: Map<String, TopicHint> = emptyMap(),
) {
    val selectionMode: Boolean get() = selection.isNotEmpty()
    val visiblePublications: List<PublicationEntity>
        get() = if (query.isBlank()) publications
        else publications.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.slug.contains(query, ignoreCase = true) ||
                hints[it.slug]?.summary?.contains(query, ignoreCase = true) == true
        }
}

/** What Discover knows about a topic it hasn't downloaded: a tagline and a page count. */
data class TopicHint(val summary: String?, val pageCount: Int)

private data class DiscoverInput(
    val selectedRelease: String? = null,
    val selection: Set<Long> = emptySet(),
    val syncing: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val detailId: Long? = null,
    val preview: TopicPreview? = null,
    val previewLoading: Boolean = false,
    val previewError: String? = null,
    val hints: Map<String, TopicHint> = emptyMap(),
)

class DiscoverViewModel : ViewModel() {
    private val db = ServiceLocator.database
    private val repository = ServiceLocator.docRepository
    private val downloads = ServiceLocator.downloadRepository

    private val _input = MutableStateFlow(DiscoverInput())
    private var fetchJob: Job? = null
    private var previewJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val publications = _input
        .map { it.selectedRelease }
        .distinctUntilChanged()
        .flatMapLatest { release ->
            release?.let { db.publicationDao().observeForRelease(it) } ?: flowOf(emptyList())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val detailDocs = _input
        .map { it.detailId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            id?.let { db.documentDao().observeForPublication(it) } ?: flowOf(emptyList())
        }

    val state: StateFlow<DiscoverUiState> = combine(
        db.releaseDao().observeAll(),
        publications,
        downloads.tasks,
        detailDocs,
        _input,
    ) { releases, pubs, tasks, docs, input ->
        val detail = pubs.firstOrNull { it.id == input.detailId }
        DiscoverUiState(
            releases = releases,
            selectedRelease = input.selectedRelease,
            publications = pubs,
            tasks = tasks.associateBy { it.publicationId },
            selection = input.selection,
            syncing = input.syncing,
            loading = input.loading,
            error = input.error,
            query = input.query,
            detailPublication = detail,
            detailDocuments = docs,
            preview = input.preview,
            previewLoading = input.previewLoading,
            previewError = input.previewError,
            hints = input.hints,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState(syncing = true))

    init {
        viewModelScope.launch {
            db.releaseDao().observeAll().collect { releases ->
                val current = _input.value.selectedRelease
                if (current == null || releases.none { it.name == current }) {
                    val preferred = releases.firstOrNull { it.isLatest } ?: releases.firstOrNull()
                    if (preferred != null) selectRelease(preferred.name)
                }
            }
        }
        viewModelScope.launch {
            val cached = db.releaseDao().observeAll().firstOrNull().orEmpty()
            val newest = cached.maxOfOrNull { it.syncedAt } ?: 0L
            val stale = cached.isEmpty() || System.currentTimeMillis() - newest > RELEASE_TTL_MS
            if (stale) syncReleases()
        }
        // Fill in the first few cards so the list explains itself. Only a handful, one at a
        // time: the outlines are a few hundred kilobytes each and are cached on disk, so this
        // is a one-time cost per release rather than something that runs on every launch.
        viewModelScope.launch {
            publications.collect { pubs ->
                val known = _input.value.hints
                pubs.filter { it.slug !in known }.take(PREFETCH_LIMIT).forEach { pub ->
                    val preview = runCatching {
                        repository.previewIndex(OWNER, REPO, pub.releaseName, pub.slug)
                    }.getOrNull()
                    val hint = TopicHint(preview?.summary, preview?.pageCount ?: 0)
                    _input.update { it.copy(hints = it.hints + (pub.slug to hint)) }
                }
            }
        }
    }

    fun syncReleases() {
        viewModelScope.launch {
            _input.update { it.copy(syncing = true, error = null) }
            try {
                repository.syncReleases(OWNER, REPO)
            } catch (e: Exception) {
                _input.update { it.copy(error = e.message ?: "Couldn't reach GitHub") }
            } finally {
                _input.update { it.copy(syncing = false) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun selectRelease(name: String) {
        if (_input.value.selectedRelease == name && state.value.publications.isNotEmpty()) return
        _input.update {
            it.copy(selectedRelease = name, loading = true, error = null, selection = emptySet(), detailId = null)
        }
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            try {
                repository.fetchPublications(OWNER, REPO, name)
            } catch (e: Exception) {
                _input.update { it.copy(error = e.message ?: "Couldn't load topics") }
            } finally {
                _input.update { it.copy(loading = false) }
            }
        }
    }

    fun setQuery(query: String) {
        _input.update { it.copy(query = query) }
    }

    fun toggleSelection(id: Long) {
        _input.update { current ->
            val next = if (id in current.selection) current.selection - id else current.selection + id
            current.copy(selection = next)
        }
    }

    fun startSelection(id: Long) {
        _input.update { it.copy(selection = it.selection + id) }
    }

    fun clearSelection() {
        _input.update { it.copy(selection = emptySet()) }
    }

    /** Opens the preview sheet and starts reading that topic's table of contents. */
    fun openDetail(pub: PublicationEntity) {
        _input.update {
            it.copy(
                detailId = pub.id,
                preview = null,
                previewLoading = true,
                previewError = null,
            )
        }
        loadPreview(pub)
    }

    fun retryPreview() {
        val pub = state.value.detailPublication ?: return
        _input.update { it.copy(previewLoading = true, previewError = null) }
        loadPreview(pub)
    }

    private fun loadPreview(pub: PublicationEntity) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val preview = runCatching {
                repository.previewIndex(OWNER, REPO, pub.releaseName, pub.slug)
            }.getOrNull()
            _input.update {
                it.copy(
                    preview = preview,
                    previewLoading = false,
                    previewError = if (preview == null || preview.isEmpty) {
                        "Couldn't load the contents of this topic"
                    } else {
                        null
                    },
                    hints = if (preview != null && !preview.isEmpty) {
                        it.hints + (pub.slug to TopicHint(preview.summary, preview.pageCount))
                    } else {
                        it.hints
                    },
                )
            }
        }
    }

    fun closeDetail() {
        previewJob?.cancel()
        _input.update {
            it.copy(detailId = null, preview = null, previewLoading = false, previewError = null)
        }
    }

    fun download(pub: PublicationEntity) {
        viewModelScope.launch {
            runCatching { downloads.enqueue(listOf(pub)) }
                .onFailure { e -> _input.update { it.copy(error = e.message ?: "Download failed") } }
        }
    }

    fun downloadSelected() {
        val selected = state.value.publications.filter { it.id in _input.value.selection }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            runCatching { downloads.enqueue(selected) }
                .onFailure { e -> _input.update { it.copy(error = e.message ?: "Download failed") } }
        }
        clearSelection()
    }

    fun refresh(pub: PublicationEntity) {
        viewModelScope.launch {
            runCatching { downloads.refresh(pub) }
                .onFailure { e -> _input.update { it.copy(error = e.message ?: "Refresh failed") } }
        }
    }

    fun cancel(pub: PublicationEntity) {
        val task = state.value.tasks[pub.id] ?: return
        viewModelScope.launch { downloads.cancel(task.id) }
    }

    fun pause(pub: PublicationEntity) {
        val task = state.value.tasks[pub.id] ?: return
        viewModelScope.launch { downloads.pause(task.id) }
    }

    fun resume(pub: PublicationEntity) {
        val task = state.value.tasks[pub.id] ?: return
        viewModelScope.launch { downloads.resume(task.id) }
    }

    companion object {
        private const val OWNER = "ServiceNow"
        private const val REPO = "ServiceNowDocs"
        private const val RELEASE_TTL_MS = 6 * 60 * 60 * 1000L
        private const val PREFETCH_LIMIT = 5
    }
}
