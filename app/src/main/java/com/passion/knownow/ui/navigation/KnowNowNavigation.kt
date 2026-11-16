package com.passion.knownow.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.passion.knownow.ui.screens.AboutScreen
import com.passion.knownow.ui.screens.BookmarksScreen
import com.passion.knownow.ui.screens.DiscoverScreen
import com.passion.knownow.ui.screens.DownloadsScreen
import com.passion.knownow.ui.screens.FlashcardsScreen
import com.passion.knownow.ui.screens.HighlightsScreen
import com.passion.knownow.ui.screens.LibraryScreen
import com.passion.knownow.ui.screens.NotesScreen
import com.passion.knownow.ui.screens.PomodoroScreen
import com.passion.knownow.ui.screens.ReaderScreen
import com.passion.knownow.ui.screens.SearchScreen
import com.passion.knownow.ui.screens.SettingsScreen
import com.passion.knownow.ui.screens.StudyScreen
import com.passion.knownow.ui.screens.TopicScreen
import com.passion.knownow.ui.screens.YouScreen

object Routes {
    const val LIBRARY = "library"
    const val DISCOVER = "discover"
    const val STUDY = "study"
    const val YOU = "you"

    const val DOWNLOADS = "downloads"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val NOTES = "notes"
    const val BOOKMARKS = "bookmarks"
    const val HIGHLIGHTS = "highlights"
    const val POMODORO = "pomodoro"
    const val FLASHCARDS = "flashcards"
    const val ABOUT = "about"

    const val READER = "reader/{documentId}"
    fun reader(documentId: Long) = "reader/$documentId"

    const val TOPIC = "topic/{publicationId}"
    fun topic(publicationId: Long) = "topic/$publicationId"

    val topLevel = setOf(LIBRARY, DISCOVER, STUDY, YOU)
}

private const val NAV_ANIM_DURATION = 320

@Composable
fun KnowNowNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        enterTransition = {
            fadeIn(tween(NAV_ANIM_DURATION)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(NAV_ANIM_DURATION),
                )
        },
        exitTransition = {
            fadeOut(tween(NAV_ANIM_DURATION)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(NAV_ANIM_DURATION),
                    targetOffset = { it / 5 },
                )
        },
        popEnterTransition = {
            fadeIn(tween(NAV_ANIM_DURATION)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(NAV_ANIM_DURATION),
                    initialOffset = { it / 5 },
                )
        },
        popExitTransition = {
            fadeOut(tween(NAV_ANIM_DURATION)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(NAV_ANIM_DURATION),
                )
        },
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpenDocument = { id -> navController.navigate(Routes.reader(id)) },
                onBrowse = { navController.navigateToTab(Routes.DISCOVER) },
                onSearch = { navController.navigate(Routes.SEARCH) },
                onOpenTopic = { id -> navController.navigate(Routes.topic(id)) },
            )
        }
        composable(Routes.DISCOVER) {
            DiscoverScreen(
                onOpenDocument = { id -> navController.navigate(Routes.reader(id)) },
                onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                onSearch = { navController.navigate(Routes.SEARCH) },
            )
        }
        composable(Routes.STUDY) {
            StudyScreen(
                onOpenPomodoro = { navController.navigate(Routes.POMODORO) },
                onOpenFlashcards = { navController.navigate(Routes.FLASHCARDS) },
                onOpenNotes = { navController.navigate(Routes.NOTES) },
                onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                onOpenHighlights = { navController.navigate(Routes.HIGHLIGHTS) },
            )
        }
        composable(Routes.YOU) {
            YouScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenLibrary = { navController.navigateToTab(Routes.LIBRARY) },
                onOpenPrivacy = {
                    navController.navigate(Routes.ABOUT)
                },
            )
        }
        composable(Routes.DOWNLOADS) {
            DownloadsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.READER) { entry ->
            val id = entry.arguments?.getString("documentId")?.toLongOrNull() ?: 0L
            ReaderScreen(documentId = id, onBackClick = { navController.popBackStack() }, onOpenDocument = { docId -> navController.navigate(Routes.reader(docId)) })
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
        composable(Routes.POMODORO) {
            PomodoroScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.FLASHCARDS) {
            FlashcardsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBackClick = { navController.popBackStack() })
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
