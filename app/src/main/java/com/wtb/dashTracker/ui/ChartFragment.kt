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
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao.Cpm
import com.wtb.dashTracker.databinding.FragChartsBinding
import com.wtb.dashTracker.extensions.dtfMini
import com.wtb.dashTracker.repository.DeductionType.ALL_EXPENSES
import com.wtb.dashTracker.ui.fragment_trends.DailyStatsViewModel
import com.wtb.dashTracker.ui.fragment_trends.DailyStatsViewModel.Companion.MIN_NUM_WEEKS
import com.wtb.dashTracker.ui.fragment_trends.TAG
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Integer.max
import java.time.LocalDate

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@ExperimentalCoroutinesApi
class ChartFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val viewModel: DailyStatsViewModel by viewModels()
    private lateinit var binding: FragChartsBinding

    private var cpmList = listOf<Cpm>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun initSeekBar() {
            binding.seekBarNumWeeks.apply {
                min = MIN_NUM_WEEKS
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        binding.numWeeksTv.text = progress.toString()
                        if (cpmList.isNotEmpty())
                            updateUI()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        // Do nothing
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        // Do nothing
                    }
                })
                progress = MIN_NUM_WEEKS
            }
        }

        super.onViewCreated(view, savedInstanceState)

        binding = FragChartsBinding.bind(view)

        initSeekBar()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weeklyList.collectLatest {
                    val numWeeklies = it.size
                    binding.seekBarNumWeeks.max = numWeeklies
                    cpmList = it.map { w ->
                        Cpm(
                            w.weekly.date,
                            viewModel.getExpensesAndCostPerMile(w, ALL_EXPENSES).second
                        )
                    }.reversed()
                    if (cpmList.isNotEmpty())
                        updateUI()
                }
            }
        }
    }

    private fun updateUI() {
        (context as MainActivity).runOnUiThread {
            initCpmLineChart()
            initBarChart()
        }
    }

    private fun initBarChart() {
        val barChart = binding.chartBarDailyHourly.apply {
            setTouchEnabled(false)
            description.isEnabled = false
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
        }


    }

    private fun initCpmLineChart() {
        val lineChart = binding.chartLineCpm.apply {
            setTouchEnabled(false)
            description.isEnabled = false
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
        }

        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            this.setCenterAxisLabels(false)
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val date = LocalDate.ofEpochDay(value.toLong())
                    return date.format(dtfMini)
                }
            }
        }

        lineChart.axisLeft.apply {
            setDrawGridLines(false)
            this.axisMinimum = 0f
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return getString(R.string.cpm_unit, value)
                }
            }
        }

        lineChart.axisRight.isEnabled = false

        val dataSet: List<Entry> =
            cpmList.mapNotNull { cpm ->
                if (cpm.cpm !in listOf(Float.NaN, 0f))
                    Entry(cpm.date.toEpochDay().toFloat(), cpm.cpm)
                else
                    null
            }.let {
                it.subList(max(it.size - binding.seekBarNumWeeks.progress, 1), it.size)
            }


        val lineData = LineData(
            LineDataSet(dataSet, "CPM").apply {
                Log.d(TAG, "Made it to here: dataset size: ${dataSet.size}")
                this.mode = LineDataSet.Mode.CUBIC_BEZIER
                this.color = MainActivity.getAttrColor(requireContext(), R.attr.colorSecondary)
                this.setDrawCircles(false)
                this.setDrawIcons(false)
                this.setDrawCircleHole(false)
                this.setDrawFilled(true)
                this.fillDrawable =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bad_to_good)
                this.fillAlpha = 85
                cubicIntensity = 0.2f
            }
        )

        lineChart.apply {
            Log.d(TAG, "Made it to here 2")
            data = lineData
            legend.isEnabled = false
            animateY(1000)
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChartFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChartFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}