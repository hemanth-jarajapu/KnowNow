package com.passion.knownow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.passion.knownow.data.db.PublicationEntity
import com.passion.knownow.data.db.RecentDocRow
import com.passion.knownow.ui.components.ContinueReadingCard
import com.passion.knownow.ui.components.EmptyState
import com.passion.knownow.ui.components.SectionHeader
import com.passion.knownow.ui.components.StatPill
import com.passion.knownow.ui.components.relativeTime
import com.passion.knownow.ui.theme.Coral
import com.passion.knownow.ui.theme.Mint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    onOpenDocument: (Long) -> Unit,
    onBrowse: () -> Unit,
    onSearch: () -> Unit,
    onOpenTopic: (Long) -> Unit,
) {
    val vm: LibraryViewModel = viewModel()
    val state by vm.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        SimpleDateFormat("EEEE", Locale.US).format(Date()).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Library",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Surface(
                    onClick = onSearch,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        modifier = Modifier.padding(12.dp).size(22.dp),
                    )
                }
            }
        }

        val latest = state.recent.firstOrNull()
        if (latest != null) {
            val topic = state.topics.firstOrNull { it.id == latest.publicationId }?.title
                ?: "Offline docs"
            val subtitle = if (topic.equals(latest.title, ignoreCase = true)) {
                "${relativeTime(latest.lastReadAt)} · ${latest.progressPercent.times(100).toInt()}% read"
            } else {
                "$topic · ${relativeTime(latest.lastReadAt)}"
            }
            item {
                ContinueReadingCard(
                    title = latest.title,
                    topic = subtitle,
                    progress = latest.progressPercent,
                    onResume = { onOpenDocument(latest.id) },
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("documents", state.documentCount.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatPill("topics", state.topicCount.toString(), Mint, Modifier.weight(1f))
                StatPill("notes", state.noteCount.toString(), Coral, Modifier.weight(1f))
            }
        }

        item {
            SectionHeader(
                title = "Keep going",
                subtitle = if (state.recent.isEmpty()) null else "${state.recent.size} recently opened",
            )
        }

        if (state.recent.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LibraryFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { vm.setFilter(filter) },
                            label = {
                                Text(
                                    when (filter) {
                                        LibraryFilter.ALL -> "All"
                                        LibraryFilter.IN_PROGRESS -> "In progress"
                                        LibraryFilter.DOWNLOADED -> "Downloaded"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }
            items(state.recent, key = { it.id }) { row ->
                RecentDocRowCard(row, onClick = { onOpenDocument(row.id) })
            }
        } else {
            item {
                EmptyState(
                    title = "A quiet shelf",
                    body = "Download a topic from Discover and it will show up here, ready offline.",
                    actionLabel = "Browse catalog",
                    onAction = onBrowse,
                )
            }
        }

        if (state.topics.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Your topics",
                    subtitle = "Available offline",
                    actionLabel = "Explore",
                    onAction = onBrowse,
                )
            }
            items(state.topics, key = { it.id }) { pub ->
                TopicRow(pub, onClick = { onOpenTopic(pub.id) })
            }
        }
    }
}

@Composable
private fun RecentDocRowCard(row: RecentDocRow, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    row.title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    relativeTime(row.lastReadAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (row.progressPercent > 0f) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { row.progressPercent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
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
            )
        }
    }
}

@Composable
private fun TopicRow(pub: PublicationEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    pub.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${pub.docCount} documents · ${pub.releaseName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.ArrowOutward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
