package com.passion.knownow.ui.screens

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.ui.theme.ReaderThemeMode
import com.passion.knownow.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val dataStore get() = ServiceLocator.dataStore
    private val themeKey = stringPreferencesKey("theme_mode")
    private val tokenKey = stringPreferencesKey("github_token")
    private val fontScaleKey = stringPreferencesKey("font_scale")
    private val sepiaKey = stringPreferencesKey("sepia")
    private val readerThemeKey = stringPreferencesKey("reader_theme")
    private val dynamicColorKey = stringPreferencesKey("dynamic_color")
    private val streakKey = stringPreferencesKey("reading_streak")
    private val lastReadDateKey = stringPreferencesKey("last_read_date")
    private val meteredKey = stringPreferencesKey("allow_metered_downloads")

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _readerTheme = MutableStateFlow(ReaderThemeMode.PAPER)
    val readerTheme: StateFlow<ReaderThemeMode> = _readerTheme.asStateFlow()

    private val _dynamicColor = MutableStateFlow(false)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    val gitHubToken: StateFlow<String> = dataStore.data
        .map { it[tokenKey].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val fontScale: StateFlow<Float> = dataStore.data
        .map { it[fontScaleKey]?.toFloatOrNull() ?: 1f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1f)

    val readingStreak: StateFlow<Int> = dataStore.data
        .map { it[streakKey]?.toIntOrNull() ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allowMeteredDownloads: StateFlow<Boolean> = dataStore.data
        .map { it[meteredKey] == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _themeMode.value = when (prefs[themeKey]) {
                    "LIGHT" -> ThemeMode.LIGHT
                    "DARK" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
                val storedTheme = prefs[readerThemeKey]
                _readerTheme.value = when {
                    storedTheme != null -> runCatching { ReaderThemeMode.valueOf(storedTheme) }
                        .getOrDefault(ReaderThemeMode.PAPER)
                    prefs[sepiaKey] == "true" -> ReaderThemeMode.SEPIA
                    else -> ReaderThemeMode.PAPER
                }
                _dynamicColor.value = prefs[dynamicColorKey] == "true"
                val token = prefs[tokenKey]
                if (!token.isNullOrBlank() && token != ServiceLocator.githubToken) {
                    ServiceLocator.githubToken = token
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[themeKey] = when (mode) {
                    ThemeMode.LIGHT -> "LIGHT"
                    ThemeMode.DARK -> "DARK"
                    ThemeMode.SYSTEM -> "SYSTEM"
                }
            }
        }
    }

    fun readerThemeOverrideFlow(documentId: Long): kotlinx.coroutines.flow.Flow<ReaderThemeMode?> {
        val key = stringPreferencesKey("reader_theme_override_$documentId")
        return dataStore.data.map { prefs ->
            prefs[key]?.let { runCatching { ReaderThemeMode.valueOf(it) }.getOrNull() }
        }
    }

    fun setReaderThemeOverride(documentId: Long, mode: ReaderThemeMode?) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                val key = stringPreferencesKey("reader_theme_override_$documentId")
                if (mode == null) prefs.remove(key) else prefs[key] = mode.name
            }
        }
    }

    fun setReaderTheme(mode: ReaderThemeMode) {
        _readerTheme.value = mode
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[readerThemeKey] = mode.name }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[dynamicColorKey] = enabled.toString() }
        }
    }

    fun saveGitHubToken(token: String) {
        ServiceLocator.githubToken = token.ifBlank { null }
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[tokenKey] = token }
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[fontScaleKey] = scale.toString() }
        }
    }

    fun setSepia(enabled: Boolean) {
        setReaderTheme(if (enabled) ReaderThemeMode.SEPIA else ReaderThemeMode.PAPER)
    }

    fun setAllowMeteredDownloads(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[meteredKey] = enabled.toString() }
        }
    }

    fun recordRead() {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toString()
            dataStore.edit { prefs ->
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
