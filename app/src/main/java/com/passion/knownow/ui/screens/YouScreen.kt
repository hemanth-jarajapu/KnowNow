package com.passion.knownow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.passion.knownow.ui.components.relativeTime
import com.passion.knownow.ui.theme.Mint

@Composable
fun YouScreen(
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenOffline: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHighlights: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val vm: LibraryViewModel = viewModel()
    val state by vm.state.collectAsState()
    val settings: SettingsViewModel = viewModel()
    val token by settings.gitHubToken.collectAsState()
    val lastRead = state.recent.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column {
                Text("YOU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("Your space", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, Mint)
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "K",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Offline reader", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            buildString {
                                append("${state.documentCount} pages")
                                if (state.topicCount > 0) append(" · ${state.topicCount} topics")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (lastRead != null) "Last read ${relativeTime(lastRead.lastReadAt)}" else "No reading yet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { GroupLabel("READING") }

        item {
            SettingsRow(
                icon = Icons.Filled.DownloadDone,
                title = "Offline library",
                subtitle = "${state.documentCount} pages you can read anywhere",
                onClick = onOpenOffline,
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.BookmarkBorder,
                title = "Bookmarks",
                subtitle = if (state.bookmarkCount == 0) "Save a page from the reader" else "${state.bookmarkCount} saved pages",
                onClick = onOpenBookmarks,
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.StickyNote2,
                title = "Notes",
                subtitle = if (state.noteCount == 0) "Jot down what matters" else "${state.noteCount} pages with notes",
                onClick = onOpenNotes,
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.Highlight,
                title = "Highlights",
                subtitle = "Passages you marked",
                onClick = onOpenHighlights,
            )
        }

        item { GroupLabel("APP") }

        item {
            SettingsRow(
                icon = Icons.Filled.Palette,
                title = "Appearance & reading",
                subtitle = "Theme, reader palette, font size",
                onClick = onOpenSettings,
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.Download,
                title = "Downloads",
                subtitle = "Queue, pause, resume, storage",
                onClick = onOpenDownloads,
            )
        }
        item {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Library",
                subtitle = "Resume and manage offline topics",
                onClick = onOpenOffline,
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "About & guide",
                subtitle = "Version 0.3.0 · how KnowNow works",
                onClick = onOpenAbout,
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.PrivacyTip,
                title = "Privacy policy",
                subtitle = "What stays on your device",
                onClick = onOpenPrivacy,
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (token.isBlank()) "GitHub token not set" else "GitHub token configured",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        if (token.isBlank()) {
                            "Add a personal token in Settings to raise rate limits for catalog sync."
                        } else {
                            "Catalog sync uses your token. It never leaves this device."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
