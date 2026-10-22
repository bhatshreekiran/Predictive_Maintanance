package com.cogxar.pdm.util

import com.cogxar.pdm.data.model.HistoricalReadingsResponse.HistoricalPoint
import com.cogxar.pdm.data.model.PhysicalAiTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Pure JVM unit tests for downsampling and chart data processing.
 * Operates on plain data models (HistoricalPoint) without Android or MPAndroidChart dependencies.
 */
class ChartUtilsTest {

    @Test
    fun downsample_whenFewerThanMax_returnsOriginalList() {
        val points = (0 until 50).map { i ->
            HistoricalPoint(
                Instant.ofEpochSecond(1000L + i * 60).toString(),
                72.0 + (i % 3),
                4.2,
                PhysicalAiTag(3),
                false
            )
        }

        val result = ChartUtils.downsample(points, maxPoints = 200)
        assertEquals(50, result.size)
    }

    @Test
    fun downsample_preservesEverySingleAnomalyPoint() {
        val anomalyIndices = setOf(10, 45, 120, 250, 380, 490)
        val totalPoints = 500

        val points = (0 until totalPoints).map { i ->
            val isAnomaly = anomalyIndices.contains(i)
            HistoricalPoint(
                Instant.ofEpochSecond(1000L + i * 60).toString(),
                if (isAnomaly) 88.0 else 72.0,
                if (isAnomaly) 9.5 else 4.2,
                PhysicalAiTag(if (isAnomaly) 14 else 2),
                isAnomaly
            )
        }

        val result = ChartUtils.downsample(points, maxPoints = 200)

        // Result size must be within max limit
        assertTrue("Result size should be <= 200, got ${result.size}", result.size <= 200)

        // EVERY anomaly point must be retained
        val resultAnomalies = result.filter { it.isAnomaly }
        assertEquals("All 6 anomalies must be preserved", anomalyIndices.size, resultAnomalies.size)

        val resultAnomalyTimestamps = resultAnomalies.map { it.timestamp }.toSet()
        for (idx in anomalyIndices) {
            val expectedTs = points[idx].timestamp
            assertTrue("Expected anomaly at index $idx ($expectedTs) to be kept", resultAnomalyTimestamps.contains(expectedTs))
        }
    }

    @Test
    fun downsample_preservesChronologicalOrder() {
        val points = (0 until 300).map { i ->
            val isAnomaly = (i % 40 == 0)
            HistoricalPoint(
                Instant.ofEpochSecond(5000L + i * 120).toString(),
                70.0 + (i % 5),
                4.0,
                PhysicalAiTag(1),
                isAnomaly
            )
        }

        val result = ChartUtils.downsample(points, maxPoints = 150)

        for (i in 0 until result.size - 1) {
            val currentTs = result[i].timestamp
            val nextTs = result[i + 1].timestamp
            assertTrue("Timestamps must be strictly non-decreasing: $currentTs <= $nextTs", currentTs <= nextTs)
        }
    }
}
