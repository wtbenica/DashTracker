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
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.wtb.dashTracker.database.daos.TransactionDao.Cpm
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ChartHolderBinding
import com.wtb.dashTracker.ui.fragment_list_item_base.toggleListItemVisibility
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
class DTChartHolder @JvmOverloads constructor(
    context: Context,
    attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrSet, defStyleAttr) {
    private var mCallback: DTChartHolderCallback? = null
    val binding = ChartHolderBinding.inflate(LayoutInflater.from(context), this)

    private var mCpmListDaily = listOf<Cpm>()
    private var mCpmListWeekly = listOf<Cpm>()
    private var mEntries = listOf<DashEntry>()
    private var mWeeklies = listOf<FullWeekly>()
    private var mChart: DTChart? = null

    interface DTChartHolderCallback

    init {
        binding.listItemHeader.setOnClickListener {
            toggleListItemVisibility(binding.listItemDetails, binding.listItemWrapper)
        }
    }

    fun initialize(callback: DTChartHolderCallback, chart: DTChart) {
        mCallback = callback
        mChart = chart
        binding.cpmBottom.addView(mChart)
        chart.title?.let { binding.chartTitleCpm.setText(it) }
        chart.subtitle?.let { binding.chartSubtitleCpm.setText(it) }
    }

    fun updateLists(
        cpmListDaily: List<Cpm>? = null,
        cpmListWeekly: List<Cpm>? = null,
        entries: List<DashEntry>? = null,
        weeklies: List<FullWeekly>? = null
    ) {
        Log.d(TAG, "updateLists")
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

    open fun update(
        cpmListDaily: List<Cpm>? = null,
        cpmListWeekly: List<Cpm>? = null,
        entries: List<DashEntry>? = null,
        weeklies: List<FullWeekly>? = null
    ) {
        cpmListDaily?.let { if (it.isNotEmpty()) mCpmListDaily = it }
        cpmListWeekly?.let { if (it.isNotEmpty()) mCpmListWeekly = it }
        entries?.let { if (it.isNotEmpty()) mEntries = it }
        weeklies?.let { if (it.isNotEmpty()) mWeeklies = it }
    }

    internal var mCpmListDaily = listOf<Cpm>()
    internal var mCpmListWeekly = listOf<Cpm>()
    internal var mEntries = listOf<DashEntry>()
    internal var mWeeklies = listOf<FullWeekly>()
}
