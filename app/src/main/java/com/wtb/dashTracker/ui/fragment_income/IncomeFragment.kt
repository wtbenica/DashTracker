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
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.FragmentIncomeBinding
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.expand
import com.wtb.dashTracker.repository.DeductionType
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
@ExperimentalCoroutinesApi
class IncomeFragment : Fragment(), WeeklyListFragment.WeeklyListFragmentCallback,
    EntryListFragment.EntryListFragmentCallback,
    YearlyListFragment.YearlyListFragmentCallback {
    private lateinit var callback: IncomeFragmentCallback
    private var cpmButtonText: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = (context as IncomeFragmentCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_income, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentIncomeBinding.bind(view)
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

        val btnGroup = binding.filterBox
        binding.filterBtn.setOnClickListener {
            if (btnGroup.isVisible) {
                btnGroup.collapse()
                binding.filterBtn.setIconResource(R.drawable.ic_arrow_expand)
                binding.filterBtn.text = cpmButtonText
            } else {
                btnGroup.expand()
                binding.filterBtn.setIconResource(R.drawable.ic_arrow_collapse)
                binding.filterBtn.text = null
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
                    cpmButtonText =
                        if (it == DeductionType.NONE) {
                            "CPM"
                        } else {
                            "CPM: " + it.text
                        }
                    if (!btnGroup.isVisible) {
                        binding.filterBtn.text = this@IncomeFragment.cpmButtonText
                    }
                }
            }
        }
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
    }

    companion object {
        private const val NUM_PAGES = 3

        @JvmStatic
        fun newInstance() = IncomeFragment()
    }
}