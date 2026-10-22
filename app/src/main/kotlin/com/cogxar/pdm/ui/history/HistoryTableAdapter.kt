package com.cogxar.pdm.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cogxar.pdm.R
import com.cogxar.pdm.data.model.HistoricalReadingsResponse.HistoricalPoint
import com.cogxar.pdm.databinding.ItemHistoryReadingBinding
import com.cogxar.pdm.util.TimeUtils

/**
 * Adapter for the historical sensor readings table.
 * Displays readings in reverse chronological order (newest first).
 * Supports expanding from the initial 15 rows to the full dataset.
 */
class HistoryTableAdapter : RecyclerView.Adapter<HistoryTableAdapter.ViewHolder>() {

    private var allReadings: List<HistoricalPoint> = emptyList()
    private var isExpanded: Boolean = false

    fun submitList(readings: List<HistoricalPoint>) {
        // Table shows newest first (reversed chronological order)
        allReadings = readings.reversed()
        notifyDataSetChanged()
    }

    fun setExpanded(expanded: Boolean) {
        if (isExpanded != expanded) {
            isExpanded = expanded
            notifyDataSetChanged()
        }
    }

    fun isExpanded(): Boolean = isExpanded

    fun hasMoreThanInitialLimit(): Boolean = allReadings.size > INITIAL_ROW_LIMIT

    override fun getItemCount(): Int {
        return if (isExpanded || allReadings.size <= INITIAL_ROW_LIMIT) {
            allReadings.size
        } else {
            INITIAL_ROW_LIMIT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryReadingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(allReadings[position])
    }

    class ViewHolder(private val binding: ItemHistoryReadingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(point: HistoricalPoint) {
            val context = itemView.context

            // Format local time (e.g. "10:21 AM")
            binding.tvColTime.text = TimeUtils.formatTimeOnly(point.timestamp)

            // Monospace sensor telemetry
            binding.tvColTemp.text = String.format("%.1f °C", point.temperatureC)
            binding.tvColVib.text = String.format("%.1f mm/s", point.vibrationRms)

            // Flag column: never relies on color alone; always includes descriptive text
            if (point.isAnomaly) {
                binding.tvColFlag.text = context.getString(R.string.history_flag_anomaly)
                binding.tvColFlag.setTextColor(ContextCompat.getColor(context, R.color.status_red))
            } else {
                binding.tvColFlag.text = context.getString(R.string.history_flag_normal)
                binding.tvColFlag.setTextColor(ContextCompat.getColor(context, R.color.status_green))
            }

            itemView.contentDescription = context.getString(
                R.string.cd_history_table
            ) + ": ${binding.tvColTime.text}, ${binding.tvColTemp.text}, ${binding.tvColVib.text}, ${binding.tvColFlag.text}"
        }
    }

    companion object {
        const val INITIAL_ROW_LIMIT = 15
    }
}
