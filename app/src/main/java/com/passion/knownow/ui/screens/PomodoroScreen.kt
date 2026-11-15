package com.passion.knownow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(onBackClick: () -> Unit) {
    var remaining by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var workMode by remember { mutableStateOf(true) }

    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1000)
            remaining--
        }
        if (remaining == 0) {
            running = false
            workMode = !workMode
            remaining = if (workMode) 25 * 60 else 5 * 60
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Pomodoro") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (workMode) "Focus" else "Break", style = MaterialTheme.typography.headlineMedium)
            Text(String.format(androidx.compose.ui.platform.LocalConfiguration.current.locales[0], "%02d:%02d", remaining / 60, remaining % 60), style = MaterialTheme.typography.displayLarge)
            Button(onClick = { running = !running }, modifier = Modifier.padding(top = 16.dp)) {
                Text(if (running) "Pause" else "Start")
            }
            Button(onClick = { running = false; remaining = if (workMode) 25 * 60 else 5 * 60 }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Reset")
            }
        }
    }
}