package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.DocumentEntity
import com.passion.knownow.data.db.PublicationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class TopicViewModel(private val publicationId: Long) : ViewModel() {

    private val db = ServiceLocator.database
    private val query = MutableStateFlow("")

    val publication: StateFlow<PublicationEntity?> = flow {
        emit(db.publicationDao().getById(publicationId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val documents: StateFlow<List<DocumentEntity>> =
        combine(db.documentDao().observeForPublication(publicationId), query) { docs, q ->
            if (q.isBlank()) {
                docs
            } else {
                docs.filter { doc ->
                    doc.title.contains(q, ignoreCase = true) ||
                        doc.relativePath.contains(q, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchQuery: StateFlow<String> = query.asStateFlow()

    fun setQuery(value: String) {
        query.value = value
    }

    companion object {
        fun factory(publicationId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { TopicViewModel(publicationId) }
        }
    }
}
