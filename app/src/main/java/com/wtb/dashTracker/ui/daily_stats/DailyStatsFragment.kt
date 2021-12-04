package com.wtb.dashTracker.ui.daily_stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wtb.dashTracker.database.DashEntry
import com.wtb.dashTracker.databinding.DailyStatsFragmentBinding
import com.wtb.dashTracker.views.DailyStats
import com.wtb.dashTracker.views.DailyStatsRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@ExperimentalCoroutinesApi
class DailyStatsFragment : Fragment() {

    private var _binding: DailyStatsFragmentBinding? = null
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
    ): View? {
        _binding = DailyStatsFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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

            DayOfWeek.values().forEach { day ->
                val d = entries.filter { de ->
                    de.startDateTime?.dayOfWeek == day
                }.fold(DailyStats(day = day)) { a, d ->
                    DailyStats(
                        day = a.day,
                        amHours = (a.amHours ?: 0f) + (d.dayHours ?: 0f),
                        pmHours = (a.pmHours ?: 0f) + (d.nightHours ?: 0f),
                        amEarned = (a.amEarned ?: 0f) + (d.dayEarned ?: 0f),
                        pmEarned = (a.pmEarned ?: 0f) + (d.nightEarned ?: 0f),
                        amDels = (a.amDels ?: 0f) + (d.dayDels ?: 0f),
                        pmDels = (a.pmDels ?: 0f) + (d.nightDels ?: 0f)
                    )
                }

                DailyStatsRow(context!!, null, d).addToGridLayout(binding.dailyStatsFragmentTable, SKIP_ROWS)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}