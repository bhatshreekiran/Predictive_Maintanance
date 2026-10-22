package com.cogxar.pdm.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cogxar.pdm.data.model.Machine
import com.cogxar.pdm.data.repository.PdmRepository
import com.cogxar.pdm.data.repository.PdmRepositoryFactory
import com.cogxar.pdm.util.getMachinesSuspend
import com.cogxar.pdm.util.getReadingsSuspend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the History screen.
 * Exposes StateFlow<HistoryUiState> and handles machine and range selections,
 * in-flight request cancellation, and stale result filtering.
 */
class HistoryViewModel(
    private val repository: PdmRepository = PdmRepositoryFactory.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _machines = MutableStateFlow<List<Machine>>(emptyList())
    val machines: StateFlow<List<Machine>> = _machines.asStateFlow()

    private val _selectedMachine = MutableStateFlow<Machine?>(null)
    val selectedMachine: StateFlow<Machine?> = _selectedMachine.asStateFlow()

    private val _selectedRange = MutableStateFlow("24h")
    val selectedRange: StateFlow<String> = _selectedRange.asStateFlow()

    private var fetchJob: Job? = null

    init {
        loadMachines()
    }

    fun loadMachines() {
        viewModelScope.launch {
            try {
                val list = repository.getMachinesSuspend()
                _machines.value = list
                if (_selectedMachine.value == null && list.isNotEmpty()) {
                    setMachine(list.first())
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _uiState.value = HistoryUiState.Error(e.message ?: "Failed to load machines")
                }
            }
        }
    }

    /**
     * Updates target machine. Cancels previous fetch and triggers a new load.
     */
    fun setMachine(machine: Machine) {
        if (_selectedMachine.value?.id == machine.id && _uiState.value is HistoryUiState.Success) {
            return
        }
        _selectedMachine.value = machine
        loadReadings(isRefresh = false)
    }

    /**
     * Updates target range ("1h", "24h", "7d"). Cancels previous fetch and triggers a new load.
     */
    fun setRange(range: String) {
        if (_selectedRange.value == range && _uiState.value is HistoryUiState.Success) {
            return
        }
        _selectedRange.value = range
        loadReadings(isRefresh = false)
    }

    /**
     * Pull-to-refresh or retry. Keeps existing data on screen if available.
     */
    fun refresh() {
        loadReadings(isRefresh = true)
    }

    fun retry() {
        if (_machines.value.isEmpty()) {
            loadMachines()
        } else {
            loadReadings(isRefresh = false)
        }
    }

    private fun loadReadings(isRefresh: Boolean) {
        val machine = _selectedMachine.value ?: return
        val range = _selectedRange.value
        val machineId = machine.id

        // Cancel previous in-flight job immediately
        fetchJob?.cancel()

        val currentState = _uiState.value
        if (isRefresh && currentState is HistoryUiState.Success) {
            _uiState.value = currentState.copy(isRefreshing = true)
        } else {
            _uiState.value = HistoryUiState.Loading
        }

        fetchJob = viewModelScope.launch {
            try {
                val response = repository.getReadingsSuspend(machineId, range, null)

                // Stale response check: discard if machine or range changed in the meantime
                if (_selectedMachine.value?.id != machineId || _selectedRange.value != range) {
                    return@launch
                }

                val readings = response.readings
                if (readings.isEmpty()) {
                    _uiState.value = HistoryUiState.Empty(range)
                } else {
                    _uiState.value = HistoryUiState.Success(
                        readings = readings,
                        range = range,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Do not surface cancellation as an error
                    return@launch
                }
                if (_selectedMachine.value?.id == machineId && _selectedRange.value == range) {
                    _uiState.value = HistoryUiState.Error(e.message ?: "Failed to load historical data")
                }
            }
        }
    }
}
