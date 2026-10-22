package com.cogxar.pdm.ui.history

import com.cogxar.pdm.data.model.HistoricalReadingsResponse.HistoricalPoint

/**
 * UI State for the History Screen.
 */
sealed interface HistoryUiState {
    /**
     * Initial loading or transition between machines/ranges when no data is cached.
     */
    data object Loading : HistoryUiState

    /**
     * Successfully loaded historical telemetry.
     * @param readings Chronological list of historical data points.
     * @param range Active time range (1h, 24h, 7d).
     * @param isRefreshing True when background refresh is active while showing existing data.
     */
    data class Success(
        val readings: List<HistoricalPoint>,
        val range: String,
        val isRefreshing: Boolean = false
    ) : HistoryUiState

    /**
     * Machine has no readings in the requested time range.
     */
    data class Empty(
        val range: String
    ) : HistoryUiState

    /**
     * Error loading history (network failure, 503, etc.).
     */
    data class Error(
        val message: String
    ) : HistoryUiState
}
