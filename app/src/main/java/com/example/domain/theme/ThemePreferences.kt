package com.example.domain.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User selectable appearance themes:
 * - SYSTEM: Follow device light / dark setting
 * - LIGHT: Professional financial off-white theme with graphite text & lime accents
 * - DARK: iQOO-inspired near-black canvas with graphite surfaces & electric lime accents
 */
enum class AppThemeMode(val storageKey: String, val displayName: String) {
    SYSTEM("system", "System Default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromStorageKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

/**
 * Centralized theme preference repository for persisting theme selection across app launches.
 * Local-first, zero-cloud dependency.
 */
class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _themeMode = MutableStateFlow(loadInitialTheme())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private fun loadInitialTheme(): AppThemeMode {
        val savedKey = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.storageKey)
        return AppThemeMode.fromStorageKey(savedKey)
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.storageKey).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val PREFS_NAME = "pakkakhata_theme_prefs"
        private const val KEY_THEME_MODE = "key_app_theme_mode"

        @Volatile
        private var instance: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
