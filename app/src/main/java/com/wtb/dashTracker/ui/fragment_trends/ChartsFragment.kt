/*
 * Copyright 2022 Wesley T. Benica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.dashTracker.ui.fragment_trends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayoutMediator
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao.NewCpm
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.FragChartsBinding
import com.wtb.dashTracker.repository.DeductionType.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class ChartsFragment : Fragment(), DTChartHolder.DTChartHolderCallback {
    private val viewModel: ChartsViewModel by viewModels()
    private lateinit var binding: FragChartsBinding

    private var newCpmListDaily = listOf<NewCpm>()
    private var newCpmListWeekly = listOf<NewCpm>()
    private var entries = listOf<DashEntry>()
    private var weeklies = listOf<FullWeekly>()
    private val charts = mutableListOf<DTChartHolder>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_charts, container, false)
    }

    inner class ChartHolder(val chartHolder: DTChartHolder) : RecyclerView.ViewHolder(chartHolder) {
        fun bind(chart: DTChart) {
            chartHolder.apply {
                initialize(this@ChartsFragment, chart)
                updateLists(
                    cpmListDaily = newCpmListDaily,
                    cpmListWeekly = newCpmListWeekly,
                    entries = entries,
                    weeklies = weeklies
                )
            }
        }

    }

    inner class ChartAdapter : RecyclerView.Adapter<ChartHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartHolder {
            return ChartHolder(DTChartHolder(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            })
        }

        override fun onBindViewHolder(holder: ChartHolder, position: Int) {
            val chart = when (position) {
                1 -> CpmChart(requireContext())
                0 -> HourlyBarChart(requireContext())
                2 -> ByDayOfWeekBarChart(requireContext())
                else -> HourlyBarChart(requireContext())
            }

            charts.add(holder.chartHolder)

            holder.bind(chart)
        }

        override fun getItemCount(): Int = 3
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragChartsBinding.bind(view)

        val viewPager = binding.chartPager
        viewPager.adapter = ChartAdapter()

        TabLayoutMediator(binding.chartsTabs, viewPager) { tab, position ->
            tab.text = when (position) {
                1 -> "Cost per mile"
                2 -> "Daily stats"
                else -> "Hourly gross/net"
            }
        }.attach()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entryList.collectLatest {
                    entries = it

                    newCpmListDaily = it.map { e ->
                        NewCpm(
                            date = e.date,
                            gasOnlyCpm = viewModel.getCostPerMile(e.date, GAS_ONLY),
                            actualCpm = viewModel.getCostPerMile(e.date, ALL_EXPENSES),
                            irsStdCpm = viewModel.getCostPerMile(e.date, IRS_STD)
                        )
                    }.reversed()

                    charts.forEach { holder ->
                        holder.updateLists(
                            cpmListDaily = newCpmListDaily,
                            cpmListWeekly = newCpmListWeekly,
                            entries = entries,
                            weeklies = weeklies
                        )
                    }
                }
            }
        }


        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weeklyList.collectLatest {
                    weeklies = it

                    newCpmListWeekly = it.map { w ->
                        NewCpm(
                            date = w.weekly.date,
                            gasOnlyCpm = viewModel.getExpensesAndCostPerMile(w, GAS_ONLY).second,
                            actualCpm = viewModel.getExpensesAndCostPerMile(w, ALL_EXPENSES).second,
                            irsStdCpm = viewModel.getExpensesAndCostPerMile(w, IRS_STD).second,
                        )
                    }.reversed()

                    charts.forEach { holder ->
                        holder.updateLists(
                            cpmListDaily = newCpmListDaily,
                            cpmListWeekly = newCpmListWeekly,
                            entries = entries,
                            weeklies = weeklies
                        )
                    }
                }
            }
        }
    }
}