package com.cogxar.pdm.ui.history

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.cogxar.pdm.R
import com.cogxar.pdm.data.model.HistoricalReadingsResponse.HistoricalPoint
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Helper to configure and render dark-themed industrial telemetry charts using MPAndroidChart.
 * Renders anomalies as an independent second dataset (circles without line) in muted red.
 * Prepared as a reusable chart component (for Trends screen).
 */
object ChartRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd", Locale.US)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun setupChart(chart: LineChart, context: Context) {
        val textColor = ContextCompat.getColor(context, R.color.text_secondary)
        val gridColor = ContextCompat.getColor(context, R.color.border_card)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setNoDataText(context.getString(R.string.history_empty_range))
        chart.setNoDataTextColor(textColor)

        // Offsets so labels never overlap the plot area
        chart.extraLeftOffset = 12f
        chart.extraRightOffset = 12f
        chart.extraBottomOffset = 8f
        chart.extraTopOffset = 8f

        // X-Axis (Bottom time axis)
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setTextColor(textColor)
            setGridColor(gridColor)
            setAxisLineColor(gridColor)
            setDrawGridLines(true)
            granularity = 1f
            setLabelCount(5, false)
            setAvoidFirstLastClipping(true)
            textSize = 10f
        }

        // Left Y-Axis (Values) - disable right axis, limit 4-5 labels, 1 decimal, padding
        chart.axisLeft.apply {
            setTextColor(textColor)
            setGridColor(gridColor)
            setAxisLineColor(gridColor)
            setDrawZeroLine(false)
            setLabelCount(5, false)
            granularity = 0.1f
            textSize = 10f
            // Padding to axis min/max
            setSpaceTop(15f)
            setSpaceBottom(15f)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format(Locale.US, "%.1f", value)
                }
            }
        }

        // Disable right Y-Axis
        chart.axisRight.isEnabled = false
    }

    @JvmOverloads
    fun renderData(
        chart: LineChart,
        points: List<HistoricalPoint>,
        range: String = "24h",
        valueExtractor: (HistoricalPoint) -> Double
    ) {
        if (points.isEmpty()) {
            chart.clear()
            return
        }

        val context = chart.context
        val accentColor = ContextCompat.getColor(context, R.color.accent_brand)
        val anomalyColor = ContextCompat.getColor(context, R.color.status_red)
        val cardBgColor = ContextCompat.getColor(context, R.color.bg_card)

        // Points are sorted oldest to newest (left to right)
        val lineEntries = ArrayList<Entry>(points.size)
        val anomalyEntries = ArrayList<Entry>()

        for (i in points.indices) {
            val pt = points[i]
            val value = valueExtractor(pt).toFloat()
            val entry = Entry(i.toFloat(), value)
            lineEntries.add(entry)

            if (pt.isAnomaly) {
                anomalyEntries.add(Entry(i.toFloat(), value))
            }
        }

        // Primary continuous telemetry line dataset
        val lineDataSet = LineDataSet(lineEntries, "Telemetry").apply {
            color = accentColor
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            isHighlightEnabled = false
        }

        // Secondary dataset for anomaly points (red circle markers without connecting line)
        val anomalyDataSet = LineDataSet(anomalyEntries, "Anomalies").apply {
            color = Color.TRANSPARENT
            setCircleColor(anomalyColor)
            circleHoleColor = cardBgColor
            circleRadius = 5f
            circleHoleRadius = 2f
            setDrawCircleHole(true)
            setDrawCircles(true)
            setDrawValues(false)
            lineWidth = 0f
            enableDashedLine(0f, 0f, 0f)
            isHighlightEnabled = false
        }

        // Limit x-axis labels to about 4-5, formatted by range (HH:mm for 1h and 24h, date for 7d)
        chart.xAxis.setLabelCount(5, false)
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index in points.indices) {
                    val timestamp = points[index].timestamp
                    return formatTimestampForRange(timestamp, range)
                }
                return ""
            }
        }

        chart.data = LineData(lineDataSet, anomalyDataSet)
        chart.invalidate()
    }

    private fun formatTimestampForRange(isoTimestamp: String?, range: String): String {
        if (isoTimestamp == null || isoTimestamp.trim().isEmpty()) return ""
        return try {
            val instant = Instant.parse(isoTimestamp.trim())
            val zdt = instant.atZone(ZoneId.systemDefault())
            if (range.equals("7d", ignoreCase = true)) {
                zdt.format(DATE_FORMATTER)
            } else {
                zdt.format(TIME_FORMATTER)
            }
        } catch (e: Exception) {
            isoTimestamp
        }
    }
}
