package com.passion.knownow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.passion.knownow.ui.screens.AboutScreen
import com.passion.knownow.ui.screens.BookmarksScreen
import com.passion.knownow.ui.screens.BrowseScreen
import com.passion.knownow.ui.screens.FlashcardsScreen
import com.passion.knownow.ui.screens.HighlightsScreen
import com.passion.knownow.ui.screens.HomeScreen
import com.passion.knownow.ui.screens.NotesScreen
import com.passion.knownow.ui.screens.PomodoroScreen
import com.passion.knownow.ui.screens.ReaderScreen
import com.passion.knownow.ui.screens.SearchScreen
import com.passion.knownow.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val BROWSE = "browse"
    const val READER = "reader/{documentId}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val NOTES = "notes"
    const val BOOKMARKS = "bookmarks"
    const val HIGHLIGHTS = "highlights"
    const val POMODORO = "pomodoro"
    const val FLASHCARDS = "flashcards"
    const val ABOUT = "about"
}

@Composable
fun KnowNowNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onBrowseClick = { navController.navigate(Routes.BROWSE) },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onNotesClick = { navController.navigate(Routes.NOTES) },
                onBookmarksClick = { navController.navigate(Routes.BOOKMARKS) },
                onHighlightsClick = { navController.navigate(Routes.HIGHLIGHTS) },
                onPomodoroClick = { navController.navigate(Routes.POMODORO) },
                onFlashcardsClick = { navController.navigate(Routes.FLASHCARDS) },
                onAboutClick = { navController.navigate(Routes.ABOUT) },
            )
        }
        composable(Routes.BROWSE) {
            BrowseScreen(
                onBackClick = { navController.popBackStack() },
                onOpenDocument = { id -> navController.navigate("reader/$id") },
            )
        }
        composable(Routes.READER) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("documentId")?.toLongOrNull() ?: 0L
            ReaderScreen(documentId = id, onBackClick = { navController.popBackStack() })
        }
        composable(Routes.SEARCH) {
            SearchScreen(onBackClick = { navController.popBackStack() }, onOpenDocument = { id -> navController.navigate("reader/$id") })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.NOTES) {
            NotesScreen(onBackClick = { navController.popBackStack() }, onOpenDocument = { id -> navController.navigate("reader/$id") })
        }
        composable(Routes.BOOKMARKS) {
            BookmarksScreen(onBackClick = { navController.popBackStack() }, onOpenDocument = { id -> navController.navigate("reader/$id") })
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