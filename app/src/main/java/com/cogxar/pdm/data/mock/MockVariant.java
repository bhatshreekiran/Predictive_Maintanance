package com.cogxar.pdm.data.mock;

/**
 * Switchable mock scenarios for industrial edge-case verification.
 */
public enum MockVariant {
    NORMAL,
    ANOMALY_FLAGGED,
    DEVICE_OFFLINE,
    STALE_DATA,
    EMPTY,
    SERVER_ERROR
}
