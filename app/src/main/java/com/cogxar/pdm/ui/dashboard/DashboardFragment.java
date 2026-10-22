package com.cogxar.pdm.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.cogxar.pdm.R;
import com.cogxar.pdm.data.model.DashboardResponse;
import com.cogxar.pdm.data.model.Device;
import com.cogxar.pdm.data.model.DeviceStatus;
import com.cogxar.pdm.data.model.FlaggedEvent;
import com.cogxar.pdm.data.model.Machine;
import com.cogxar.pdm.data.repository.PdmRepositoryFactory;
import com.cogxar.pdm.databinding.FragmentDashboardBinding;
import com.cogxar.pdm.debug.DebugVariantPicker;
import com.cogxar.pdm.ui.SharedMachineViewModel;
import com.cogxar.pdm.ui.dashboard.adapter.SensorAdapter;
import com.cogxar.pdm.util.TimeUtils;
import java.util.List;

/**
 * Main monitoring dashboard fragment.
 * Read-only telemetry display, lifecycle-aware polling, and strict single-banner priority.
 */
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private SensorAdapter sensorAdapter;
    private ArrayAdapter<Machine> machineSpinnerAdapter;
    private SharedMachineViewModel sharedMachineViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        sharedMachineViewModel = new ViewModelProvider(requireActivity()).get(SharedMachineViewModel.class);

        setupSensorsRecyclerView();
        setupMachineSelector();
        setupRefreshAndRetry();
        setupDebugVariantPicker();
        observeViewModel();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Start periodic polling when screen becomes visible
        viewModel.startPolling();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop polling to preserve battery and network when screen is not visible
        viewModel.stopPolling();
    }

    private void setupSensorsRecyclerView() {
        sensorAdapter = new SensorAdapter();
        binding.rvSensors.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvSensors.setAdapter(sensorAdapter);
    }

    private void setupMachineSelector() {
        machineSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line);
        binding.actvMachineDropdown.setAdapter(machineSpinnerAdapter);

        binding.actvMachineDropdown.setOnItemClickListener((parent, view, position, id) -> {
            Machine selected = machineSpinnerAdapter.getItem(position);
            if (selected != null) {
                viewModel.selectMachine(selected);
                sharedMachineViewModel.selectMachine(selected);
            }
        });
    }

    private void setupRefreshAndRetry() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.accent_brand);
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refresh());

        binding.layoutErrorState.btnRetry.setOnClickListener(v -> {
            binding.swipeRefreshLayout.setRefreshing(true);
            viewModel.refresh();
        });
    }

    private void setupDebugVariantPicker() {
        DebugVariantPicker.setup(
                requireContext(),
                binding.btnDebugVariant,
                PdmRepositoryFactory.getInstance(),
                () -> viewModel.refresh()
        );
    }

    private void observeViewModel() {
        // Observe machine list
        viewModel.getMachines().observe(getViewLifecycleOwner(), machines -> {
            if (machines != null) {
                machineSpinnerAdapter.clear();
                machineSpinnerAdapter.addAll(machines);
                machineSpinnerAdapter.notifyDataSetChanged();
            }
        });

        // Observe selected machine from dashboard
        viewModel.getSelectedMachine().observe(getViewLifecycleOwner(), machine -> {
            if (machine != null) {
                binding.actvMachineDropdown.setText(machine.getName(), false);
                sharedMachineViewModel.selectMachine(machine);
            }
        });

        // Observe selected machine from shared view model (e.g. changed in History)
        sharedMachineViewModel.getSelectedMachine().observe(getViewLifecycleOwner(), machine -> {
            if (machine != null) {
                Machine current = viewModel.getSelectedMachine().getValue();
                if (current == null || !machine.getId().equals(current.getId())) {
                    viewModel.selectMachine(machine);
                }
            }
        });

        // Observe UI state
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            binding.swipeRefreshLayout.setRefreshing(false);

            switch (state.getType()) {
                case LOADING:
                    showLoadingState();
                    break;
                case SUCCESS:
                    showSuccessState(state);
                    break;
                case EMPTY:
                    showEmptyState();
                    break;
                case ERROR:
                    showErrorState(state.getErrorMessage());
                    break;
            }
        });
    }

    private void showLoadingState() {
        binding.shimmerViewContainer.setVisibility(View.VISIBLE);
        binding.shimmerViewContainer.startShimmer();
        binding.nsvDashboardContent.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.layoutErrorState.layoutErrorRoot.setVisibility(View.GONE);
        binding.flBannerContainer.setVisibility(View.GONE);
        binding.tvConnectionIndicator.setVisibility(View.GONE);
    }

    private void showSuccessState(DashboardUiState state) {
        binding.shimmerViewContainer.stopShimmer();
        binding.shimmerViewContainer.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.layoutErrorState.layoutErrorRoot.setVisibility(View.GONE);
        binding.nsvDashboardContent.setVisibility(View.VISIBLE);

        DashboardResponse data = state.getData();
        if (data == null) return;

        // 1. Bind Sensors List
        sensorAdapter.submitList(data.getSensors());

        // 2. Bind "Last Reading" Card
        String formattedLastReading = TimeUtils.formatToLocalTime(data.getLastReadingTs());
        binding.tvLastReadingTimestamp.setText(formattedLastReading);

        // 3. Bind "Device Status" Card
        Device device = data.getDevice();
        DeviceStatus status = device != null ? device.getStatus() : DeviceStatus.UNKNOWN;
        bindDeviceStatus(status, device != null ? device.getLastSeen() : null);

        // 4. Bind "Latest Flagged Event" Card
        bindFlaggedEvent(data.getLatestFlag());

        // 5. Handle Connection Indicator & Banner Priority
        bindBannersAndConnection(state, status, device != null ? device.getLastSeen() : null, data.getLastReadingTs());
    }

    private void bindDeviceStatus(DeviceStatus status, String lastSeenIso) {
        int dotDrawable;
        int statusTextColor;
        String statusText;

        if (status == DeviceStatus.RUNNING) {
            dotDrawable = R.drawable.bg_status_dot_green;
            statusTextColor = ContextCompat.getColor(requireContext(), R.color.status_green);
            statusText = getString(R.string.status_running);
        } else if (status == DeviceStatus.STOPPED) {
            dotDrawable = R.drawable.bg_status_dot_amber;
            statusTextColor = ContextCompat.getColor(requireContext(), R.color.status_amber);
            statusText = getString(R.string.status_stopped);
        } else if (status == DeviceStatus.OFFLINE) {
            dotDrawable = R.drawable.bg_status_dot_red;
            statusTextColor = ContextCompat.getColor(requireContext(), R.color.status_red);
            statusText = getString(R.string.status_offline);
        } else {
            dotDrawable = R.drawable.bg_status_dot_amber;
            statusTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
            statusText = getString(R.string.status_unknown);
        }

        binding.viewStatusDot.setBackgroundResource(dotDrawable);
        binding.tvDeviceStatus.setTextColor(statusTextColor);
        binding.tvDeviceStatus.setText(statusText);

        String relativeLastSeen = TimeUtils.formatRelativeTime(lastSeenIso);
        binding.tvDeviceLastSeen.setText(getString(R.string.last_seen_prefix, relativeLastSeen));
    }

    private void bindFlaggedEvent(FlaggedEvent flag) {
        if (flag != null) {
            // Anomaly flagged
            binding.cardFlaggedEvent.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.status_red));
            binding.llAnomalyDetails.setVisibility(View.VISIBLE);
            binding.llNoAnomaly.setVisibility(View.GONE);

            binding.tvFlagMessage.setText(flag.getMessage());
            String sensorLabel = flag.getSensorName() != null ? flag.getSensorName()
                    : (flag.getSensorId() != null ? flag.getSensorId() : "Device Sensor");
            binding.tvFlagSensor.setText(getString(R.string.anomaly_sensor_label, sensorLabel));
            binding.tvFlagTimestamp.setText(TimeUtils.formatToLocalTime(flag.getTimestamp()));
        } else {
            // No anomaly
            binding.cardFlaggedEvent.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.border_card));
            binding.llAnomalyDetails.setVisibility(View.GONE);
            binding.llNoAnomaly.setVisibility(View.VISIBLE);
        }
    }

    private void bindBannersAndConnection(DashboardUiState state, DeviceStatus status, String lastSeenIso, String lastReadingTs) {
        // Priority 1: Server Unreachable / Reconnecting
        if (state.getConnectionStatus() == ConnectionStatus.RECONNECTING) {
            binding.tvConnectionIndicator.setText(R.string.connection_reconnecting);
            binding.tvConnectionIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_amber_dim));
            binding.tvConnectionIndicator.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_amber));
            binding.tvConnectionIndicator.setVisibility(View.VISIBLE);
        } else if (state.getConnectionStatus() == ConnectionStatus.SERVER_UNREACHABLE) {
            binding.tvConnectionIndicator.setText(R.string.connection_unreachable);
            binding.tvConnectionIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_red_dim));
            binding.tvConnectionIndicator.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
            binding.tvConnectionIndicator.setVisibility(View.VISIBLE);
        } else {
            binding.tvConnectionIndicator.setVisibility(View.GONE);
        }

        // Priority 2: Device Offline (Offline strictly supersedes Stale)
        if (status == DeviceStatus.OFFLINE) {
            binding.flBannerContainer.setVisibility(View.VISIBLE);
            binding.bannerOffline.setVisibility(View.VISIBLE);
            binding.bannerStale.setVisibility(View.GONE);

            String relativeLastSeen = TimeUtils.formatRelativeTime(lastSeenIso);
            binding.tvBannerOffline.setText(getString(R.string.banner_device_offline, relativeLastSeen));
            return;
        }

        // Priority 3: Stale Data (device is running/stopped, but reading is outdated)
        if (state.isStale()) {
            binding.flBannerContainer.setVisibility(View.VISIBLE);
            binding.bannerOffline.setVisibility(View.GONE);
            binding.bannerStale.setVisibility(View.VISIBLE);

            String relativeReadingTime = TimeUtils.formatRelativeTime(lastReadingTs);
            binding.tvBannerStale.setText(getString(R.string.banner_stale_data, relativeReadingTime));
            return;
        }

        // No warnings
        binding.flBannerContainer.setVisibility(View.GONE);
        binding.bannerOffline.setVisibility(View.GONE);
        binding.bannerStale.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        binding.shimmerViewContainer.stopShimmer();
        binding.shimmerViewContainer.setVisibility(View.GONE);
        binding.nsvDashboardContent.setVisibility(View.GONE);
        binding.layoutErrorState.layoutErrorRoot.setVisibility(View.GONE);
        binding.flBannerContainer.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void showErrorState(String errorMessage) {
        binding.shimmerViewContainer.stopShimmer();
        binding.shimmerViewContainer.setVisibility(View.GONE);
        binding.nsvDashboardContent.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.flBannerContainer.setVisibility(View.GONE);
        binding.layoutErrorState.layoutErrorRoot.setVisibility(View.VISIBLE);

        if (errorMessage != null && !errorMessage.isEmpty()) {
            binding.layoutErrorState.tvErrorMessage.setText(errorMessage);
        } else {
            binding.layoutErrorState.tvErrorMessage.setText(R.string.error_default_message);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
