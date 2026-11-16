package com.passion.knownow.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.passion.knownow.data.db.DocumentEntity
import com.passion.knownow.data.db.DownloadState
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.ui.components.TopicCard
import com.passion.knownow.ui.components.formatBytes

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenDocument: (Long) -> Unit,
    onOpenDownloads: () -> Unit,
    onSearch: () -> Unit,
) {
    val vm: DiscoverViewModel = viewModel()
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        if (state.selectionMode) {
            SelectionBar(
                count = state.selection.size,
                onClose = vm::clearSelection,
                onDownload = vm::downloadSelected,
            )
        } else {
            DiscoverHeader(
                onSearch = onSearch,
                onDownloads = onOpenDownloads,
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.releases, key = { it.name }) { release ->
                FilterChip(
                    selected = release.name == state.selectedRelease,
                    onClick = { vm.selectRelease(release.name) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(release.displayName)
                            if (release.isLatest) {
                                Spacer(Modifier.width(4.dp))
                                Text("•", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("Filter topics") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
        )

        when {
            state.syncing && state.publications.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Syncing releases…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            state.publications.isEmpty() && state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.error?.let { error ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = vm::syncReleases) { Text("Retry") }
                                }
                            }
                        }
                    }
                    if (state.visiblePublications.isEmpty() && !state.loading) {
                        item {
                            Text(
                                "No topics match “${state.query}”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }
                    items(state.visiblePublications, key = { it.id }) { pub ->
                        val selected = pub.id in state.selection
                        Column {
                            TopicCard(
                                publication = pub,
                                task = state.tasks[pub.id],
                                onClick = {
                                    if (state.selectionMode) vm.toggleSelection(pub.id) else vm.openDetail(pub)
                                },
                                onLongClick = { vm.startSelection(pub.id) },
                                onDownload = { vm.download(pub) },
                                highlighted = selected,
                                trailing = {
                                    if (state.selectionMode) {
                                        SelectionMark(selected)
                                    } else {
                                        TopicActions(
                                            pub = pub,
                                            taskState = state.tasks[pub.id]?.state,
                                            onDownload = { vm.download(pub) },
                                            onRefresh = { vm.refresh(pub) },
                                            onPause = { vm.pause(pub) },
                                            onResume = { vm.resume(pub) },
                                            onCancel = { vm.cancel(pub) },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    state.detailPublication?.let { pub ->
        TopicDetailSheet(
            publication = pub,
            documents = state.detailDocuments,
            taskState = state.tasks[pub.id]?.state,
            onDismiss = vm::closeDetail,
            onDownload = { vm.download(pub) },
            onRefresh = { vm.refresh(pub) },
            onOpenDocument = onOpenDocument,
        )
    }
}

@Composable
private fun DiscoverHeader(onSearch: () -> Unit, onDownloads: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Discover",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "ServiceNow docs, offline-first",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        }
        IconButton(onClick = onDownloads) {
            Icon(Icons.Filled.Download, contentDescription = "Downloads")
        }
    }
}

@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onDownload: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
            }
            Text(
                "$count selected",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onDownload) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download")
            }
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TopicActions(
    pub: PublicationEntity,
    taskState: String?,
    onDownload: () -> Unit,
    onRefresh: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (taskState) {
            DownloadState.RUNNING, DownloadState.QUEUED -> {
                IconButton(onClick = onPause) { Icon(Icons.Filled.Pause, contentDescription = "Pause") }
                IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
            }
            DownloadState.PAUSED -> {
                IconButton(onClick = onResume) { Icon(Icons.Filled.PlayArrow, contentDescription = "Resume") }
                IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
            }
            DownloadState.FAILED -> {
                IconButton(onClick = onResume) { Icon(Icons.Filled.Refresh, contentDescription = "Retry") }
            }
            DownloadState.COMPLETED -> {
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }
            }
            else -> {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = "Download ${pub.title}")
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TopicDetailSheet(
    publication: PublicationEntity,
    documents: List<DocumentEntity>,
    taskState: String?,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDocument: (Long) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(publication.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append(publication.releaseName)
                    if (publication.docCount > 0) append(" · ${publication.docCount} documents")
                    if (publication.totalBytes > 0) append(" · ${formatBytes(publication.totalBytes)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            when (taskState) {
                DownloadState.RUNNING, DownloadState.QUEUED -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("Downloading…", style = MaterialTheme.typography.labelSmall)
                }
                DownloadState.PAUSED -> Text("Paused", style = MaterialTheme.typography.labelMedium)
                DownloadState.FAILED -> Text(
                    "Download failed — retry from the queue",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                DownloadState.COMPLETED -> Text(
                    "Available offline",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                else -> Unit
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (taskState == DownloadState.COMPLETED) {
                    Button(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh")
                    }
                } else if (taskState == null || taskState == DownloadState.CANCELLED) {
                    Button(onClick = onDownload) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download topic")
                    }
                }
            }

            if (documents.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(documents, key = { it.id }) { doc ->
                        Surface(
                            onClick = { onOpenDocument(doc.id) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Article,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    doc.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
