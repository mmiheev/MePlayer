package com.zeon.meplayer.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

/**
 * ViewModel responsible for managing the application's theme preference (dark/light mode).
 * It holds a reactive [isDarkTheme] state that can be observed by the UI to apply
 * the appropriate theme dynamically.
 */
class ThemeViewModel : ViewModel() {

    private val _isDarkTheme = mutableStateOf(false)
    val isDarkTheme: State<Boolean> = _isDarkTheme

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }
}