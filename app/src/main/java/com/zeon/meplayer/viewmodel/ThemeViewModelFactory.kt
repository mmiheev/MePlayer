package com.zeon.meplayer.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeon.meplayer.ui.utils.theme.ThemePreferences

@Composable
fun rememberThemeViewModel(): ThemeViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ThemeViewModel(ThemePreferences(context)) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )
}