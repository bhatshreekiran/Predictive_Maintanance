package com.cogxar.pdm.ui.dashboard;

import com.cogxar.pdm.data.model.DashboardResponse;

/**
 * Immutable UI State for the Dashboard screen.
 * Handles Loading, Success (with stale flag and connection health), Empty, and Error states.
 */
public class DashboardUiState {

    public enum Type {
        LOADING,
        SUCCESS,
        EMPTY,
        ERROR
    }

    private final Type type;
    private final DashboardResponse data;
    private final boolean isStale;
    private final ConnectionStatus connectionStatus;
    private final String errorMessage;
    private final String machineName;

    private DashboardUiState(
            Type type,
            DashboardResponse data,
            boolean isStale,
            ConnectionStatus connectionStatus,
            String errorMessage,
            String machineName
    ) {
        this.type = type;
        this.data = data;
        this.isStale = isStale;
        this.connectionStatus = connectionStatus != null ? connectionStatus : ConnectionStatus.CONNECTED;
        this.errorMessage = errorMessage;
        this.machineName = machineName;
    }

    public static DashboardUiState loading() {
        return new DashboardUiState(Type.LOADING, null, false, ConnectionStatus.CONNECTED, null, null);
    }

    public static DashboardUiState success(DashboardResponse data, boolean isStale, ConnectionStatus connectionStatus) {
        return new DashboardUiState(Type.SUCCESS, data, isStale, connectionStatus, null, null);
    }

    public static DashboardUiState empty(String machineName) {
        return new DashboardUiState(Type.EMPTY, null, false, ConnectionStatus.CONNECTED, null, machineName);
    }

    public static DashboardUiState error(String errorMessage) {
        return new DashboardUiState(Type.ERROR, null, false, ConnectionStatus.SERVER_UNREACHABLE, errorMessage, null);
    }

    public Type getType() {
        return type;
    }

    public DashboardResponse getData() {
        return data;
    }

    public boolean isStale() {
        return isStale;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMachineName() {
        return machineName;
    }
}
