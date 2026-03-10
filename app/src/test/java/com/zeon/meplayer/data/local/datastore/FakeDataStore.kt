package com.zeon.meplayer.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDataStore : DataStore<Preferences> {
    private val _data = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = _data

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val newData = transform(_data.value)
        _data.value = newData
        return newData
    }
}