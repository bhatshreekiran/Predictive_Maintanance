package com.cogxar.pdm.data.repository;

import com.cogxar.pdm.data.model.DashboardResponse;
import com.cogxar.pdm.data.model.HistoricalReadingsResponse;
import com.cogxar.pdm.data.model.Machine;
import java.util.List;

/**
 * Single data entry point contract for predictive maintenance monitoring.
 * Pure Java interface with no Android framework dependencies.
 */
public interface PdmRepository {

    /**
     * Retrieves the list of monitored machines.
     */
    CancellableRequest getMachines(RepositoryCallback<List<Machine>> callback);

    /**
     * Retrieves the latest dashboard telemetry for the specified machine.
     *
     * @param machineId ID of the target machine (e.g. "pump-01")
     * @param callback  Result callback
     */
    CancellableRequest getDashboard(String machineId, RepositoryCallback<DashboardResponse> callback);

    /**
     * Prepared for Phase 2: Retrieves historical sensor trends and anomaly markers.
     *
     * @param machineId ID of the target machine
     * @param timeRange e.g. "1h", "24h", "7d"
     * @param cursor    Pagination cursor or null
     * @param callback  Result callback
     */
    CancellableRequest getReadings(
            String machineId,
            String timeRange,
            String cursor,
            RepositoryCallback<HistoricalReadingsResponse> callback
    );

    /**
     * Future endpoint: GET /machines/{id}/export?range=1d&format=xlsx
     * Requests an export file generation or download URL from the server.
     *
     * @param machineId ID of the target machine
     * @param range     e.g. "1h", "24h", "7d"
     * @param format    e.g. "xlsx", "csv"
     * @param callback  Result callback
     */
    CancellableRequest exportReadings(
            String machineId,
            String range,
            String format,
            RepositoryCallback<String> callback
    );
}
