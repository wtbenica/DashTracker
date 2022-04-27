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

@file:Suppress("unused")

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
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.databinding.FragTrendsBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@ExperimentalCoroutinesApi
class DailyStatsFragment : Fragment() {

    private var _binding: FragTrendsBinding? = null
    private val binding
        get() = _binding!!
    private var _entries: List<DashEntry>? = null

    companion object {
        private const val SKIP_ROWS = 1

        fun newInstance() = DailyStatsFragment()
    }

    private val viewModel: DailyStatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragTrendsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entryList.collectLatest {
                    _entries = it
                    updateUI()
                }
            }
        }
    }

    private fun updateUI() {
        _entries?.let { entries ->
            binding.dailyStatsFragmentTable.removeAllViews()
            binding.dailyStatsFragmentTable.addView(binding.dailyStatsFragmentLabelHourly)
            binding.dailyStatsFragmentTable.addView(binding.dailyStatsFragmentLabelAvgDel)
            binding.dailyStatsFragmentTable.addView(binding.dailyStatsFragmentLabelDph)
            binding.dailyStatsFragmentTable.addView(binding.dailyStatsFragmentLabelNumShifts)

            DayOfWeek.values().forEach { day ->
                val stats: DailyStats2 = entries.filter { entry: DashEntry ->
                    entry.startDateTime?.dayOfWeek == day
                }.fold(DailyStats2(day = day)) { a: DailyStats2, d: DashEntry ->
                    DailyStats2(
                        day = a.day,
                        amHours = (a.amHours ?: 0f) + (d.dayHours ?: 0f),
                        pmHours = (a.pmHours ?: 0f) + (d.nightHours ?: 0f),
                        amEarned = (a.amEarned ?: 0f) + (d.dayEarned ?: 0f),
                        pmEarned = (a.pmEarned ?: 0f) + (d.nightEarned ?: 0f),
                        amDels = (a.amDels ?: 0f) + (d.dayDels ?: 0f),
                        pmDels = (a.pmDels ?: 0f) + (d.nightDels ?: 0f),
                        amNumShifts = (a.amNumShifts ?: 0) + (if ((d.dayHours ?: 0f) > 0f) 1 else 0),
                        pmNumShifts = (a.pmNumShifts ?: 0) + (if ((d.nightHours ?: 0f) > 0f) 1 else 0),
                    )
                }

                DailyStatsRow(requireContext(), null, stats).addToGridLayout(
                    binding.dailyStatsFragmentTable,
                    SKIP_ROWS
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}