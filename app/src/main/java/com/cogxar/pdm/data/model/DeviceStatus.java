package com.cogxar.pdm.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Status of the telemetry hardware device (RP2040-ETH).
 */
public enum DeviceStatus {
    @SerializedName("running")
    RUNNING("running"),

    @SerializedName("stopped")
    STOPPED("stopped"),

    @SerializedName("offline")
    OFFLINE("offline"),

    @SerializedName("unknown")
    UNKNOWN("unknown");

    private final String value;

    DeviceStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DeviceStatus fromString(String text) {
        if (text == null) {
            return UNKNOWN;
        }
        for (DeviceStatus status : DeviceStatus.values()) {
            if (status.value.equalsIgnoreCase(text)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
