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

package com.wtb.dashTracker.ui.fragment_expenses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.FragExpenseBinding
import com.wtb.dashTracker.ui.activity_main.ScrollableFragment
import com.wtb.dashTracker.ui.fragment_expenses.fragment_monthly_expenses.MonthlyExpenseListFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_dailies.EntryListFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_dailies.EntryListFragment.EntryListFragmentCallback
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_weeklies.WeeklyListFragment.WeeklyListFragmentCallback
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_yearlies.YearlyListFragment.YearlyListFragmentCallback
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * A [Fragment] that contains a [com.google.android.material.tabs.TabLayout] with [ExpenseListFragment] and [MonthlyExpenseListFragment]
 * tabs.
 * Use the [ExpenseFragment.newInstance] factory method to create an instance of this fragment.
 */
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class ExpenseFragment : Fragment(),
    WeeklyListFragmentCallback,
    EntryListFragmentCallback,
    YearlyListFragmentCallback,
    ScrollableFragment {

    // Public variables
    override val isAtTop: Boolean
        get() = currentFragment?.isAtTop ?: false

    // Private variables
    private lateinit var binding: FragExpenseBinding

    /**
     * Fragments - stores references to each fragment of the tablayout
     */
    private val fragments: MutableMap<Int, ListItemFragment> = mutableMapOf()

    /**
     * Current fragment - set by OnTabSelected using [fragments] and [TabLayout.Tab.position]
     */
    private var currentFragment: ScrollableFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragExpenseBinding.bind(view)
        val tabLayout = binding.fragExpenseTabLayout
        val viewPager = binding.fragExpenseViewPager
        viewPager.adapter = ExpensePagerAdapter(this)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFragment = fragments[tab!!.position]
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Do nothing
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Do nothing
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = (ExpensePages.values().getOrNull(position) ?: ExpensePages.DAILY).tabLabel
        }.attach()
    }

    companion object {
        private val NUM_PAGES = ExpensePages.values().size

        enum class ExpensePages(val tabLabel: String, val fragment: () -> ExpenseListItemFragment) {
            DAILY("Daily", { ExpenseListFragment() }),
            MONTHLY("Monthly", { MonthlyExpenseListFragment() }),
        }

        @JvmStatic
        fun newInstance(): ExpenseFragment = ExpenseFragment()
    }

    @ExperimentalCoroutinesApi
    inner class ExpensePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment =
            (ExpensePages.values().getOrNull(position)?.fragment?.invoke()
                ?: EntryListFragment())
                .also {
                    fragments[position] = it
                }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
abstract class ExpenseListItemFragment : ListItemFragment()