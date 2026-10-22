package com.cogxar.pdm.ui.history

import app.cash.turbine.test
import com.cogxar.pdm.data.mock.MockDataSource
import com.cogxar.pdm.data.mock.MockVariant
import com.cogxar.pdm.data.model.Machine
import com.cogxar.pdm.data.repository.MockPdmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HistoryViewModel using kotlinx-coroutines-test and Turbine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: MockPdmRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = MockPdmRepository(MockDataSource(), Runnable::run)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoad_emitsLoadingThenSuccess() = runTest(testDispatcher) {
        val viewModel = HistoryViewModel(repository)
        viewModel.uiState.test {
            // Initial state is Loading
            assertEquals(HistoryUiState.Loading, awaitItem())

            // Advance dispatcher to execute loadMachines & loadReadings
            advanceUntilIdle()

            val success = awaitItem()
            assertTrue("Expected Success state, got $success", success is HistoryUiState.Success)
            val successState = success as HistoryUiState.Success
            assertEquals("24h", successState.range)
            assertEquals(96, successState.readings.size) // 24h mock returns 96 points
        }
    }

    @Test
    fun testEmptyVariant_emitsEmptyState() = runTest(testDispatcher) {
        repository.setVariant(MockVariant.EMPTY)
        val viewModel = HistoryViewModel(repository)

        viewModel.uiState.test {
            assertEquals(HistoryUiState.Loading, awaitItem())

            advanceUntilIdle()

            val empty = awaitItem()
            assertTrue("Expected Empty state, got $empty", empty is HistoryUiState.Empty)
            assertEquals("24h", (empty as HistoryUiState.Empty).range)
        }
    }

    @Test
    fun testErrorVariant_emitsErrorState() = runTest(testDispatcher) {
        repository.setVariant(MockVariant.SERVER_ERROR)
        val viewModel = HistoryViewModel(repository)

        viewModel.uiState.test {
            assertEquals(HistoryUiState.Loading, awaitItem())

            advanceUntilIdle()

            val error = awaitItem()
            assertTrue("Expected Error state, got $error", error is HistoryUiState.Error)
            assertTrue((error as HistoryUiState.Error).message.contains("503"))
        }
    }

    @Test
    fun testRangeSwitch_loadsNewRangePoints() = runTest(testDispatcher) {
        val viewModel = HistoryViewModel(repository)

        viewModel.uiState.test {
            // Initial load
            assertEquals(HistoryUiState.Loading, awaitItem())
            advanceUntilIdle()

            val state24h = awaitItem() as HistoryUiState.Success
            assertEquals("24h", state24h.range)
            assertEquals(96, state24h.readings.size)

            // Switch to 1h
            viewModel.setRange("1h")
            assertEquals(HistoryUiState.Loading, awaitItem())
            advanceUntilIdle()

            val state1h = awaitItem() as HistoryUiState.Success
            assertEquals("1h", state1h.range)
            assertEquals(60, state1h.readings.size) // 1h mock returns 60 points

            // Switch to 7d
            viewModel.setRange("7d")
            assertEquals(HistoryUiState.Loading, awaitItem())
            advanceUntilIdle()

            val state7d = awaitItem() as HistoryUiState.Success
            assertEquals("7d", state7d.range)
            assertEquals(84, state7d.readings.size) // 7d mock returns 84 points
        }
    }

    @Test
    fun testStaleResults_areIgnoredWhenMachineChangesQuickly() = runTest(testDispatcher) {
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        val machineA = Machine("pump-01", "Pump 01")
        val machineB = Machine("compressor-02", "Compressor 02")

        viewModel.setMachine(machineA)
        viewModel.setMachine(machineB)
        advanceUntilIdle()

        assertEquals("compressor-02", viewModel.selectedMachine.value?.id)
        val state = viewModel.uiState.value
        assertTrue("State must be Success for the latest machine", state is HistoryUiState.Success)
    }

    @Test
    fun testMachineSwitch_reloadsAndUpdatesSelectedMachine() = runTest(testDispatcher) {
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        val compressor = Machine("compressor-02", "Compressor 02")
        viewModel.setMachine(compressor)

        viewModel.uiState.test {
            // Emits Loading on machine change, then Success
            val state = awaitItem()
            assertTrue("Expected Loading or Success state", state is HistoryUiState.Loading || state is HistoryUiState.Success)
            advanceUntilIdle()
            val finalState = awaitItem()
            assertTrue("Expected Success state for compressor", finalState is HistoryUiState.Success)
        }
        assertEquals("compressor-02", viewModel.selectedMachine.value?.id)
    }
}
