package com.cogxar.pdm.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

/**
 * Hardware device details attached to the machine.
 */
public class Device {
    @SerializedName("status")
    @NonNull
    private final DeviceStatus status;

    @SerializedName("last_seen")
    @Nullable
    private final String lastSeen; // ISO-8601 UTC

    @SerializedName("ip_address")
    @Nullable
    private final String ipAddress; // Optional future field

    @SerializedName("firmware_version")
    @Nullable
    private final String firmwareVersion; // Optional future field

    public Device(@NonNull DeviceStatus status, @Nullable String lastSeen) {
        this(status, lastSeen, null, null);
    }

    public Device(@NonNull DeviceStatus status, @Nullable String lastSeen, @Nullable String ipAddress, @Nullable String firmwareVersion) {
        this.status = status != null ? status : DeviceStatus.UNKNOWN;
        this.lastSeen = lastSeen;
        this.ipAddress = ipAddress;
        this.firmwareVersion = firmwareVersion;
    }

    @NonNull
    public DeviceStatus getStatus() {
        return status;
    }

    @Nullable
    public String getLastSeen() {
        return lastSeen;
    }

    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    @Nullable
    public String getFirmwareVersion() {
        return firmwareVersion;
    }
}
