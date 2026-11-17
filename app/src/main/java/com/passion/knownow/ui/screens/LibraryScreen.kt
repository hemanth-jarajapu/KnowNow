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
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.passion.knownow.data.db.RecentDocRow
import com.passion.knownow.ui.components.ContinueReadingCard
import com.passion.knownow.ui.components.EmptyState
import com.passion.knownow.ui.components.SectionHeader
import com.passion.knownow.ui.components.relativeTime
import com.passion.knownow.ui.theme.Mint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home. One entry point per intent:
 *
 * - "keep reading where I stopped" -> the resume card
 * - "read something I already have" -> Offline
 * - "what did I mark" -> Bookmarks / Notes
 * - "what did I read lately" -> recently read
 *
 * Nothing here repeats another tab: the offline shelf lives in Offline, the catalog in
 * Discover, so Library stays a short, single-column page that fits on one screen.
 */
@Composable
fun LibraryScreen(
    onOpenDocument: (Long) -> Unit,
    onBrowse: () -> Unit,
    onSearch: () -> Unit,
    onOpenOffline: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenBookmarks: () -> Unit,
) {
    val vm: LibraryViewModel = viewModel()
    val state by vm.state.collectAsState()

    val latest = state.recent.firstOrNull()
    // The resume card already shows the most recent page; repeating it in the list below
    // would be the same row twice.
    val earlier = if (latest != null) state.recent.drop(1) else state.recent
    val hasDownloads = state.documentCount > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
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

        if (latest != null) {
            val topic = latest.topicTitle?.takeIf { it.isNotBlank() }
            val subtitle = if (topic == null || topic.equals(latest.title, ignoreCase = true)) {
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
                ShortcutTile(
                    icon = Icons.Filled.DownloadDone,
                    value = state.topicCount.toString(),
                    label = if (state.topicCount == 1) "Topic offline" else "Topics offline",
                    subLabel = "${state.documentCount} pages",
                    tint = Mint,
                    onClick = onOpenOffline,
                    modifier = Modifier.weight(1f),
                )
                ShortcutTile(
                    icon = Icons.Filled.BookmarkBorder,
                    value = state.bookmarkCount.toString(),
                    label = "Bookmarks",
                    subLabel = if (state.bookmarkCount == 0) "Nothing saved" else "Saved pages",
                    tint = com.passion.knownow.ui.theme.Violet,
                    onClick = onOpenBookmarks,
                    modifier = Modifier.weight(1f),
                )
                ShortcutTile(
                    icon = Icons.AutoMirrored.Filled.StickyNote2,
                    value = state.noteCount.toString(),
                    label = "Notes",
                    subLabel = if (state.noteCount == 0) "No thoughts yet" else "Your thoughts",
                    tint = com.passion.knownow.ui.theme.Coral,
                    onClick = onOpenNotes,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (earlier.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently read")
            }
            items(earlier, key = { "doc-${it.id}" }) { row ->
                RecentDocRowCard(row, onClick = { onOpenDocument(row.id) })
            }
        }

        if (!hasDownloads) {
            item {
                EmptyState(
                    title = "Your shelf is empty",
                    body = "Download a topic from Discover and its pages will be readable here, offline.",
                    actionLabel = "Browse catalog",
                    onAction = onBrowse,
                )
            }
        } else if (latest == null) {
            item {
                EmptyState(
                    title = "Nothing opened yet",
                    body = "${state.documentCount} pages are ready on this device. Pick one to start reading.",
                    actionLabel = "Open offline shelf",
                    onAction = onOpenOffline,
                )
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    icon: ImageVector,
    value: String,
    label: String,
    subLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                val meta = if (row.topicTitle.isNullOrBlank() || row.topicTitle.equals(row.title, ignoreCase = true)) {
                    relativeTime(row.lastReadAt)
                } else {
                    "${row.topicTitle} · ${relativeTime(row.lastReadAt)}"
                }
                Text(
                    meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
