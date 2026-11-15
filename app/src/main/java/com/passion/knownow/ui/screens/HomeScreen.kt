package com.passion.knownow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.passion.knownow.ui.screens.SettingsViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onBrowseClick: () -> Unit, onSearchClick: () -> Unit, onSettingsClick: () -> Unit, onNotesClick: () -> Unit, onBookmarksClick: () -> Unit, onHighlightsClick: () -> Unit, onPomodoroClick: () -> Unit, onFlashcardsClick: () -> Unit, onAboutClick: () -> Unit) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val streak by settingsViewModel.readingStreak.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "KnowNow", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text(text = "ServiceNow docs, offline & beautiful.", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Reading streak: $streak days", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onBrowseClick, modifier = Modifier.padding(top = 24.dp)) {
            Text("Browse releases")
        }
        Button(onClick = onSearchClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("Search downloaded docs")
        }
        Button(onClick = onSettingsClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("Settings")
        }
        Button(onClick = onNotesClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("My notes")
        }
        Button(onClick = onBookmarksClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("Bookmarks")
        }
        Button(onClick = onHighlightsClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("Highlights")
        }
        Button(onClick = onPomodoroClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("Pomodoro")
        }
        Button(onClick = onFlashcardsClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("Flashcards")
        }
        Button(onClick = onAboutClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("About & Guide")
        }
    }
}