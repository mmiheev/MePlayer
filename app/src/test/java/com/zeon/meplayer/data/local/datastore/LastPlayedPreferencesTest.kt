package com.zeon.meplayer.data.local.datastore

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class LastPlayedPreferencesTest {

    private lateinit var preferences: LastPlayedPreferences
    private lateinit var fakeDataStore: FakeDataStore

    @Before
    fun setUp() {
        fakeDataStore = FakeDataStore()
        preferences = LastPlayedPreferences(fakeDataStore)
    }

    @Test
    fun `saveState and getLastPlayedState should return saved data`() = runTest {
        val state = LastPlayedState(1L, 5000L, true, false)
        preferences.saveState(state)
        val result = preferences.getLastPlayedState()
        assertEquals(state, result)
    }

    @Test
    fun `clearState should remove data`() = runTest {
        val state = LastPlayedState(1L, 5000L, true, false)
        preferences.saveState(state)
        preferences.clearState()
        val result = preferences.getLastPlayedState()
        assertNull(result)
    }
}