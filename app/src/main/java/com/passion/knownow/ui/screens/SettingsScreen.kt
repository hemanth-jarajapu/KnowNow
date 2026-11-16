package com.passion.knownow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.passion.knownow.ui.theme.ReaderThemeMode
import com.passion.knownow.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val theme by vm.themeMode.collectAsState()
    val readerTheme by vm.readerTheme.collectAsState()
    val dynamicColor by vm.dynamicColor.collectAsState()
    val tokenFlow by vm.gitHubToken.collectAsState()
    var token by remember(tokenFlow) { mutableStateOf(tokenFlow) }
    val scale by vm.fontScale.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance & reading", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("App theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                    .forEach { (mode, label) ->
                        FilterChip(
                            selected = theme == mode,
                            onClick = { vm.setThemeMode(mode) },
                            label = { Text(label) },
                        )
                    }
            }

            ToggleRow(
                title = "Use wallpaper colors",
                subtitle = "Material You palette when supported",
                checked = dynamicColor,
                onChange = vm::setDynamicColor,
            )

            Text(
                "Reader palette",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Applies to documents. Override it any time from the reader.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ReaderThemeMode.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { mode ->
                            FilterChip(
                                selected = readerTheme == mode,
                                onClick = { vm.setReaderTheme(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ReaderThemeMode.PAPER -> "Paper"
                                            ReaderThemeMode.SEPIA -> "Sepia"
                                            ReaderThemeMode.NIGHT -> "Night"
                                            ReaderThemeMode.EXPRESSIVE -> "Vivid"
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }

            Text(
                "Reader text size · ${(scale * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Slider(value = scale, onValueChange = { vm.setFontScale(it) }, valueRange = 0.85f..1.35f)

            ToggleRow(
                title = "Download on metered networks",
                subtitle = "Otherwise downloads wait for Wi-Fi",
                checked = vm.allowMeteredDownloads.collectAsState().value,
                onChange = vm::setAllowMeteredDownloads,
            )

            Text(
                "GitHub token (optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Raises catalog sync rate limits. Stored only on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Personal access token") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
            )
            Button(onClick = { vm.saveGitHubToken(token) }) { Text("Save token") }

            Text(
                "Reading streak: ${vm.readingStreak.collectAsState().value} days",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
