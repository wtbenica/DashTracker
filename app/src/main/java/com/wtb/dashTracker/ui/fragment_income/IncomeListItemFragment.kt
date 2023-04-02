/*
 * Copyright 2023 Wesley T. Benica
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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.extensions.getCpmIrsStdString
import com.wtb.dashTracker.extensions.getCpmString
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.fragment_list_item_base.ExpandableAdapter
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Contains controls for income fragment options menu
 */
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
abstract class IncomeListItemFragment<T : IncomeListItemFragment.IncomeListItemType, ExpenseValues : Any, HolderType : IncomeListItemFragment.IncomeItemHolder<T, ExpenseValues>> :
    ListItemFragment() {

    abstract val entryAdapter: RecyclerView.Adapter<HolderType>

    protected var callback: IncomeFragment.IncomeFragmentCallback? = null

    protected var deductionType: DeductionType = DeductionType.NONE

    override fun onAttach(context: Context) {
        super.onAttach(context)

        callback = context as IncomeFragment.IncomeFragmentCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        binding.itemListRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState != SCROLL_STATE_IDLE) {
                    parentFragmentManager.setFragmentResult(
                        REQ_KEY_INCOME_LIST_ITEM_SELECTED,
                        bundleOf()
                    )
                }
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.itemListRecyclerView.adapter = entryAdapter

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                callback?.deductionType?.collectLatest {
                    if (deductionType != it) {
                        deductionType = it
                        entryAdapter.notifyItemRangeChanged(
                            0,
                            entryAdapter.itemCount,
                            Pair(IncomeItemListAdapter.Companion.PayloadField.DEDUCTION, it)
                        )
                    }
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onItemExpanded() {
        super.onItemExpanded()
        parentFragmentManager.setFragmentResult(REQ_KEY_INCOME_LIST_ITEM_SELECTED, bundleOf())
    }

    protected fun formatCpm(cpm: Float): String =
        if (deductionType == DeductionType.IRS_STD)
            getCpmIrsStdString(cpm)
        else
            getCpmString(cpm)

    abstract class IncomeItemPagingDataAdapter<T : IncomeListItemType, ExpenseType : Any, HolderType : IncomeItemHolder<T, ExpenseType>>(
        diffCallback: DiffUtil.ItemCallback<T>
    ) : BaseItemPagingDataAdapter<T, HolderType>(diffCallback), ExpandableAdapter {
        override fun onBindViewHolder(
            holder: HolderType,
            position: Int,
            payloads: List<Any>
        ) {
            if (payloads.isNotEmpty()) {
                holder.updateDeductionType()
            }

            super.onBindViewHolder(holder, position, payloads)
        }
    }

    abstract class IncomeItemListAdapter<T : IncomeListItemType, ExpenseType : Any, HolderType : IncomeItemHolder<T, ExpenseType>>(
        diffCallback: DiffUtil.ItemCallback<T>
    ) : BaseItemListAdapter<T, HolderType>(diffCallback), ExpandableAdapter {
        override fun onBindViewHolder(
            holder: HolderType,
            position: Int,
            payloads: List<Any>
        ) {
            if (payloads.isNotEmpty()) {
                holder.updateDeductionType()
            }

            super.onBindViewHolder(holder, position, payloads)
        }

        companion object {
            enum class PayloadField {
                EXPANDED, DEDUCTION
            }
        }
    }

    abstract class IncomeItemHolder<T : IncomeListItemType, ExpenseValues : Any>(itemView: View) :
        BaseItemHolder<T>(itemView), View.OnClickListener {

        protected val deductionType: DeductionType
            get() = (parentFrag as? IncomeListItemFragment<*, *, *>?)?.deductionType
                ?: DeductionType.NONE

        protected val shouldShow: Boolean
            get() = deductionType != DeductionType.NONE

        fun updateDeductionType() {
            launchObservers()

            updateExpenseFieldVisibilities()
            updateExpenseFieldValues()
        }

        private fun updateExpenseValues(values: ExpenseValues) {
            expenseValues = values

            updateExpenseFieldVisibilities()
            updateExpenseFieldValues()
        }

        protected abstract var expenseValues: ExpenseValues

        protected abstract suspend fun getExpenseValues(): ExpenseValues

        protected abstract fun updateExpenseFieldValues()

        protected abstract fun updateExpenseFieldVisibilities()

        override fun bind(item: T, payloads: List<Any>?) {
            if (!mIsInitialized || this.mItem != item) {
                super.bind(item, payloads)

                launchObservers()
            }

            updateExpenseFieldVisibilities()
            updateExpenseFieldValues()
        }

        private fun launchObservers() {
            CoroutineScope(Dispatchers.Default).launch {
                withContext(Dispatchers.Default) {
                    getExpenseValues()
                }.let { ev: ExpenseValues ->
                    (parentFrag.requireContext() as MainActivity).runOnUiThread {
                        updateExpenseValues(ev)
                    }
                }
            }
        }
    }

    interface IncomeListItemType : ListItemType

    interface IncomeListItemFragmentCallback {
        fun onItemSelected()
    }

    companion object {
        internal const val REQ_KEY_INCOME_LIST_ITEM_SELECTED =
            "${BuildConfig.APPLICATION_ID}.income_item_list_selected"
    }
}