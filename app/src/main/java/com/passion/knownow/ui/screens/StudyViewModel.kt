package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.data.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StudyViewModel : ViewModel() {
    private val dao = ServiceLocator.database.libraryDao()

    val bookmarkCount: StateFlow<Int> = dao.observeBookmarkIds()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val noteCount: StateFlow<Int> = dao.observeNoteDocumentIds()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val documentCount: StateFlow<Int> = dao.observeDocumentCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
