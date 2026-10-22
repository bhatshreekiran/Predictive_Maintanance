package com.cogxar.pdm.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

/**
 * Top-level response for GET /machines/{id}/dashboard.
 */
public class DashboardResponse {
    @SerializedName("machine")
    @NonNull
    private final Machine machine;

    @SerializedName("device")
    @NonNull
    private final Device device;

    @SerializedName("last_reading_ts")
    @NonNull
    private final String lastReadingTs; // ISO-8601 UTC

    @SerializedName("sensors")
    @NonNull
    private final List<SensorReading> sensors;

    @SerializedName("latest_flag")
    @Nullable
    private final FlaggedEvent latestFlag; // Nullable if no anomaly

    @SerializedName("tag")
    @Nullable
    private final PhysicalAiTag tag; // Optional 4-bit Physical AI tag

    public DashboardResponse(
            @NonNull Machine machine,
            @NonNull Device device,
            @NonNull String lastReadingTs,
            @Nullable List<SensorReading> sensors,
            @Nullable FlaggedEvent latestFlag
    ) {
        this(machine, device, lastReadingTs, sensors, latestFlag, null);
    }

    public DashboardResponse(
            @NonNull Machine machine,
            @NonNull Device device,
            @NonNull String lastReadingTs,
            @Nullable List<SensorReading> sensors,
            @Nullable FlaggedEvent latestFlag,
            @Nullable PhysicalAiTag tag
    ) {
        this.machine = machine;
        this.device = device;
        this.lastReadingTs = lastReadingTs;
        this.sensors = sensors != null ? sensors : Collections.emptyList();
        this.latestFlag = latestFlag;
        this.tag = tag;
    }

    @NonNull
    public Machine getMachine() {
        return machine;
    }

    @NonNull
    public Device getDevice() {
        return device;
    }

    @NonNull
    public String getLastReadingTs() {
        return lastReadingTs;
    }

    @NonNull
    public List<SensorReading> getSensors() {
        return sensors;
    }

    @Nullable
    public FlaggedEvent getLatestFlag() {
        return latestFlag;
    }

    @Nullable
    public PhysicalAiTag getTag() {
        return tag;
    }
}
