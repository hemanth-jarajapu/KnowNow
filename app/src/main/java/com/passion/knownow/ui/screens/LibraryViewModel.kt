package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.data.db.RecentDocRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LibraryUiState(
    val recent: List<RecentDocRow> = emptyList(),
    val topics: List<PublicationEntity> = emptyList(),
    val documentCount: Int = 0,
    val topicCount: Int = 0,
    val noteCount: Int = 0,
    val bookmarkCount: Int = 0,
    val isLoading: Boolean = true,
)

class LibraryViewModel : ViewModel() {
    private val dao = ServiceLocator.database.libraryDao()

    private data class Counts(val docs: Int, val topics: Int, val bookmarks: Int, val notes: Int)

    private val counts = combine(
        dao.observeOfflineDocCount(),
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
    ) { recentRows, topicRows, totals ->
        LibraryUiState(
            recent = recentRows,
            topics = topicRows,
            documentCount = totals.docs,
            topicCount = totals.topics,
            noteCount = totals.notes,
            bookmarkCount = totals.bookmarks,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
}
