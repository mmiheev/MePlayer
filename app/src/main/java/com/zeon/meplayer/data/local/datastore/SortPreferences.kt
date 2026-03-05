package com.zeon.meplayer.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zeon.meplayer.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sort_settings")

class SortPreferences(private val context: Context) {
    companion object {
        private val SORT_OPTION_KEY = stringPreferencesKey("sort_option")
    }

    val sortOptionFlow: Flow<SortOption> = context.dataStore.data
        .map { preferences ->
            val name = preferences[SORT_OPTION_KEY] ?: SortOption.TITLE.name
            SortOption.valueOf(name)
        }

    suspend fun saveSortOption(option: SortOption) {
        context.dataStore.edit { it[SORT_OPTION_KEY] = option.name }
    }
}