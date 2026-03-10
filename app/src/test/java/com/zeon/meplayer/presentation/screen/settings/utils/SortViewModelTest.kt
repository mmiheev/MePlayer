package com.zeon.meplayer.presentation.screen.settings.utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.zeon.meplayer.data.local.datastore.SortPreferences
import com.zeon.meplayer.domain.model.SortOption
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@ExperimentalCoroutinesApi
class SortViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: SortViewModel
    private lateinit var mockPreferences: SortPreferences
    private val sortOptionFlow = MutableStateFlow(SortOption.TITLE)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockPreferences = mockk(relaxed = true)
        every { mockPreferences.sortOptionFlow } returns sortOptionFlow.asStateFlow()
        viewModel = SortViewModel(mockPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `init collects sortOption from preferences and updates state`() = runTest {
        // Given (initial state is already set in setUp)
        // Then
        assertEquals(SortOption.TITLE, viewModel.sortOption.value)

        // When
        sortOptionFlow.value = SortOption.ARTIST

        // Then
        assertEquals(SortOption.ARTIST, viewModel.sortOption.value)
    }

    @Test
    fun `setSortOption saves to preferences and updates state`() = runTest {
        // Given
        coEvery { mockPreferences.saveSortOption(any()) } returns Unit

        // When
        viewModel.setSortOption(SortOption.DURATION)

        // Then
        assertEquals(SortOption.DURATION, viewModel.sortOption.value)
        coVerify(exactly = 1) { mockPreferences.saveSortOption(SortOption.DURATION) }
    }
}