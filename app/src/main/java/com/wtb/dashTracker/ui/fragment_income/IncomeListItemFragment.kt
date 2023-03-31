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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.fragment_list_item_base.ExpandableAdapter
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Contains controls for income fragment options menu
 */
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
abstract class IncomeListItemFragment : ListItemFragment() {

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

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onItemExpanded() {
        super.onItemExpanded()
        parentFragmentManager.setFragmentResult(REQ_KEY_INCOME_LIST_ITEM_SELECTED, bundleOf())
    }

    abstract inner class IncomeItemPagingDataAdapter<T : IncomeListItemType, HolderType : IncomeItemHolder<T>>(diffCallback: DiffUtil.ItemCallback<T>) :
        BaseItemPagingDataAdapter<T, HolderType>(diffCallback), ExpandableAdapter {
        override fun onBindViewHolder(
            holder: HolderType,
            position: Int,
            payloads: List<Any>
        ) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                val tDeductionType: DeductionType? =
                    (payloads.lastOrNull { it is Pair<*, *> && it.first == "Deduction" } as Pair<*, *>?)?.second as DeductionType?
                holder.updateExpenseFieldsVisibility(tDeductionType)
                holder.setExpandedFromPayloads(payloads)
            }
        }
    }

    @Suppress("LeakingThis")
    abstract inner class IncomeItemHolder<T : IncomeListItemType>(itemView: View) :
        BaseItemHolder<T>(itemView), View.OnClickListener {
        override fun bind(item: T, payloads: List<Any>?) {
            super.bind(item, payloads)

            updateExpenseFieldsVisibility()
        }

        abstract fun updateExpenseFieldsVisibility(tDeductionType: DeductionType? = null)
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