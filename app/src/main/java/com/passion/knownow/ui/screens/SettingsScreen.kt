package com.passion.knownow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.passion.knownow.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val theme by vm.themeMode.collectAsState()
    val tokenFlow by vm.gitHubToken.collectAsState()
    var token by remember(tokenFlow) { mutableStateOf(tokenFlow) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            RowButtons(
                options = listOf("System", "Light", "Dark"),
                selected = when (theme) {
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                },
                onSelect = { label ->
                    vm.setThemeMode(
                        when (label) {
                            "Light" -> ThemeMode.LIGHT
                            "Dark" -> ThemeMode.DARK
                            else -> ThemeMode.SYSTEM
                        }
                    )
                },
            )
            Text("GitHub token (optional, for higher rate limits)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Token") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(onClick = { vm.saveGitHubToken(token) }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Save token")
            }

            Text("Font scale", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            val scale by vm.fontScale.collectAsState()
            Slider(value = scale, onValueChange = { vm.setFontScale(it) }, valueRange = 0.85f..1.3f, modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sepia mode")
                Switch(checked = vm.sepia.collectAsState().value, onCheckedChange = { vm.setSepia(it) })
            }

            Text("Reading streak: ${vm.readingStreak.collectAsState().value} days", modifier = Modifier.padding(top = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Download on metered networks")
                Switch(checked = vm.allowMeteredDownloads.collectAsState().value, onCheckedChange = { vm.setAllowMeteredDownloads(it) })
            }
        }
    }
}

@Composable
private fun RowButtons(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        options.forEach { label ->
            Button(onClick = { onSelect(label) }, enabled = label != selected) {
                Text(label)
            }
        }
    }
}