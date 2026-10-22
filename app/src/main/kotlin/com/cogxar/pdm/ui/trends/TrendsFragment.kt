package com.cogxar.pdm.ui.trends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cogxar.pdm.databinding.FragmentTrendsBinding
import com.cogxar.pdm.ui.SharedMachineViewModel

/**
 * Trends Screen placeholder (Kotlin).
 * Shares machine selection across tabs and is reserved to host multi-sensor telemetry charts.
 */
class TrendsFragment : Fragment() {

    private var _binding: FragmentTrendsBinding? = null
    private val binding get() = _binding!!

    private val sharedMachineViewModel: SharedMachineViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Shared machine selection is observed to keep state in sync across Dashboard, History, and Trends
        sharedMachineViewModel.selectedMachine.observe(viewLifecycleOwner) { machine ->
            // Ready for machine-specific telemetry trend charts
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
