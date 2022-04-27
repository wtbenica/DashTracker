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

package com.wtb.dashTracker.ui

import android.os.Bundle
import android.util.Log
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
import com.wtb.dashTracker.database.daos.TransactionDao.Cpm
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.FragChartsBinding
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.repository.DeductionType.ALL_EXPENSES
import com.wtb.dashTracker.ui.fragment_trends.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalCoroutinesApi
class ChartFragment : Fragment(), DTChartHolder.DTChartHolderCallback {
    private val viewModel: DailyStatsViewModel by viewModels()
    private lateinit var binding: FragChartsBinding

    private lateinit var barChartHourlyByDay: HorizontalBarChart

    private var cpmList = listOf<Cpm>()
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
            initialize(this@ChartFragment, CpmChart(context).apply { init() })
        }
        val hourlyGrossNetChartHolder = DTChartHolder(requireContext()).apply {
            initialize(this@ChartFragment, HourlyBarChart(context).apply { init() })
        }
        val hourlyByDayChartHolder = DTChartHolder(requireContext()).apply {
            initialize(this@ChartFragment, HourlyByDayBarChart(context).apply { init() })
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

                    hourlyGrossNetChartHolder.updateLists(cpmList, entries, weeklies)
                    hourlyByDayChartHolder.updateLists(cpmList, entries, weeklies)
                }
            }
        }


        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weeklyList.collectLatest {
                    Log.d(TAG, "collectLatest weeklyList")
                    weeklies = it

                    cpmList = it.map { w ->
                        Cpm(
                            w.weekly.date,
                            viewModel.getExpensesAndCostPerMile(w, ALL_EXPENSES).second
                        )
                    }.reversed()

                    cpmChartHolder.updateLists(cpmList)
                    hourlyGrossNetChartHolder.updateLists(
                        cpmList = cpmList,
                        entries = entries,
                        weeklies = weeklies
                    )
                }
            }
        }
    }
}