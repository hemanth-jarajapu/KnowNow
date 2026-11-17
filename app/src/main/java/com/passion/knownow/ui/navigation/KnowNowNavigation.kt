package com.passion.knownow.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.passion.knownow.ui.components.SwipeBackHost
import com.passion.knownow.ui.screens.AboutScreen
import com.passion.knownow.ui.screens.BookmarksScreen
import com.passion.knownow.ui.screens.DiscoverScreen
import com.passion.knownow.ui.screens.DownloadsScreen
import com.passion.knownow.ui.screens.HighlightsScreen
import com.passion.knownow.ui.screens.LibraryScreen
import com.passion.knownow.ui.screens.NotesScreen
import com.passion.knownow.ui.screens.OfflineScreen
import com.passion.knownow.ui.screens.ReaderScreen
import com.passion.knownow.ui.screens.SearchScreen
import com.passion.knownow.ui.screens.SettingsScreen
import com.passion.knownow.ui.screens.TopicScreen
import com.passion.knownow.ui.screens.YouScreen

object Routes {
    const val LIBRARY = "library"
    const val DISCOVER = "discover"
    const val OFFLINE = "offline"
    const val YOU = "you"

    const val DOWNLOADS = "downloads"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val NOTES = "notes"
    const val BOOKMARKS = "bookmarks"
    const val HIGHLIGHTS = "highlights"
    const val ABOUT = "about"

    const val READER = "reader/{documentId}"
    fun reader(documentId: Long) = "reader/$documentId"

    const val TOPIC = "topic/{publicationId}"
    fun topic(publicationId: Long) = "topic/$publicationId"

    val topLevel = setOf(LIBRARY, DISCOVER, OFFLINE, YOU)
}

private const val NAV_ANIM_DURATION = 300

@Composable
fun KnowNowNavigation(navController: NavHostController = rememberNavController()) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val canSwipeBack = entry != null &&
        route !in Routes.topLevel &&
        navController.previousBackStackEntry != null

    SwipeBackHost(enabled = canSwipeBack, onBack = { navController.popBackStack() }) {
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            enterTransition = {
                fadeIn(tween(NAV_ANIM_DURATION)) +
                    slideInHorizontally(tween(NAV_ANIM_DURATION)) { it / 5 } +
                    scaleIn(tween(NAV_ANIM_DURATION), initialScale = 0.97f)
            },
            exitTransition = {
                fadeOut(tween(200)) +
                    slideOutHorizontally(tween(NAV_ANIM_DURATION)) { -it / 10 } +
                    scaleOut(tween(NAV_ANIM_DURATION), targetScale = 0.98f)
            },
            popEnterTransition = {
                fadeIn(tween(220)) +
                    slideInHorizontally(tween(NAV_ANIM_DURATION)) { -it / 7 } +
                    scaleIn(tween(NAV_ANIM_DURATION), initialScale = 0.96f)
            },
            popExitTransition = {
                fadeOut(tween(NAV_ANIM_DURATION)) +
                    slideOutHorizontally(tween(NAV_ANIM_DURATION)) { it } +
                    scaleOut(tween(NAV_ANIM_DURATION), targetScale = 0.92f)
            },
        ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenDocument = { id -> navController.navigate(Routes.reader(id)) },
                    onBrowse = { navController.navigateToTab(Routes.DISCOVER) },
                    onSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenOffline = { navController.navigateToTab(Routes.OFFLINE) },
                    onOpenNotes = { navController.navigate(Routes.NOTES) },
                    onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                )
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    onOpenDocument = { id -> navController.navigate(Routes.reader(id)) },
                    onOpenTopic = { id -> navController.navigate(Routes.topic(id)) },
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onSearch = { navController.navigate(Routes.SEARCH) },
                )
            }
            composable(Routes.OFFLINE) {
                OfflineScreen(
                    onOpenDocument = { id -> navController.navigate(Routes.reader(id)) },
                    onOpenTopic = { id -> navController.navigate(Routes.topic(id)) },
                    onDiscover = { navController.navigate(Routes.DISCOVER) },
                )
            }
            composable(Routes.YOU) {
                YouScreen(
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenOffline = { navController.navigate(Routes.OFFLINE) },
                    onOpenNotes = { navController.navigate(Routes.NOTES) },
                    onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                    onOpenHighlights = { navController.navigate(Routes.HIGHLIGHTS) },
                    onOpenPrivacy = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.DOWNLOADS) {
                DownloadsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.READER) { entry ->
                val id = entry.arguments?.getString("documentId")?.toLongOrNull() ?: 0L
                ReaderScreen(
                    documentId = id,
                    onBackClick = { navController.popBackStack() },
                    onOpenDocument = { docId -> navController.navigate(Routes.reader(docId)) },
                )
            }
            composable(Routes.TOPIC) { entry ->
                val id = entry.arguments?.getString("publicationId")?.toLongOrNull() ?: 0L
                TopicScreen(
                    publicationId = id,
                    onBack = { navController.popBackStack() },
                    onOpenDocument = { docId -> navController.navigate(Routes.reader(docId)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenDocument = { id -> navController.navigate(Routes.reader(id)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Routes.NOTES) {
                NotesScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenDocument = { id -> navController.navigate(Routes.reader(id)) },
                )
            }
            composable(Routes.BOOKMARKS) {
                BookmarksScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenDocument = { id -> navController.navigate(Routes.reader(id)) },
                )
            }
            composable(Routes.HIGHLIGHTS) {
                HighlightsScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}

fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}