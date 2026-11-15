package com.passion.knownow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.passion.knownow.data.ServiceLocator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(onBackClick: () -> Unit, onOpenDocument: (Long) -> Unit) {
    val bookmarks by ServiceLocator.database.bookmarkDao().observeAll().collectAsState(initial = emptyList())
    Scaffold(topBar = { TopAppBar(title = { Text("Bookmarks") }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        if (bookmarks.isEmpty()) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No bookmarks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                items(bookmarks, key = { it.documentId }) { bm ->
                    androidx.compose.material3.ElevatedCard(
                        onClick = { onOpenDocument(bm.documentId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = "Document #${bm.documentId}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}