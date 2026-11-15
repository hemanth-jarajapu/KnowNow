package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReaderViewModel : ViewModel() {
    private val _document = MutableStateFlow<DocumentEntity?>(null)
    val document: StateFlow<DocumentEntity?> = _document.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val doc = ServiceLocator.database.documentDao().getById(id)
            _document.value = doc
            val text = withContext(Dispatchers.IO) {
                doc?.localPath?.let { path -> runCatching { File(path).readText() }.getOrDefault("") } ?: ""
            }
            _content.value = text
        }
    }
}