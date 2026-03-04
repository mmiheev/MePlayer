package com.zeon.meplayer.presentation.screen.settings.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeon.meplayer.data.local.datastore.SortPreferences
import com.zeon.meplayer.domain.model.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SortViewModel(private val sortPreferences: SortPreferences) : ViewModel() {
    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption

    init {
        viewModelScope.launch {
            sortPreferences.sortOptionFlow.collect { _sortOption.value = it }
        }
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch {
            sortPreferences.saveSortOption(option)
            _sortOption.value = option
        }
    }
}