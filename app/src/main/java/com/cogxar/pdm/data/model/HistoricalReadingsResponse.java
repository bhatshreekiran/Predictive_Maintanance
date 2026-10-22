package com.cogxar.pdm.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

/**
 * Model prepared for Phase 2 Historical Readings (GET /machines/{id}/readings).
 */
public class HistoricalReadingsResponse {

    public static class HistoricalPoint {
        @SerializedName("ts")
        @NonNull
        private final String timestamp;

        @SerializedName("temperature_c")
        private final double temperatureC;

        @SerializedName("vibration_rms")
        private final double vibrationRms;

        @SerializedName("tag")
        @Nullable
        private final PhysicalAiTag tag;

        @SerializedName("anomaly")
        private final boolean anomaly;

        public HistoricalPoint(
                @NonNull String timestamp,
                double temperatureC,
                double vibrationRms,
                @Nullable PhysicalAiTag tag,
                boolean anomaly
        ) {
            this.timestamp = timestamp;
            this.temperatureC = temperatureC;
            this.vibrationRms = vibrationRms;
            this.tag = tag;
            this.anomaly = anomaly;
        }

        @NonNull
        public String getTimestamp() {
            return timestamp;
        }

        public double getTemperatureC() {
            return temperatureC;
        }

        public double getVibrationRms() {
            return vibrationRms;
        }

        @Nullable
        public PhysicalAiTag getTag() {
            return tag;
        }

        @Nullable
        public Integer getTagValue() {
            return tag != null ? tag.getValue() : null;
        }

        public boolean isAnomaly() {
            return anomaly;
        }
    }

    @SerializedName("readings")
    @NonNull
    private final List<HistoricalPoint> readings;

    @SerializedName("next_cursor")
    @Nullable
    private final String nextCursor;

    public HistoricalReadingsResponse(@Nullable List<HistoricalPoint> readings, @Nullable String nextCursor) {
        this.readings = readings != null ? readings : Collections.emptyList();
        this.nextCursor = nextCursor;
    }

    @NonNull
    public List<HistoricalPoint> getReadings() {
        return readings;
    }

    @Nullable
    public String getNextCursor() {
        return nextCursor;
    }
}
