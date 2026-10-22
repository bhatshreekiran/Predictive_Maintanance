package com.cogxar.pdm.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.cogxar.pdm.data.mock.MockDataSource;
import com.cogxar.pdm.data.mock.MockVariant;
import com.cogxar.pdm.data.model.DashboardResponse;
import com.cogxar.pdm.data.model.DeviceStatus;
import com.cogxar.pdm.data.model.Machine;
import com.cogxar.pdm.data.repository.MockPdmRepository;
import com.cogxar.pdm.ui.dashboard.DashboardUiState;
import com.cogxar.pdm.ui.dashboard.DashboardViewModel;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit tests for DashboardViewModel using InstantTaskExecutorRule for synchronous LiveData evaluation.
 */
public class DashboardViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MockPdmRepository repository;
    private DashboardViewModel viewModel;

    @Before
    public void setUp() {
        repository = new MockPdmRepository(new MockDataSource(), Runnable::run);
        viewModel = new DashboardViewModel(repository, Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    public void testInitialLoad_selectsFirstMachineAndEmitsSuccess() {
        assertNotNull(viewModel.getMachines().getValue());
        assertFalse(viewModel.getMachines().getValue().isEmpty());

        Machine selected = viewModel.getSelectedMachine().getValue();
        assertNotNull(selected);
        assertEquals("pump-01", selected.getId());

        DashboardUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertEquals(DashboardUiState.Type.SUCCESS, state.getType());
        assertFalse("Normal variant should not be stale", state.isStale());
        assertEquals(2, state.getData().getSensors().size());
    }

    @Test
    public void testSelectMachine_updatesSelectedMachineAndFetchesData() {
        Machine compressor = new Machine("compressor-02", "Compressor 02");
        viewModel.selectMachine(compressor);

        assertEquals("compressor-02", viewModel.getSelectedMachine().getValue().getId());
        DashboardUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertEquals(DashboardUiState.Type.SUCCESS, state.getType());
        assertEquals("compressor-02", state.getData().getMachine().getId());
    }

    @Test
    public void testOfflineVariant_emitsOfflineDevice() {
        repository.setVariant(MockVariant.DEVICE_OFFLINE);
        viewModel.refresh();

        DashboardUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertEquals(DashboardUiState.Type.SUCCESS, state.getType());
        assertEquals(DeviceStatus.OFFLINE, state.getData().getDevice().getStatus());
    }

    @Test
    public void testStaleVariant_emitsStaleFlagTrue() {
        repository.setVariant(MockVariant.STALE_DATA);
        viewModel.refresh();

        DashboardUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertEquals(DashboardUiState.Type.SUCCESS, state.getType());
        assertTrue("Stale variant should flag isStale as true", state.isStale());
    }

    @Test
    public void testEmptyVariant_emitsEmptyState() {
        repository.setVariant(MockVariant.EMPTY);
        viewModel.refresh();

        DashboardUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertEquals(DashboardUiState.Type.EMPTY, state.getType());
    }
}
