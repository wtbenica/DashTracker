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

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao.NewCpm
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ChartHolderBinding
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.expand
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
class DTChartHolder @JvmOverloads constructor(
    context: Context,
    attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrSet, defStyleAttr) {
    private var mCallback: DTChartHolderCallback? = null
    val binding: ChartHolderBinding = ChartHolderBinding.inflate(LayoutInflater.from(context), this)

    private var mCpmListDaily = listOf<NewCpm>()
    private var mCpmListWeekly = listOf<NewCpm>()
    private var mEntries = listOf<DashEntry>()
    private var mWeeklies = listOf<FullWeekly>()
    private var mChart: DTChart? = null
    private var mFilterVisible: Boolean = false

    interface DTChartHolderCallback

    init {
        binding.btnShowFilters.setOnClickListener {
            if (mFilterVisible) {
                mChart?.hideFilters()
                runShowTuneIconCollapseAnimation()
            } else {
                mChart?.showFilters()
                runShowTuneIconExpandAnimation()
            }
            mFilterVisible = !mFilterVisible
        }
    }

    fun initialize(callback: DTChartHolderCallback, chart: DTChart) {
        mCallback = callback
        mChart = chart
        binding.cpmBottom.addView(mChart)
        chart.title?.let { binding.chartHolderTitle.setText(it) }
    }

    private fun runShowTuneIconCollapseAnimation() {
        binding.btnShowFilters.run {
            setImageResource(R.drawable.anim_close_to_tune)
            when (val d = drawable) {
                is AnimatedVectorDrawableCompat -> d.start()
                is AnimatedVectorDrawable -> d.start()
            }
        }
    }

    private fun runShowTuneIconExpandAnimation() {
        binding.btnShowFilters.run {
            setImageResource(R.drawable.anim_tune_to_close)
            when (val d = drawable) {
                is AnimatedVectorDrawableCompat -> d.start()
                is AnimatedVectorDrawable -> d.start()
            }
        }
    }

    fun updateLists(
        cpmListDaily: List<NewCpm>? = null,
        cpmListWeekly: List<NewCpm>? = null,
        entries: List<DashEntry>? = null,
        weeklies: List<FullWeekly>? = null
    ) {
        if (cpmListDaily?.isNotEmpty() == true) {
            mCpmListDaily = cpmListDaily
        }
        if (cpmListWeekly?.isNotEmpty() == true) {
            mCpmListWeekly = cpmListWeekly
        }
        if (entries?.isNotEmpty() == true) {
            mEntries = entries
        }
        if (weeklies?.isNotEmpty() == true) {
            mWeeklies = weeklies
        }
        mChart?.update(mCpmListDaily, mCpmListWeekly, mEntries, mWeeklies)
    }
}

@ExperimentalCoroutinesApi
abstract class DTChart @JvmOverloads constructor(
    context: Context,
    attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StringRes val title: Int? = null,
    @StringRes val subtitle: Int? = null
) : LinearLayout(context, attrSet, defStyleAttr) {

    abstract val filterTable: ViewGroup

    fun hideFilters(onComplete: (() -> Unit)? = null) {
        filterTable.collapse(onComplete)
    }

    fun showFilters(onComplete: (() -> Unit)? = null) {
        filterTable.expand(onComplete)
    }


    open fun update(
        cpmListDaily: List<NewCpm>? = null,
        cpmListWeekly: List<NewCpm>? = null,
        entries: List<DashEntry>? = null,
        weeklies: List<FullWeekly>? = null
    ) {
        cpmListDaily?.let { if (it.isNotEmpty()) mCpmListDaily = it }
        cpmListWeekly?.let { if (it.isNotEmpty()) mCpmListWeekly = it }
        entries?.let { if (it.isNotEmpty()) mEntries = it }
        weeklies?.let { if (it.isNotEmpty()) mWeeklies = it }
    }

    internal var mCpmListDaily = listOf<NewCpm>()
    internal var mCpmListWeekly = listOf<NewCpm>()
    internal var mEntries = listOf<DashEntry>()
    internal var mWeeklies = listOf<FullWeekly>()
}

/**
 * TODO
 *
 * @param res
 * @return
 */
fun Context.getDimen(@DimenRes res: Int): Float =
    resources.getDimension(res) / resources.displayMetrics.density
