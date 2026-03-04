package com.zeon.meplayer.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeon.meplayer.data.local.datastore.SortPreferences
import com.zeon.meplayer.data.local.datastore.ThemePreferences
import com.zeon.meplayer.presentation.screen.settings.utils.SortViewModel

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

@Composable
fun rememberSortViewModel(): SortViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SortViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return SortViewModel(SortPreferences(context)) as T
                }
                throw IllegalArgumentException()
            }
        }
    )
}