package com.cogxar.pdm.data.repository;

import com.cogxar.pdm.data.model.DashboardResponse;
import com.cogxar.pdm.data.model.HistoricalReadingsResponse;
import com.cogxar.pdm.data.model.Machine;
import com.cogxar.pdm.data.remote.ApiService;
import java.io.IOException;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Live API implementation of PdmRepository backed by Retrofit.
 */
public class RemotePdmRepository implements PdmRepository {

    private final ApiService apiService;

    public RemotePdmRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public CancellableRequest getMachines(RepositoryCallback<List<Machine>> callback) {
        Call<List<Machine>> call = apiService.getMachines();
        call.enqueue(new Callback<List<Machine>>() {
            @Override
            public void onResponse(Call<List<Machine>> call, Response<List<Machine>> response) {
                if (call.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new IOException("HTTP " + response.code() + ": Failed to fetch machines"));
                }
            }

            @Override
            public void onFailure(Call<List<Machine>> call, Throwable t) {
                if (call.isCanceled()) {
                    // Canceled intentionally on machine change or lifecycle stop; do not trigger error
                    return;
                }
                callback.onError(t);
            }
        });
        return toCancellable(call);
    }

    @Override
    public CancellableRequest getDashboard(String machineId, RepositoryCallback<DashboardResponse> callback) {
        Call<DashboardResponse> call = apiService.getDashboard(machineId);
        call.enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                if (call.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new IOException("HTTP " + response.code() + ": Failed to fetch dashboard"));
                }
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                if (call.isCanceled()) {
                    // In-flight call was canceled; ignore
                    return;
                }
                callback.onError(t);
            }
        });
        return toCancellable(call);
    }

    @Override
    public CancellableRequest getReadings(
            String machineId,
            String timeRange,
            String cursor,
            RepositoryCallback<HistoricalReadingsResponse> callback
    ) {
        Call<HistoricalReadingsResponse> call = apiService.getReadings(machineId, timeRange, null, cursor);
        call.enqueue(new Callback<HistoricalReadingsResponse>() {
            @Override
            public void onResponse(Call<HistoricalReadingsResponse> call, Response<HistoricalReadingsResponse> response) {
                if (call.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new IOException("HTTP " + response.code() + ": Failed to fetch readings"));
                }
            }

            @Override
            public void onFailure(Call<HistoricalReadingsResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                callback.onError(t);
            }
        });
        return toCancellable(call);
    }

    /**
     * Future endpoint: GET /machines/{id}/export?range=1d&format=xlsx
     * Currently returns an error callback as live server export endpoint is not connected yet.
     */
    @Override
    public CancellableRequest exportReadings(
            String machineId,
            String range,
            String format,
            RepositoryCallback<String> callback
    ) {
        callback.onError(new UnsupportedOperationException("GET /machines/" + machineId + "/export?range=" + range + "&format=" + format + " not implemented"));
        return new CancellableRequest() {
            private boolean canceled = false;

            @Override
            public void cancel() {
                canceled = true;
            }

            @Override
            public boolean isCanceled() {
                return canceled;
            }
        };
    }

    private <T> CancellableRequest toCancellable(Call<T> call) {
        return new CancellableRequest() {
            @Override
            public void cancel() {
                call.cancel();
            }

            @Override
            public boolean isCanceled() {
                return call.isCanceled();
            }
        };
    }
}
