package com.passion.knownow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.passion.knownow.ui.components.AppBackground
import com.passion.knownow.ui.navigation.KnowNowNavigation
import com.passion.knownow.ui.navigation.Routes
import com.passion.knownow.ui.navigation.navigateToTab
import com.passion.knownow.ui.screens.DownloadsViewModel
import com.passion.knownow.ui.screens.SettingsViewModel
import com.passion.knownow.ui.theme.KnowNowTheme

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.LIBRARY, "Library", Icons.AutoMirrored.Filled.MenuBook),
    TopLevelDestination(Routes.DISCOVER, "Discover", Icons.Filled.Explore),
    TopLevelDestination(Routes.OFFLINE, "Offline", Icons.Filled.DownloadDone),
    TopLevelDestination(Routes.YOU, "You", Icons.Filled.Person),
)

@Composable
fun KnowNowApp() {
    val settings: SettingsViewModel = viewModel()
    val themeMode by settings.themeMode.collectAsState()
    val dynamicColor by settings.dynamicColor.collectAsState()

    KnowNowTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        AppShell()
    }
}

@Composable
private fun AppShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showChrome = route == null || route in Routes.topLevel

    val downloads: DownloadsViewModel = viewModel()
    val downloadState by downloads.state.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp

        if (showChrome) {
            AppBackground(Modifier.fillMaxSize()) {}
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showChrome && !expanded) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                    ) {
                        topLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = route == destination.route,
                                onClick = { navController.navigateToTab(destination.route) },
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                label = { Text(destination.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (showChrome && expanded) {
                    NavigationRail(
                        containerColor = Color.Transparent,
                        modifier = Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                    ) {
                        topLevelDestinations.forEach { destination ->
                            NavigationRailItem(
                                selected = route == destination.route,
                                onClick = { navController.navigateToTab(destination.route) },
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .then(if (showChrome) Modifier.statusBarsPadding() else Modifier)
                ) {
                    KnowNowNavigation(navController = navController)

                    val showTray = showChrome && downloadState.active.isNotEmpty() &&
                        route != Routes.DOWNLOADS
                    DownloadTrayHost(
                        visible = showTray,
                        fraction = downloadState.fraction,
                        completed = downloadState.completedUnits,
                        total = downloadState.totalUnits,
                        running = downloadState.isRunning,
                        onClick = { navController.navigate(Routes.DOWNLOADS) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadTrayHost(
    visible: Boolean,
    fraction: Float,
    completed: Int,
    total: Int,
    running: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        DownloadTray(
            fraction = fraction,
            completed = completed,
            total = total,
            running = running,
            onClick = onClick,
        )
    }
}

@Composable
private fun DownloadTray(
    fraction: Float,
    completed: Int,
    total: Int,
    running: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.width(280.dp)) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (running) "Downloading" else "Queue paused",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (total > 0) "$completed of $total documents" else "Preparing…",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.2f),
            )
        }
    }
}
