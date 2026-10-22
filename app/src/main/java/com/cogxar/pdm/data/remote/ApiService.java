package com.cogxar.pdm.data.remote;

import com.cogxar.pdm.data.model.DashboardResponse;
import com.cogxar.pdm.data.model.HistoricalReadingsResponse;
import com.cogxar.pdm.data.model.Machine;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit REST API interface for FastAPI backend.
 */
public interface ApiService {

    @GET("machines")
    Call<List<Machine>> getMachines();

    @GET("machines/{id}/dashboard")
    Call<DashboardResponse> getDashboard(@Path("id") String machineId);

    @GET("machines/{id}/readings")
    Call<HistoricalReadingsResponse> getReadings(
            @Path("id") String machineId,
            @Query("range") String range,
            @Query("limit") Integer limit,
            @Query("cursor") String cursor
    );
}
