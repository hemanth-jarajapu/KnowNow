package com.passion.knownow.ui.screens

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "knownow_settings")

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext
    private val themeKey = stringPreferencesKey("theme_mode")
    private val tokenKey = stringPreferencesKey("github_token")
    private val fontScaleKey = stringPreferencesKey("font_scale")
    private val sepiaKey = stringPreferencesKey("sepia")
    private val streakKey = stringPreferencesKey("reading_streak")
    private val lastReadDateKey = stringPreferencesKey("last_read_date")
    private val meteredKey = stringPreferencesKey("allow_metered_downloads")

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    val gitHubToken: StateFlow<String> = context.dataStore.data
        .map { it[tokenKey].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val fontScale: StateFlow<Float> = context.dataStore.data
        .map { it[fontScaleKey]?.toFloatOrNull() ?: 1f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1f)

    val sepia: StateFlow<Boolean> = context.dataStore.data
        .map { it[sepiaKey] == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val readingStreak: StateFlow<Int> = context.dataStore.data
        .map { it[streakKey]?.toIntOrNull() ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allowMeteredDownloads: StateFlow<Boolean> = context.dataStore.data
        .map { it[meteredKey] == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            context.dataStore.data.map { it[themeKey] }.collect { value ->
                _themeMode.value = when (value) {
                    "LIGHT" -> ThemeMode.LIGHT
                    "DARK" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[themeKey] = when (mode) {
                    ThemeMode.LIGHT -> "LIGHT"
                    ThemeMode.DARK -> "DARK"
                    ThemeMode.SYSTEM -> "SYSTEM"
                }
            }
        }
    }

    fun saveGitHubToken(token: String) {
        viewModelScope.launch {
            context.dataStore.edit { prefs -> prefs[tokenKey] = token }
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            context.dataStore.edit { prefs -> prefs[fontScaleKey] = scale.toString() }
        }
    }

    fun setSepia(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs -> prefs[sepiaKey] = enabled.toString() }
        }
    }

    fun setAllowMeteredDownloads(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs -> prefs[meteredKey] = enabled.toString() }
        }
    }

    fun recordRead() {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toString()
            context.dataStore.edit { prefs ->
                val last = prefs[lastReadDateKey]
                val streak = prefs[streakKey]?.toIntOrNull() ?: 0
                if (last == today) return@edit
                val yesterday = java.time.LocalDate.now().minusDays(1).toString()
                val newStreak = if (last == yesterday) streak + 1 else 1
                prefs[lastReadDateKey] = today
                prefs[streakKey] = newStreak.toString()
            }
        }
    }
}