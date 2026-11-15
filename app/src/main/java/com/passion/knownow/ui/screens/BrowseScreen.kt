package com.passion.knownow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(onBackClick: () -> Unit, onOpenDocument: (Long) -> Unit) {
    val vm: BrowseViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(state.downloadedFirstDocumentId) {
        state.downloadedFirstDocumentId?.let {
            onOpenDocument(it)
            vm.consumeDownloadedDocument()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Browse") }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (state.isLoadingReleases && state.releases.isEmpty()) {
                CircularProgressIndicator()
                return@Column
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.releases.forEach { rel ->
                    AssistChip(
                        onClick = { vm.selectRelease(rel.name) },
                        label = { Text(rel.displayName) },
                        enabled = rel.name != state.selectedRelease,
                    )
                }
            }
            if (state.isLoadingPublications) {
                CircularProgressIndicator(Modifier.padding(top = 16.dp))
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp)) {
                items(state.publications, key = { it.id }) { pub ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            text = pub.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.downloadingPublicationId == pub.id) {
                            LinearProgressIndicator(
                                progress = { state.downloadProgress },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        } else {
                            Button(
                                onClick = { vm.downloadPublication(pub) },
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Text(if (pub.status == "READY") "Open" else "Download")
                            }
                        }
                    }
                }
            }
        }
    }
}