package com.cogxar.pdm.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cogxar.pdm.data.mock.MockDataSource;
import com.cogxar.pdm.data.mock.MockVariant;
import com.cogxar.pdm.data.model.DashboardResponse;
import com.cogxar.pdm.data.model.DeviceStatus;
import com.cogxar.pdm.data.model.Machine;
import com.cogxar.pdm.data.repository.CancellableRequest;
import com.cogxar.pdm.data.repository.MockPdmRepository;
import com.cogxar.pdm.data.repository.RepositoryCallback;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;

/**
 * Pure Java Unit tests for MockPdmRepository without any Android runtime dependencies.
 */
public class MockPdmRepositoryTest {

    private MockPdmRepository repository;

    @Before
    public void setUp() {
        // Use synchronous Runnable::run executor for 100% deterministic test execution
        repository = new MockPdmRepository(new MockDataSource(), Runnable::run);
    }

    @Test
    public void testGetMachines_success() {
        AtomicReference<List<Machine>> resultRef = new AtomicReference<>();
        repository.getMachines(new RepositoryCallback<List<Machine>>() {
            @Override
            public void onSuccess(List<Machine> result) {
                resultRef.set(result);
            }

            @Override
            public void onError(Throwable throwable) {}
        });

        assertNotNull(resultRef.get());
        assertFalse(resultRef.get().isEmpty());
        assertEquals("pump-01", resultRef.get().get(0).getId());
    }

    @Test
    public void testGetDashboard_normalVariant() {
        repository.setVariant(MockVariant.NORMAL);
        AtomicReference<DashboardResponse> responseRef = new AtomicReference<>();

        repository.getDashboard("pump-01", new RepositoryCallback<DashboardResponse>() {
            @Override
            public void onSuccess(DashboardResponse result) {
                responseRef.set(result);
            }

            @Override
            public void onError(Throwable throwable) {}
        });

        DashboardResponse response = responseRef.get();
        assertNotNull(response);
        assertEquals(DeviceStatus.RUNNING, response.getDevice().getStatus());
        assertEquals(2, response.getSensors().size());
        assertNull("Normal variant must have no anomaly flag", response.getLatestFlag());
    }

    @Test
    public void testGetDashboard_anomalyVariant() {
        repository.setVariant(MockVariant.ANOMALY_FLAGGED);
        AtomicReference<DashboardResponse> responseRef = new AtomicReference<>();

        repository.getDashboard("pump-01", new RepositoryCallback<DashboardResponse>() {
            @Override
            public void onSuccess(DashboardResponse result) {
                responseRef.set(result);
            }

            @Override
            public void onError(Throwable throwable) {}
        });

        DashboardResponse response = responseRef.get();
        assertNotNull(response);
        assertNotNull("Anomaly variant must contain a flagged event", response.getLatestFlag());
        assertEquals("s2", response.getLatestFlag().getSensorId());
    }

    @Test
    public void testGetDashboard_offlineVariant() {
        repository.setVariant(MockVariant.DEVICE_OFFLINE);
        AtomicReference<DashboardResponse> responseRef = new AtomicReference<>();

        repository.getDashboard("pump-01", new RepositoryCallback<DashboardResponse>() {
            @Override
            public void onSuccess(DashboardResponse result) {
                responseRef.set(result);
            }

            @Override
            public void onError(Throwable throwable) {}
        });

        DashboardResponse response = responseRef.get();
        assertNotNull(response);
        assertEquals(DeviceStatus.OFFLINE, response.getDevice().getStatus());
    }

    @Test
    public void testGetDashboard_emptyVariant() {
        repository.setVariant(MockVariant.EMPTY);
        AtomicReference<DashboardResponse> responseRef = new AtomicReference<>();

        repository.getDashboard("pump-01", new RepositoryCallback<DashboardResponse>() {
            @Override
            public void onSuccess(DashboardResponse result) {
                responseRef.set(result);
            }

            @Override
            public void onError(Throwable throwable) {}
        });

        DashboardResponse response = responseRef.get();
        assertNotNull(response);
        assertTrue("Empty variant must have 0 sensors", response.getSensors().isEmpty());
    }

    @Test
    public void testGetDashboard_serverErrorVariant() {
        repository.setVariant(MockVariant.SERVER_ERROR);
        AtomicBoolean errorTriggered = new AtomicBoolean(false);

        repository.getDashboard("pump-01", new RepositoryCallback<DashboardResponse>() {
            @Override
            public void onSuccess(DashboardResponse result) {}

            @Override
            public void onError(Throwable throwable) {
                errorTriggered.set(true);
            }
        });

        assertTrue("Server error variant must invoke onError callback", errorTriggered.get());
    }

    @Test
    public void testCancellableRequest_abortsCallback() {
        // Test an asynchronous executor scenario with cancellation
        MockPdmRepository asyncRepo = new MockPdmRepository(new MockDataSource(), command -> {
            // Do not execute immediately to simulate in-flight delay
        });

        AtomicBoolean callbackTriggered = new AtomicBoolean(false);
        CancellableRequest handle = asyncRepo.getDashboard("pump-01", new RepositoryCallback<DashboardResponse>() {
            @Override
            public void onSuccess(DashboardResponse result) {
                callbackTriggered.set(true);
            }

            @Override
            public void onError(Throwable throwable) {
                callbackTriggered.set(true);
            }
        });

        handle.cancel();
        assertTrue(handle.isCanceled());
        assertFalse(callbackTriggered.get());
    }

    @Test
    public void testExportReadings_returnsError() {
        AtomicBoolean errorTriggered = new AtomicBoolean(false);
        repository.exportReadings("pump-01", "24h", "xlsx", new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {}

            @Override
            public void onError(Throwable throwable) {
                errorTriggered.set(true);
            }
        });

        assertTrue("Export must trigger error since server export is not available yet", errorTriggered.get());
    }
}
