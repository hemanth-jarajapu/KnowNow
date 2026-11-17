package com.passion.knownow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.core.parsing.DocNormalizer
import com.passion.knownow.core.parsing.DocSection
import com.passion.knownow.core.parsing.FrontmatterParser
import com.passion.knownow.core.parsing.MarkdownToc
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ReaderUiState(
    val document: DocumentEntity? = null,
    val sections: List<DocSection> = emptyList(),
    val plainText: String = "",
    val slug: String? = null,
    val publicationTitle: String? = null,
    val missing: Boolean = false,
    val loading: Boolean = true,
)

class ReaderViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    fun load(id: Long) {
        _state.value = ReaderUiState(loading = true)
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { buildState(id) }
            _state.value = loaded
        }
    }

    private suspend fun buildState(id: Long): ReaderUiState {
        val db = ServiceLocator.database
        val doc = runCatching { db.documentDao().getById(id) }.getOrNull()
            ?: return ReaderUiState(missing = true, loading = false)
        val publication = runCatching { db.publicationDao().getById(doc.publicationId) }.getOrNull()
        val slug = publication?.slug

        val raw = doc.localPath?.let { path ->
            runCatching { File(path).readText() }.getOrDefault("")
        } ?: ""

        if (raw.isBlank()) {
            return ReaderUiState(
                document = doc,
                slug = slug,
                publicationTitle = publication?.title,
                missing = true,
                loading = false,
            )
        }

        // Parsing is the expensive part of opening a page: keep it off the main thread so a
        // long document can never freeze the UI or trip an ANR.
        val body = runCatching {
            val withoutFrontmatter = FrontmatterParser.parse(raw).second
            DocNormalizer.normalize(
                withoutFrontmatter,
                doc.title,
                slug = slug,
                currentPath = doc.relativePath,
            )
        }.getOrDefault("")

        val sections = runCatching { MarkdownToc.sections(body) }.getOrDefault(emptyList())
        return ReaderUiState(
            document = doc,
            sections = sections,
            plainText = body,
            slug = slug,
            publicationTitle = publication?.title,
            loading = false,
        )
    }
}
