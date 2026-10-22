package com.cogxar.pdm.util

import com.cogxar.pdm.data.model.HistoricalReadingsResponse.HistoricalPoint
import kotlin.math.max

/**
 * Utility functions for historical telemetry processing and charting.
 * Independent of Android/MPAndroidChart UI classes for testability in standard JVM unit tests.
 */
object ChartUtils {

    /**
     * Downsamples a list of historical data points to at most [maxPoints] while guaranteeing
     * that EVERY anomaly point is preserved.
     *
     * @param points Chronological list of points.
     * @param maxPoints Maximum target size (default 200).
     * @return Ordered downsampled list containing all anomalies and sampled normal points.
     */
    fun downsample(points: List<HistoricalPoint>, maxPoints: Int = 200): List<HistoricalPoint> {
        if (points.size <= maxPoints) {
            return points
        }

        val anomalies = points.filter { it.isAnomaly }
        val normals = points.filterNot { it.isAnomaly }

        if (anomalies.size >= maxPoints) {
            // If anomalies alone exceed maxPoints, return all anomalies sorted
            return anomalies.sortedBy { it.timestamp }
        }

        val allowedNormals = maxPoints - anomalies.size
        if (allowedNormals <= 0 || normals.isEmpty()) {
            return anomalies.sortedBy { it.timestamp }
        }

        val step = max(1, normals.size / allowedNormals)
        val sampledNormals = ArrayList<HistoricalPoint>(allowedNormals)

        for (i in normals.indices step step) {
            sampledNormals.add(normals[i])
            if (sampledNormals.size >= allowedNormals) break
        }

        // Always ensure the very latest normal reading is preserved if not already included
        val lastNormal = normals.last()
        if (sampledNormals.isNotEmpty() && sampledNormals.last() != lastNormal && sampledNormals.size < allowedNormals) {
            sampledNormals.add(lastNormal)
        }

        val combined = ArrayList<HistoricalPoint>(sampledNormals.size + anomalies.size)
        combined.addAll(sampledNormals)
        combined.addAll(anomalies)

        // Sort chronologically by ISO-8601 timestamp
        combined.sortBy { it.timestamp }
        return combined
    }
}
