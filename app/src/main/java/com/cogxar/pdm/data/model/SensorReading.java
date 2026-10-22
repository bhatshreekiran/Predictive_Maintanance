package com.cogxar.pdm.data.model;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

/**
 * Reading from a specific sensor attached to a machine.
 */
public class SensorReading {
    @SerializedName("id")
    @NonNull
    private final String id;

    @SerializedName("name")
    @NonNull
    private final String name;

    @SerializedName("type")
    @NonNull
    private final String type;

    @SerializedName("value")
    private final double value;

    @SerializedName("unit")
    @NonNull
    private final String unit;

    @SerializedName("ts")
    @NonNull
    private final String timestamp;

    public SensorReading(@NonNull String id, @NonNull String name, @NonNull String type, double value, @NonNull String unit, @NonNull String timestamp) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    @NonNull
    public String getUnit() {
        return unit;
    }

    @NonNull
    public String getTimestamp() {
        return timestamp;
    }
}
