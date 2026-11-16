package com.passion.knownow.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("About & Guide") }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("KnowNow", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text("Offline ServiceNow documentation reader", style = MaterialTheme.typography.bodyMedium)
            Text("Version 0.2.0", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))

            Text("Guide", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text("1. Discover releases and topics, long-press to select several.", style = MaterialTheme.typography.bodyMedium)
            Text("2. Watch the queue download while you keep browsing; pause or resume any time.", style = MaterialTheme.typography.bodyMedium)
            Text("3. Read offline with an editorial palette, contents sheet, and adjustable type.", style = MaterialTheme.typography.bodyMedium)
            Text("4. Bookmark, highlight, and take notes while reading.", style = MaterialTheme.typography.bodyMedium)
            Text("5. Search across downloaded docs and notes, then study with Pomodoro and flashcards.", style = MaterialTheme.typography.bodyMedium)

            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/hemanth-jarajapu/KnowNow/blob/master/PRIVACY.md".toUri()))
            }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Privacy Policy")
            }
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/hemanth-jarajapu/KnowNow".toUri()))
            }) {
                Text("Source code")
            }
        }
    }
}