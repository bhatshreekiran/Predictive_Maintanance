package com.cogxar.pdm.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.cogxar.pdm.BuildConfig;
import com.cogxar.pdm.data.model.DashboardResponse;
import com.cogxar.pdm.data.model.Machine;
import com.cogxar.pdm.data.repository.CancellableRequest;
import com.cogxar.pdm.data.repository.PdmRepository;
import com.cogxar.pdm.data.repository.PdmRepositoryFactory;
import com.cogxar.pdm.data.repository.RepositoryCallback;
import com.cogxar.pdm.util.TimeUtils;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * ViewModel managing dashboard state, machine switching with in-flight cancellation,
 * and periodic background polling.
 */
public class DashboardViewModel extends ViewModel {

    private final PdmRepository repository;
    private final ScheduledExecutorService pollerExecutor;

    private final MutableLiveData<DashboardUiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<List<Machine>> machines = new MutableLiveData<>();
    private final MutableLiveData<Machine> selectedMachine = new MutableLiveData<>();

    private CancellableRequest activeDashboardRequest;
    private CancellableRequest activeMachinesRequest;
    private ScheduledFuture<?> pollingTask;

    private String currentSelectedMachineId;
    private boolean isPollingActive = false;

    public DashboardViewModel() {
        this(PdmRepositoryFactory.getInstance(), Executors.newSingleThreadScheduledExecutor());
    }

    public DashboardViewModel(PdmRepository repository, ScheduledExecutorService pollerExecutor) {
        this.repository = repository;
        this.pollerExecutor = pollerExecutor;
        loadMachines();
    }

    public LiveData<DashboardUiState> getUiState() {
        return uiState;
    }

    public LiveData<List<Machine>> getMachines() {
        return machines;
    }

    public LiveData<Machine> getSelectedMachine() {
        return selectedMachine;
    }

    /**
     * Fetches the machine list. Automatically selects the first machine once loaded.
     */
    public void loadMachines() {
        if (activeMachinesRequest != null) {
            activeMachinesRequest.cancel();
        }

        activeMachinesRequest = repository.getMachines(new RepositoryCallback<List<Machine>>() {
            @Override
            public void onSuccess(List<Machine> result) {
                machines.postValue(result);
                if (result != null && !result.isEmpty() && currentSelectedMachineId == null) {
                    selectMachine(result.get(0));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                uiState.postValue(DashboardUiState.error("Failed to load machines: " + throwable.getMessage()));
            }
        });
    }

    /**
     * User selected a machine from the dropdown.
     * Cancels any in-flight request, stops existing polling, and starts fresh polling.
     */
    public void selectMachine(Machine machine) {
        if (machine == null || machine.getId() == null) return;
        if (machine.getId().equals(currentSelectedMachineId) && uiState.getValue() != null
                && uiState.getValue().getType() == DashboardUiState.Type.SUCCESS) {
            return;
        }

        currentSelectedMachineId = machine.getId();
        selectedMachine.postValue(machine);

        // Cancel previous in-flight request
        if (activeDashboardRequest != null) {
            activeDashboardRequest.cancel();
            activeDashboardRequest = null;
        }

        // Emit loading state for the new machine
        uiState.postValue(DashboardUiState.loading());

        // Perform immediate fetch
        fetchDashboardTelemetry(currentSelectedMachineId, false);

        // Reset polling task for the new machine if polling is active
        if (isPollingActive) {
            restartPollingTimer();
        }
    }

    /**
     * Starts periodic polling. Called from Fragment onStart().
     */
    public void startPolling() {
        isPollingActive = true;
        if (currentSelectedMachineId != null) {
            // If we have an existing state, refresh immediately or resume polling
            fetchDashboardTelemetry(currentSelectedMachineId, false);
            restartPollingTimer();
        }
    }

    /**
     * Stops periodic polling. Called from Fragment onStop().
     */
    public void stopPolling() {
        isPollingActive = false;
        if (pollingTask != null) {
            pollingTask.cancel(true);
            pollingTask = null;
        }
        if (activeDashboardRequest != null) {
            activeDashboardRequest.cancel();
            activeDashboardRequest = null;
        }
    }

    /**
     * Manual refresh triggered by pull-to-refresh or retry button.
     */
    public void refresh() {
        if (currentSelectedMachineId != null) {
            fetchDashboardTelemetry(currentSelectedMachineId, true);
        } else {
            loadMachines();
        }
    }

    private void restartPollingTimer() {
        if (pollingTask != null) {
            pollingTask.cancel(true);
        }
        pollingTask = pollerExecutor.scheduleWithFixedDelay(
                () -> {
                    if (isPollingActive && currentSelectedMachineId != null) {
                        fetchDashboardTelemetry(currentSelectedMachineId, false);
                    }
                },
                BuildConfig.POLL_INTERVAL_MS,
                BuildConfig.POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void fetchDashboardTelemetry(String requestedMachineId, boolean isManualRefresh) {
        if (requestedMachineId == null) return;

        // Cancel existing request before starting a new one
        if (activeDashboardRequest != null && !activeDashboardRequest.isCanceled()) {
            activeDashboardRequest.cancel();
        }

        activeDashboardRequest = repository.getDashboard(requestedMachineId, new RepositoryCallback<DashboardResponse>() {
            @Override
            public void onSuccess(DashboardResponse response) {
                // Ensure response corresponds to the currently selected machine
                if (!requestedMachineId.equals(currentSelectedMachineId)) {
                    // Stale response from previously selected machine; discard
                    return;
                }

                if (response.getSensors() == null || response.getSensors().isEmpty()) {
                    String mName = response.getMachine() != null ? response.getMachine().getName() : requestedMachineId;
                    uiState.postValue(DashboardUiState.empty(mName));
                    return;
                }

                // Check staleness against configured threshold
                boolean isStale = TimeUtils.isStale(
                        response.getLastReadingTs(),
                        BuildConfig.STALE_THRESHOLD_SEC,
                        System.currentTimeMillis()
                );

                uiState.postValue(DashboardUiState.success(response, isStale, ConnectionStatus.CONNECTED));
            }

            @Override
            public void onError(Throwable throwable) {
                if (!requestedMachineId.equals(currentSelectedMachineId)) {
                    return;
                }

                DashboardUiState currentState = uiState.getValue();
                if (currentState != null && currentState.getType() == DashboardUiState.Type.SUCCESS) {
                    // Keep last known values on screen, but indicate reconnecting / unreachable
                    uiState.postValue(DashboardUiState.success(
                            currentState.getData(),
                            true,
                            ConnectionStatus.RECONNECTING
                    ));
                } else if (!isPollingActive || isManualRefresh || currentState == null || currentState.getType() == DashboardUiState.Type.LOADING) {
                    uiState.postValue(DashboardUiState.error(throwable.getMessage()));
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPolling();
        pollerExecutor.shutdown();
    }
}
