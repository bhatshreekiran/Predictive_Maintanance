package com.cogxar.pdm.ui.dashboard.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.cogxar.pdm.R;
import com.cogxar.pdm.data.model.SensorReading;
import com.cogxar.pdm.databinding.ItemSensorCardBinding;

/**
 * Adapter presenting sensors in a 2-column dynamic grid.
 * Sensor types, names, values, and units are data-driven.
 */
public class SensorAdapter extends ListAdapter<SensorReading, SensorAdapter.SensorViewHolder> {

    public SensorAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<SensorReading> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SensorReading>() {
                @Override
                public boolean areItemsTheSame(@NonNull SensorReading oldItem, @NonNull SensorReading newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull SensorReading oldItem, @NonNull SensorReading newItem) {
                    return Double.compare(oldItem.getValue(), newItem.getValue()) == 0
                            && oldItem.getName().equals(newItem.getName())
                            && oldItem.getUnit().equals(newItem.getUnit());
                }
            };

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSensorCardBinding binding = ItemSensorCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new SensorViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class SensorViewHolder extends RecyclerView.ViewHolder {
        private final ItemSensorCardBinding binding;

        public SensorViewHolder(@NonNull ItemSensorCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SensorReading sensor) {
            binding.tvSensorName.setText(sensor.getName());
            binding.tvSensorValue.setText(String.format(java.util.Locale.US, "%.1f", sensor.getValue()));
            binding.tvSensorUnit.setText(sensor.getUnit());

            // Icon selection by sensor 'type'
            String type = sensor.getType() != null ? sensor.getType().toLowerCase() : "";
            if (type.contains("temp")) {
                binding.ivSensorIcon.setImageResource(R.drawable.ic_thermometer);
            } else if (type.contains("vib")) {
                binding.ivSensorIcon.setImageResource(R.drawable.ic_waveform);
            } else {
                binding.ivSensorIcon.setImageResource(R.drawable.ic_grid);
            }
        }
    }
}
