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

package com.wtb.dashTracker.ui.frag_income

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.FragmentIncomeBinding
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.expand
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.DeductionTypeViewModel
import com.wtb.dashTracker.ui.entry_list.EntryListFragment
import com.wtb.dashTracker.ui.weekly_list.WeeklyListFragment
import com.wtb.dashTracker.ui.yearly_list.YearlyListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [IncomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@ExperimentalCoroutinesApi
class IncomeFragment : Fragment(), WeeklyListFragment.WeeklyListFragmentCallback,
    EntryListFragment.EntryListFragmentCallback,
    YearlyListFragment.YearlyListFragmentCallback {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var callback: IncomeFragmentCallback
    private val deductionTypeViewModel: DeductionTypeViewModel by viewModels()
    override val deductionType: StateFlow<DeductionType>
        get() = deductionTypeViewModel.deductionType
    override var standardMileageDeductions: Map<Int, Float> = emptyMap()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = (context as IncomeFragmentCallback)
    }

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
            if (btnGroup.visibility == VISIBLE) {
                btnGroup.collapse()
                binding.filterBtn.setIconResource(R.drawable.ic_arrow_expand)
                binding.filterBtn.text = when (binding.buttonGroupDeductionType.checkedButtonId) {
                    R.id.gas_button -> "CPM: " + DeductionType.GAS_ONLY.text
                    R.id.actual_button -> "CPM: " + DeductionType.ALL_EXPENSES.text
                    R.id.standard_button -> "CPM: " + DeductionType.STD_DEDUCTION.text
                    else -> "CPM"
                }
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
                        callback.setDeductionType(DeductionType.STD_DEDUCTION)
                    }
                }
            } else {
                callback.setDeductionType(DeductionType.NONE)
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

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment IncomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            IncomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}