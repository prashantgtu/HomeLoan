package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "app_theme"
    }

    private val _themeFlow = MutableStateFlow(getThemePreference())
    val themeFlow: StateFlow<String> = _themeFlow

    fun getThemePreference(): String {
        return prefs.getString(KEY_THEME, "system") ?: "system"
    }

    fun setThemePreference(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
        _themeFlow.value = theme
    }
}
