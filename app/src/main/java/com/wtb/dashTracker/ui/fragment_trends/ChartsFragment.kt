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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao.NewCpm
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.FragChartsBinding
import com.wtb.dashTracker.repository.DeductionType.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChartsFragment : Fragment(), DTChartHolder.DTChartHolderCallback {
    private val viewModel: ChartsViewModel by viewModels()
    private lateinit var binding: FragChartsBinding

    private lateinit var barChartHourlyByDay: HorizontalBarChart

    private var newCpmListDaily = listOf<NewCpm>()
    private var newCpmListWeekly = listOf<NewCpm>()
    private var entries = listOf<DashEntry>()
    private var weeklies = listOf<FullWeekly>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragChartsBinding.bind(view)

        val cpmChartHolder = DTChartHolder(requireContext()).apply {
            initialize(this@ChartsFragment, CpmChart(context))
        }
        val hourlyGrossNetChartHolder = DTChartHolder(requireContext()).apply {
            initialize(this@ChartsFragment, HourlyBarChart(context))
        }
        val hourlyByDayChartHolder = DTChartHolder(requireContext()).apply {
            initialize(this@ChartsFragment, ByDayOfWeekBarChart(context))
        }

        binding.chartList.apply {
            addView(cpmChartHolder)
            addView(hourlyGrossNetChartHolder)
            addView(hourlyByDayChartHolder)
        }

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

                    hourlyGrossNetChartHolder.updateLists(
                        cpmListDaily = newCpmListDaily,
                        cpmListWeekly = newCpmListWeekly,
                        entries = entries,
                        weeklies = weeklies
                    )
                    hourlyByDayChartHolder.updateLists(
                        cpmListDaily = newCpmListDaily,
                        cpmListWeekly = newCpmListWeekly,
                        entries = entries,
                        weeklies = weeklies
                    )
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

                    cpmChartHolder.updateLists(cpmListWeekly = newCpmListWeekly)

                    hourlyGrossNetChartHolder.updateLists(
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