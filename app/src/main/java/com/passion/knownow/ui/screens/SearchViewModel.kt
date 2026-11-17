package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.RecentDocRow
import com.passion.knownow.data.db.SearchRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val results: List<SearchRow> = emptyList(),
    val recent: List<RecentDocRow> = emptyList(),
    val searching: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel : ViewModel() {
    private val dao = ServiceLocator.database.libraryDao()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val results = _query
        .debounce(180)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList()) else dao.searchDocs(q)
        }

    val state: StateFlow<SearchUiState> = combine(_query, results, dao.observeRecent()) { q, hits, recent ->
        SearchUiState(query = q, results = hits, recent = recent, searching = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun clear() {
        _query.value = ""
    }
}
