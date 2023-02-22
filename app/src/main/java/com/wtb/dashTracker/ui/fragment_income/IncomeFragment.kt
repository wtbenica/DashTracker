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

package com.wtb.dashTracker.ui.fragment_income

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.FragIncomeBinding
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.expand
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.fragment_list_item_base.IncomeListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.IncomeListItemFragment.Companion.REQ_KEY_INCOME_LIST_ITEM_SELECTED
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_dailies.EntryListFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_weeklies.WeeklyListFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_yearlies.YearlyListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A [Fragment] that contains a [com.google.android.material.tabs.TabLayout] with [EntryListFragment], [WeeklyListFragment], and
 * [YearlyListFragment] tabs. It contains an option to set [DeductionType] for calculating
 * expenses in each of the tabs.
 * Use the [IncomeFragment.newInstance] factory method to create an instance of this fragment.
 */
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class IncomeFragment : Fragment(), IncomeListItemFragment.IncomeListItemFragmentCallback,
    WeeklyListFragment.WeeklyListFragmentCallback,
    EntryListFragment.EntryListFragmentCallback,
    YearlyListFragment.YearlyListFragmentCallback {
    private lateinit var callback: IncomeFragmentCallback
    private var cpmButtonText: String? = null
    private lateinit var binding: FragIncomeBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = (context as IncomeFragmentCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener(
            REQ_KEY_INCOME_LIST_ITEM_SELECTED,
            this
        ) { string, bundle ->
            hideOptionsMenu()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_income, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragIncomeBinding.bind(view)
        val tabLayout = binding.fragIncomeTabLayout
        val viewPager = binding.fragIncomeViewPager
        viewPager.adapter = IncomePagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                1 -> getString(R.string.frag_title_weekly_list)
                2 -> getString(R.string.frag_title_yearly_list)
                else -> getString(R.string.frag_title_daily_list)
            }
        }.attach()

        binding.topStuff.apply {
            setOnClickListener {
                toggleOptionsMenuOpen()
            }
        }

        binding.noneButton.isChecked = true

        binding.buttonGroupDeductionType.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.none_button -> {
                        callback.setDeductionType(DeductionType.NONE)
                    }
                    R.id.gas_button -> {
                        callback.setDeductionType(DeductionType.GAS_ONLY)
                    }
                    R.id.actual_button -> {
                        callback.setDeductionType(DeductionType.ALL_EXPENSES)
                    }
                    R.id.standard_button -> {
                        callback.setDeductionType(DeductionType.IRS_STD)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                callback.deductionType.collectLatest {
                    cpmButtonText = getString(R.string.lbl_cpm) +
                            if (it == DeductionType.NONE) {
                                ""
                            } else {
                                ": " + it.text
                            }
                    if (!binding.filterBoxCollapsableArea.isVisible) {
                        binding.selectCpm.text = this@IncomeFragment.cpmButtonText
                    }
                }
            }
        }
    }

    private fun toggleOptionsMenuOpen() {
        if (binding.filterBoxCollapsableArea.isVisible) {
            hideOptionsMenu()
        } else {
            showOptionsMenu()
        }
    }

    private fun showOptionsMenu() {
        binding.filterBoxCollapsableArea.expand()
        binding.expandArrow.setImageResource(R.drawable.ic_arrow_collapse)
        binding.selectCpm.visibility = GONE
    }

    private fun hideOptionsMenu() {
        binding.filterBoxCollapsableArea.collapse()
        binding.expandArrow.setImageResource(R.drawable.ic_arrow_expand)
        binding.selectCpm.visibility = VISIBLE
        binding.selectCpm.text = cpmButtonText
    }

    @ExperimentalCoroutinesApi
    inner class IncomePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment =
            when (position) {
                1 -> WeeklyListFragment()
                2 -> YearlyListFragment()
                else -> EntryListFragment()
            }
    }

    interface IncomeFragmentCallback {
        fun setDeductionType(dType: DeductionType)
        val deductionType: StateFlow<DeductionType>
        fun hideStuff()
        fun showStuff()
    }

    companion object {
        private const val NUM_PAGES = 3

        @JvmStatic
        fun newInstance(): IncomeFragment = IncomeFragment()
    }

    override fun onItemSelected() {
        if (binding.filterBoxCollapsableArea.isVisible) {
            hideOptionsMenu()
        }
    }
}