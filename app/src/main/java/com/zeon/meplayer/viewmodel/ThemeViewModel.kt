package com.zeon.meplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeon.meplayer.ui.utils.theme.ThemeMode
import com.zeon.meplayer.ui.utils.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the application's theme preference (dark/light mode).
 * It holds a reactive [isDarkTheme] state that can be observed by the UI to apply
 * the appropriate theme dynamically.
 */
class ThemeViewModel(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.AUTO)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferences.themeModeFlow.collect { mode ->
                _themeMode.value = mode
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.saveThemeMode(mode)
            _themeMode.value = mode
        }
    }
}