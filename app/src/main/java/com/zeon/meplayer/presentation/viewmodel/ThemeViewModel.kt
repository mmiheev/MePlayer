package com.zeon.meplayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeon.meplayer.data.local.datastore.ThemePreferences
import com.zeon.meplayer.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the application's theme preference.
 * It holds a reactive [themeMode] state of type [ThemeMode] (LIGHT, DARK, AUTO),
 * which can be observed by the UI to apply the appropriate theme dynamically.
 *
 * The selected theme mode is persisted via [ThemePreferences] (DataStore) and
 * survives app restarts.
 */
class ThemeViewModel(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.AUTO)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferences.themeModeFlow.collect { mode ->
                _themeMode.value = mode
                _isLoading.value = false
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