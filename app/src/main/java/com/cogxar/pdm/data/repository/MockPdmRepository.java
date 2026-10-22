package com.cogxar.pdm.data.repository;

import com.cogxar.pdm.data.mock.MockDataSource;
import com.cogxar.pdm.data.mock.MockVariant;
import com.cogxar.pdm.data.model.DashboardResponse;
import com.cogxar.pdm.data.model.HistoricalReadingsResponse;
import com.cogxar.pdm.data.model.Machine;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mock implementation of PdmRepository.
 * Pure Java implementation with thread management via Executor.
 */
public class MockPdmRepository implements PdmRepository {

    private final MockDataSource mockDataSource;
    private final Executor executor;
    private final AtomicReference<MockVariant> currentVariant = new AtomicReference<>(MockVariant.NORMAL);

    public MockPdmRepository() {
        this(new MockDataSource(), Executors.newSingleThreadExecutor());
    }

    public MockPdmRepository(MockDataSource mockDataSource, Executor executor) {
        this.mockDataSource = mockDataSource;
        this.executor = executor;
    }

    public void setVariant(MockVariant variant) {
        if (variant != null) {
            currentVariant.set(variant);
        }
    }

    public MockVariant getVariant() {
        return currentVariant.get();
    }

    @Override
    public CancellableRequest getMachines(RepositoryCallback<List<Machine>> callback) {
        AtomicBoolean canceled = new AtomicBoolean(false);
        executor.execute(() -> {
            if (canceled.get()) return;
            try {
                if (currentVariant.get() == MockVariant.SERVER_ERROR) {
                    throw new IOException("503 Service Unavailable: Could not load machines");
                }
                List<Machine> machines = mockDataSource.getMachines();
                if (!canceled.get()) {
                    callback.onSuccess(machines);
                }
            } catch (Throwable t) {
                if (!canceled.get()) {
                    callback.onError(t);
                }
            }
        });
        return new CancellableRequest() {
            @Override
            public void cancel() {
                canceled.set(true);
            }

            @Override
            public boolean isCanceled() {
                return canceled.get();
            }
        };
    }

    @Override
    public CancellableRequest getDashboard(String machineId, RepositoryCallback<DashboardResponse> callback) {
        AtomicBoolean canceled = new AtomicBoolean(false);
        MockVariant variant = currentVariant.get();
        executor.execute(() -> {
            if (canceled.get()) return;
            try {
                DashboardResponse response = mockDataSource.getDashboard(machineId, variant);
                if (!canceled.get()) {
                    callback.onSuccess(response);
                }
            } catch (Throwable t) {
                if (!canceled.get()) {
                    callback.onError(t);
                }
            }
        });
        return new CancellableRequest() {
            @Override
            public void cancel() {
                canceled.set(true);
            }

            @Override
            public boolean isCanceled() {
                return canceled.get();
            }
        };
    }

    @Override
    public CancellableRequest getReadings(
            String machineId,
            String timeRange,
            String cursor,
            RepositoryCallback<HistoricalReadingsResponse> callback
    ) {
        AtomicBoolean canceled = new AtomicBoolean(false);
        executor.execute(() -> {
            if (canceled.get()) return;
            try {
                HistoricalReadingsResponse response = mockDataSource.getReadings(machineId, timeRange, cursor, currentVariant.get());
                if (!canceled.get()) {
                    callback.onSuccess(response);
                }
            } catch (Throwable t) {
                if (!canceled.get()) {
                    callback.onError(t);
                }
            }
        });
        return new CancellableRequest() {
            @Override
            public void cancel() {
                canceled.set(true);
            }

            @Override
            public boolean isCanceled() {
                return canceled.get();
            }
        };
    }

    /**
     * Future endpoint: GET /machines/{id}/export?range=1d&format=xlsx
     * Currently returns an error callback as server export is not connected yet.
     */
    @Override
    public CancellableRequest exportReadings(
            String machineId,
            String range,
            String format,
            RepositoryCallback<String> callback
    ) {
        AtomicBoolean canceled = new AtomicBoolean(false);
        executor.execute(() -> {
            if (canceled.get()) return;
            callback.onError(new UnsupportedOperationException("Server export not available yet"));
        });
        return new CancellableRequest() {
            @Override
            public void cancel() {
                canceled.set(true);
            }

            @Override
            public boolean isCanceled() {
                return canceled.get();
            }
        };
    }
}
