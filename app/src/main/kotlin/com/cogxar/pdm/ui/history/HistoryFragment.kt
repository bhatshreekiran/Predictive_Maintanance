package com.cogxar.pdm.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.cogxar.pdm.R
import com.cogxar.pdm.data.model.HistoricalReadingsResponse.HistoricalPoint
import com.cogxar.pdm.data.model.Machine
import com.cogxar.pdm.databinding.FragmentHistoryBinding
import com.cogxar.pdm.ui.SharedMachineViewModel
import kotlinx.coroutines.launch


/**
 * History Screen (Kotlin, ViewBinding, MVVM with StateFlow).
 * Pure data log displaying newest-first telemetry readings with range and machine selectors.
 * Synchronizes machine selection with Dashboard via SharedMachineViewModel.
 */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private val sharedMachineViewModel: SharedMachineViewModel by activityViewModels()

    private lateinit var machineAdapter: ArrayAdapter<Machine>
    private lateinit var rangeAdapter: ArrayAdapter<String>
    private lateinit var tableAdapter: HistoryTableAdapter

    private val rangeLabels by lazy {
        listOf(
            getString(R.string.range_1_hour),
            getString(R.string.range_1_day),
            getString(R.string.range_7_days)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMachineDropdown()
        setupRangeDropdown()
        setupExportButton()
        setupReadingsTable()
        setupRefreshAndRetry()
        observeState()
    }

    private fun setupMachineDropdown() {
        machineAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line)
        binding.actvHistoryMachineDropdown.setAdapter(machineAdapter)

        binding.actvHistoryMachineDropdown.setOnItemClickListener { _, _, position, _ ->
            val selected = machineAdapter.getItem(position)
            if (selected != null) {
                viewModel.setMachine(selected)
                sharedMachineViewModel.selectMachine(selected)
            }
        }
    }

    private fun setupRangeDropdown() {
        rangeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, rangeLabels)
        binding.actvHistoryRangeDropdown.setAdapter(rangeAdapter)
        // Default display: 1 day
        binding.actvHistoryRangeDropdown.setText(getString(R.string.range_1_day), false)

        binding.actvHistoryRangeDropdown.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> viewModel.setRange("1h")
                2 -> viewModel.setRange("7d")
                else -> viewModel.setRange("24h")
            }
        }
    }

    private fun setupExportButton() {
        binding.btnExport.setOnClickListener {
            // Frontend button ready for future server export integration
        }
    }

    private fun setupReadingsTable() {
        tableAdapter = HistoryTableAdapter()
        binding.rvHistoryTable.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistoryTable.adapter = tableAdapter

        binding.btnShowMore.setOnClickListener {
            val willExpand = !tableAdapter.isExpanded()
            tableAdapter.setExpanded(willExpand)
            binding.btnShowMore.text = if (willExpand) "Show Less" else getString(R.string.history_btn_show_more)
        }
    }

    private fun setupRefreshAndRetry() {
        binding.historySwipeRefresh.setColorSchemeResources(R.color.accent_brand)
        binding.historySwipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card)
        binding.historySwipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.layoutHistoryError.btnRetry.setOnClickListener {
            binding.historySwipeRefresh.isRefreshing = true
            viewModel.retry()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect machines list
                launch {
                    viewModel.machines.collect { machines ->
                        machineAdapter.clear()
                        machineAdapter.addAll(machines)
                        machineAdapter.notifyDataSetChanged()
                    }
                }

                // Collect selected machine
                launch {
                    viewModel.selectedMachine.collect { machine ->
                        if (machine != null) {
                            binding.actvHistoryMachineDropdown.setText(machine.name, false)
                            sharedMachineViewModel.selectMachine(machine)
                        }
                    }
                }

                // Collect selected machine from shared activity view model
                launch {
                    sharedMachineViewModel.selectedMachine.observe(viewLifecycleOwner) { machine ->
                        if (machine != null && machine.id != viewModel.selectedMachine.value?.id) {
                            viewModel.setMachine(machine)
                        }
                    }
                }

                // Collect selected range
                launch {
                    viewModel.selectedRange.collect { range ->
                        val label = when (range.lowercase()) {
                            "1h" -> getString(R.string.range_1_hour)
                            "7d" -> getString(R.string.range_7_days)
                            else -> getString(R.string.range_1_day)
                        }
                        binding.actvHistoryRangeDropdown.setText(label, false)
                    }
                }

                // Collect UI state
                launch {
                    viewModel.uiState.collect { state ->
                        renderUiState(state)
                    }
                }
            }
        }
    }

    private fun renderUiState(state: HistoryUiState) {
        binding.historySwipeRefresh.isRefreshing = false

        when (state) {
            is HistoryUiState.Loading -> {
                binding.btnExport.isEnabled = false
                binding.historyShimmerContainer.visibility = View.VISIBLE
                binding.historyShimmerContainer.startShimmer()
                binding.nsvHistoryContent.visibility = View.GONE
                binding.layoutHistoryEmpty.visibility = View.GONE
                binding.layoutHistoryError.layoutErrorRoot.visibility = View.GONE
            }

            is HistoryUiState.Success -> {
                binding.historyShimmerContainer.stopShimmer()
                binding.historyShimmerContainer.visibility = View.GONE
                binding.nsvHistoryContent.visibility = View.VISIBLE
                binding.layoutHistoryEmpty.visibility = View.GONE
                binding.layoutHistoryError.layoutErrorRoot.visibility = View.GONE

                binding.historySwipeRefresh.isRefreshing = state.isRefreshing
                binding.btnExport.isEnabled = state.readings.isNotEmpty()

                populateData(state.readings)
            }

            is HistoryUiState.Empty -> {
                binding.btnExport.isEnabled = false
                binding.historyShimmerContainer.stopShimmer()
                binding.historyShimmerContainer.visibility = View.GONE
                binding.nsvHistoryContent.visibility = View.GONE
                binding.layoutHistoryEmpty.visibility = View.VISIBLE
                binding.layoutHistoryError.layoutErrorRoot.visibility = View.GONE
            }

            is HistoryUiState.Error -> {
                binding.btnExport.isEnabled = false
                binding.historyShimmerContainer.stopShimmer()
                binding.historyShimmerContainer.visibility = View.GONE
                binding.nsvHistoryContent.visibility = View.GONE
                binding.layoutHistoryEmpty.visibility = View.GONE
                binding.layoutHistoryError.layoutErrorRoot.visibility = View.VISIBLE
                binding.layoutHistoryError.tvErrorMessage.text = state.message
            }
        }
    }

    private fun populateData(readings: List<HistoricalPoint>) {
        // Render readings table (tableAdapter reverses to show newest first)
        tableAdapter.submitList(readings)

        // Update "Show More" button visibility
        binding.btnShowMore.visibility = if (tableAdapter.hasMoreThanInitialLimit()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
