package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.data.db.RecentDocRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class LibraryFilter { ALL, IN_PROGRESS, DOWNLOADED }

data class LibraryUiState(
    val recent: List<RecentDocRow> = emptyList(),
    val topics: List<PublicationEntity> = emptyList(),
    val documentCount: Int = 0,
    val topicCount: Int = 0,
    val noteCount: Int = 0,
    val bookmarkCount: Int = 0,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val isLoading: Boolean = true,
)

class LibraryViewModel : ViewModel() {
    private val dao = ServiceLocator.database.libraryDao()
    private val _filter = MutableStateFlow(LibraryFilter.ALL)
    val filter: StateFlow<LibraryFilter> = _filter.asStateFlow()

    private data class Counts(val docs: Int, val topics: Int, val bookmarks: Int, val notes: Int)

    private val counts = combine(
        dao.observeDocumentCount(),
        dao.observePublicationCount(),
        dao.observeBookmarkIds(),
        dao.observeNoteDocumentIds(),
    ) { docs, topics, bookmarks, notes ->
        Counts(docs, topics, bookmarks.size, notes.size)
    }

    val state: StateFlow<LibraryUiState> = combine(
        dao.observeRecent(),
        ServiceLocator.database.publicationDao().observeDownloaded(),
        counts,
        _filter,
    ) { recentRows, topicRows, totals, filter ->
        val filteredRecent = when (filter) {
            LibraryFilter.ALL -> recentRows
            LibraryFilter.IN_PROGRESS -> recentRows.filter { it.progressPercent in 0.02f..0.98f }
            LibraryFilter.DOWNLOADED -> recentRows
        }
        LibraryUiState(
            recent = filteredRecent,
            topics = if (filter == LibraryFilter.IN_PROGRESS) emptyList() else topicRows,
            documentCount = totals.docs,
            topicCount = totals.topics,
            noteCount = totals.notes,
            bookmarkCount = totals.bookmarks,
            filter = filter,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setFilter(filter: LibraryFilter) {
        _filter.value = filter
    }
}
