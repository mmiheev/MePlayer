package com.zeon.meplayer.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.zeon.meplayer.data.local.datastore.ThemePreferences
import com.zeon.meplayer.presentation.theme.ThemeMode
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ThemeViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: ThemeViewModel
    private lateinit var mockPreferences: ThemePreferences
    private val themeModeFlow = MutableStateFlow(ThemeMode.AUTO)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockPreferences = mockk(relaxed = true)
        every { mockPreferences.themeModeFlow } returns themeModeFlow.asStateFlow()
        viewModel = ThemeViewModel(mockPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `init collects themeMode from preferences and updates state`() = runTest {
        assertEquals(ThemeMode.AUTO, viewModel.themeMode.value)

        themeModeFlow.value = ThemeMode.DARK

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `setThemeMode saves to preferences and updates state`() = runTest {
        coEvery { mockPreferences.saveThemeMode(any()) } returns Unit

        viewModel.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)

        coVerify(exactly = 1) { mockPreferences.saveThemeMode(ThemeMode.LIGHT) }
    }

}