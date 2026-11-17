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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.passion.knownow.core.parsing.PreviewEntry
import com.passion.knownow.core.parsing.TopicPreview
import com.passion.knownow.data.db.DocumentEntity
import com.passion.knownow.data.db.DownloadState
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.ui.components.TopicCard
import com.passion.knownow.ui.components.formatBytes

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenDocument: (Long) -> Unit,
    onOpenTopic: (Long) -> Unit,
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
                                Text("â€¢", color = MaterialTheme.colorScheme.secondary)
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
                        Text("Syncing releasesâ€¦", style = MaterialTheme.typography.bodyMedium)
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
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
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
                                "No topics match â€œ${state.query}â€",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }
                    items(state.visiblePublications, key = { it.id }) { pub ->
                        val selected = pub.id in state.selection
                        val ready = pub.status == "READY"
                        Column {
                            TopicCard(
                                publication = pub,
                                task = state.tasks[pub.id],
                                summary = state.hints[pub.slug]?.summary,
                                pageCount = state.hints[pub.slug]?.pageCount?.takeIf { it > 0 },
                                onClick = {
                                    when {
                                        state.selectionMode -> vm.toggleSelection(pub.id)
                                        ready -> onOpenTopic(pub.id)
                                        else -> vm.openDetail(pub)
                                    }
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
                                            ready = ready,
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
            ready = pub.status == "READY",
            preview = state.preview,
            previewLoading = state.previewLoading,
            previewError = state.previewError,
            onDismiss = vm::closeDetail,
            onDownload = { vm.download(pub) },
            onRefresh = { vm.refresh(pub) },
            onRetryPreview = vm::retryPreview,
            onOpenTopic = {
                vm.closeDetail()
                onOpenTopic(pub.id)
            },
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
    ready: Boolean,
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
                Icon(
                    Icons.Filled.DownloadDone,
                    contentDescription = "Saved offline",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            else -> {
                if (ready) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Filled.DownloadDone,
                            contentDescription = "Saved offline â€” tap to refresh",
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                } else {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = "Download ${pub.title}")
                    }
                }
            }
        }
    }
}

/**
 * The "what am I downloading?" sheet.
 *
 * A publication cannot be described by its title alone ("Solutions", "Core Business Suite"),
 * so before anything is downloaded this shows the topic's own landing-page summary and its
 * table of contents, searchable, straight from the publication's `index.md`. Once the topic
 * is on the device the same sheet lists the local pages instead.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TopicDetailSheet(
    publication: PublicationEntity,
    documents: List<DocumentEntity>,
    taskState: String?,
    ready: Boolean,
    preview: TopicPreview?,
    previewLoading: Boolean,
    previewError: String?,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onRefresh: () -> Unit,
    onRetryPreview: () -> Unit,
    onOpenTopic: () -> Unit,
    onOpenDocument: (Long) -> Unit,
) {
    var query by remember(publication.id) { mutableStateOf("") }
    var limit by remember(publication.id) { mutableIntStateOf(PREVIEW_CHUNK) }

    val entries = preview?.entries.orEmpty()
    val filtered = remember(entries, query) {
        if (query.isBlank()) {
            entries
        } else {
            entries.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.ancestors.any { parent -> parent.contains(query, ignoreCase = true) }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                publication.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append(publication.releaseName)
                    val pages = if (ready) publication.docCount else preview?.pageCount ?: 0
                    if (pages > 0) append(" · $pages pages")
                    if (publication.totalBytes > 0) append(" · ${formatBytes(publication.totalBytes)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                taskState == DownloadState.RUNNING || taskState == DownloadState.QUEUED -> {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("Downloading…", style = MaterialTheme.typography.labelSmall)
                }
                taskState == DownloadState.PAUSED -> {
                    Spacer(Modifier.height(12.dp))
                    Text("Paused", style = MaterialTheme.typography.labelMedium)
                }
                taskState == DownloadState.FAILED -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Download failed — retry from the queue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                ready -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Available offline",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ready && taskState != DownloadState.RUNNING && taskState != DownloadState.QUEUED) {
                    Button(onClick = onOpenTopic) {
                        Icon(Icons.Filled.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open topic")
                    }
                    OutlinedButton(onClick = onRefresh) { Text("Refresh") }
                } else if (taskState == null || taskState == DownloadState.CANCELLED || taskState == DownloadState.FAILED) {
                    Button(onClick = onDownload) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (preview != null && preview.pageCount > 0) {
                                "Download ${preview.pageCount} pages"
                            } else {
                                "Download topic"
                            }
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 14.dp))

            when {
                ready && documents.isEmpty() -> SheetNote("Loading this topic…")
                ready -> {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                previewLoading -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Reading the topic contents…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                preview == null -> Column {
                    SheetNote(
                        previewError
                            ?: "Previews need a connection the first time. Download the topic to read it offline."
                    )
                    TextButton(onClick = onRetryPreview) { Text("Try again") }
                }
                else -> {
                    preview.summary?.let { summary ->
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            limit = PREVIEW_CHUNK
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search these ${entries.size} pages") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (query.isBlank()) "What's inside" else "${filtered.size} matching pages",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (filtered.isEmpty()) {
                        SheetNote("No page title mentions “$query”.")
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            items(filtered.take(limit)) { entry -> ContentRow(entry) }
                            if (filtered.size > limit) {
                                item {
                                    TextButton(
                                        onClick = { limit += PREVIEW_CHUNK },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Show more (${filtered.size - limit} left)")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun ContentRow(entry: PreviewEntry) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (entry.ancestors.isNotEmpty()) {
            Text(
                entry.ancestors.take(2).reversed().joinToString(" › "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            entry.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.description.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val PREVIEW_CHUNK = 25
