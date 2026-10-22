package com.cogxar.pdm.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Anomaly event flagged directly on the device.
 * The mobile app only displays this flag and does not perform anomaly analysis itself.
 */
public class FlaggedEvent {
    @SerializedName("sensor_id")
    @NonNull
    private final String sensorId;

    @SerializedName("sensor_name")
    @Nullable
    private final String sensorName; // Optional display helper from backend

    @SerializedName("message")
    @NonNull
    private final String message;

    @SerializedName("ts")
    @NonNull
    private final String timestamp;

    public FlaggedEvent(@NonNull String sensorId, @NonNull String message, @NonNull String timestamp) {
        this(sensorId, null, message, timestamp);
    }

    public FlaggedEvent(@NonNull String sensorId, @Nullable String sensorName, @NonNull String message, @NonNull String timestamp) {
        this.sensorId = sensorId;
        this.sensorName = sensorName;
        this.message = message;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getSensorId() {
        return sensorId;
    }

    @Nullable
    public String getSensorName() {
        return sensorName;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @NonNull
    public String getTimestamp() {
        return timestamp;
    }
}
