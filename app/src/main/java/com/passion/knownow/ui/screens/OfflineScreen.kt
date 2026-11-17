package com.passion.knownow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.OfflineDocRow
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.ui.components.EmptyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class OfflineUiState(
    val topics: List<PublicationEntity> = emptyList(),
    val docs: List<OfflineDocRow> = emptyList(),
    val loading: Boolean = true,
)

class OfflineViewModel : ViewModel() {
    private val db = ServiceLocator.database

    val state: StateFlow<OfflineUiState> = combine(
        db.publicationDao().observeDownloaded(),
        db.libraryDao().observeOfflineDocs(),
    ) { topics, docs ->
        OfflineUiState(topics = topics, docs = docs, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfflineUiState())
}

/**
 * Everything that actually lives on the device, grouped by topic. This is the "go read
 * something" screen: no catalog, no network, no download state to reason about.
 */
@Composable
fun OfflineScreen(
    onOpenDocument: (Long) -> Unit,
    onOpenTopic: (Long) -> Unit,
    onDiscover: () -> Unit,
) {
    val vm = androidx.lifecycle.viewmodel.compose.viewModel<OfflineViewModel>()
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var topicFilter by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(state.docs, query, topicFilter) {
        state.docs.filter { doc ->
            (topicFilter == null || doc.topicId == topicFilter) &&
                (query.isBlank() || doc.title.contains(query, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column {
                Text("OFFLINE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("Your offline library", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    if (state.docs.isEmpty()) "Nothing downloaded yet"
                    else "${state.docs.size} pages · ${state.topics.size} topics available without a connection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.docs.isEmpty()) {
            item {
                EmptyState(
                    title = "No offline pages yet",
                    body = "Pick a topic in Discover and download it. It will show up here, ready to read on a plane.",
                    actionLabel = "Browse topics",
                    onAction = onDiscover,
                )
            }
        } else {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    placeholder = { Text("Find a page you've downloaded") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                )
            }

            if (state.topics.size > 1) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = topicFilter == null,
                                onClick = { topicFilter = null },
                                label = { Text("All topics") },
                            )
                        }
                        items(state.topics, key = { it.id }) { topic ->
                            FilterChip(
                                selected = topicFilter == topic.id,
                                onClick = {
                                    topicFilter = if (topicFilter == topic.id) null else topic.id
                                },
                                label = { Text(topic.title, maxLines = 1) },
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No downloaded page matches “$query”",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                val grouped = filtered.groupBy { it.topicTitle }
                grouped.forEach { (topicTitle, docs) ->
                    item(key = "header-$topicTitle") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                topicTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${docs.size} pages",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(docs, key = { it.id }) { doc ->
                        OfflineDocCard(doc = doc, onClick = { onOpenDocument(doc.id) })
                    }
                }
            }

            if (topicFilter != null) {
                val topic = state.topics.firstOrNull { it.id == topicFilter }
                if (topic != null) {
                    item {
                        Surface(
                            onClick = { onOpenTopic(topic.id) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Browse ${topic.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "Document list with search",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineDocCard(doc: OfflineDocRow, onClick: () -> Unit) {
    val progress = (doc.progressPercent ?: 0f).coerceIn(0f, 1f)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    doc.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        if (progress > 0.02f) append("${(progress * 100).toInt()}% read")
                        doc.readingTimeMinutes?.let {
                            if (isNotEmpty()) append(" · ")
                            append("$it min")
                        }
                        if (isEmpty()) append("Saved offline")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progress > 0.02f) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
