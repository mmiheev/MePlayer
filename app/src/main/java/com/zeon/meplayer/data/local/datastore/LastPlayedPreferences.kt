package com.zeon.meplayer.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zeon.meplayer.core.playback.LastPlayedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "last_played")

class LastPlayedPreferences(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.dataStore)

    companion object {
        private val SONG_ID_KEY = longPreferencesKey("song_id")
        private val POSITION_KEY = longPreferencesKey("position")
        private val SHUFFLE_ENABLED_KEY = booleanPreferencesKey("shuffle_enabled")
        private val IS_MUTED_KEY = booleanPreferencesKey("is_muted")
    }

    val lastPlayedStateFlow: Flow<LastPlayedState?> = dataStore.data
        .map { preferences ->
            val songId = preferences[SONG_ID_KEY] ?: return@map null
            val position = preferences[POSITION_KEY] ?: 0L
            val shuffleEnabled = preferences[SHUFFLE_ENABLED_KEY] ?: false
            val isMuted = preferences[IS_MUTED_KEY] ?: false
            LastPlayedState(songId, position, shuffleEnabled, isMuted)
        }

    suspend fun saveState(state: LastPlayedState) {
        dataStore.edit { preferences ->
            preferences[SONG_ID_KEY] = state.songId
            preferences[POSITION_KEY] = state.position
            preferences[SHUFFLE_ENABLED_KEY] = state.shuffleEnabled
            preferences[IS_MUTED_KEY] = state.isMuted
        }
    }

    suspend fun clearState() {
        dataStore.edit { preferences ->
            preferences.remove(SONG_ID_KEY)
            preferences.remove(POSITION_KEY)
            preferences.remove(SHUFFLE_ENABLED_KEY)
            preferences.remove(IS_MUTED_KEY)
        }
    }

    suspend fun getLastPlayedState(): LastPlayedState? = lastPlayedStateFlow.firstOrNull()
}