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

package com.wtb.dashTracker.ui.fragment_list_item_base

import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.FragItemListBinding
import com.wtb.dashTracker.ui.activity_main.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
abstract class ListItemFragment : Fragment() {
    protected lateinit var binding: FragItemListBinding
    protected val recyclerView: RecyclerView
        get() = binding.itemListRecyclerView

    abstract inner class BaseItemListAdapter<ItemType : ListItemType>(diffCallback: DiffUtil.ItemCallback<ItemType>) :
        ListAdapter<ItemType, BaseItemHolder<ItemType>>(diffCallback),
        ExpandableAdapter {

        override var mExpandedPositions: MutableSet<Int> = mutableSetOf()

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)

            registerAdapterDataObserver(adapterObserver)
        }

        override fun onBindViewHolder(
            holder: BaseItemHolder<ItemType>,
            position: Int,
            payloads: List<Any>
        ) {
            super.onBindViewHolder(holder, position, payloads)
            val itemIsExpanded = if (position in mExpandedPositions) {
                VISIBLE
            } else {
                GONE
            }
            val newPayloads = listOf(itemIsExpanded) + payloads
            getItem(position)?.let {
                holder.bind(it, newPayloads)
            }
        }

        override fun onBindViewHolder(
            holder: BaseItemHolder<ItemType>,
            position: Int
        ) {
            getItem(position)?.let { holder.bind(it) }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseItemHolder<ItemType> =
            getViewHolder(parent, viewType)

        abstract fun getViewHolder(
            parent: ViewGroup,
            viewType: Int? = null
        ): BaseItemHolder<ItemType>
    }

    abstract inner class BaseItemPagingDataAdapter<T : ListItemType>(diffCallback: DiffUtil.ItemCallback<T>) :
        PagingDataAdapter<T, BaseItemHolder<T>>(diffCallback),
        ExpandableAdapter {

        override var mExpandedPositions: MutableSet<Int> = mutableSetOf()

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)

            registerAdapterDataObserver(adapterObserver)
        }

        override fun onBindViewHolder(
            holder: BaseItemHolder<T>,
            position: Int,
            payloads: List<Any>
        ) {
            super.onBindViewHolder(holder, position, payloads)
            val itemIsExpanded = if (position in mExpandedPositions) {
                VISIBLE
            } else {
                GONE
            }
            val newPayloads = listOf(itemIsExpanded) + payloads
            getItem(position)?.let {
                holder.bind(it, newPayloads)
            }
        }

        override fun onBindViewHolder(holder: BaseItemHolder<T>, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseItemHolder<T> =
            getViewHolder(parent, viewType)

        abstract fun getViewHolder(parent: ViewGroup, viewType: Int? = null): BaseItemHolder<T>
    }

    /**
     * Implements collapsing-details-area for list items
     */
    @Suppress("LeakingThis")
    abstract inner class BaseItemHolder<T : ListItemType>(itemView: View) :
        RecyclerView.ViewHolder(itemView), OnClickListener {
        protected lateinit var item: T
        abstract val collapseArea: Array<View>
        abstract val backgroundArea: ViewGroup
        abstract val bgCard: CardView

        abstract fun bind(item: T, payloads: List<Any>? = null)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (bindingAdapter is ExpandableAdapter) {
                val adapter: ExpandableAdapter? = when (bindingAdapter) {
                    is ListItemFragment.BaseItemPagingDataAdapter<*> -> bindingAdapter as ListItemFragment.BaseItemPagingDataAdapter<*>
                    is ListItemFragment.BaseItemListAdapter<*> -> bindingAdapter as ListItemFragment.BaseItemListAdapter<*>
                    else -> null
                }

                adapter?.let {
                    if (bindingAdapterPosition in it.mExpandedPositions) {
                        it.mExpandedPositions.remove(bindingAdapterPosition)
                    } else {
                        it.mExpandedPositions.add(bindingAdapterPosition)
                        val scroller = object : LinearSmoothScroller(context) {
                            override fun getVerticalSnapPreference(): Int {
                                return SNAP_TO_START
                            }
                        }.apply {
                            targetPosition = bindingAdapterPosition
                        }
                        recyclerView.layoutManager?.startSmoothScroll(scroller)
                        (activity as MainActivity?)?.binding?.apply {
                            appBarLayout.setExpanded(false, true)
                        }
                    }
                }
            }

            bindingAdapter?.notifyItemChanged(bindingAdapterPosition)
        }

        protected fun setVisibilityFromPayloads(payloads: List<Any>?) {
            val listItemDetailsVisibility = (payloads?.let {
                if (it.isNotEmpty() && it[0] in listOf(
                        VISIBLE,
                        GONE
                    )
                ) it[0] else null
            } ?: GONE) as Int

            collapseArea.forEach { it.visibility = listItemDetailsVisibility }

            if (listItemDetailsVisibility == VISIBLE) {
                backgroundArea.setBackgroundResource(R.drawable.ripple_list_item)
                if (backgroundArea.background is RippleDrawable) {
                    (backgroundArea.background as RippleDrawable).setDrawable(
                        0,
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.bg_list_item_expanded,
                            requireContext().theme
                        )
                    )

                    val tv = TypedValue()
                    requireContext().theme.resolveAttribute(
                        R.attr.dimenListItemElevationElevated,
                        tv,
                        true
                    )

                    val elev = resources.getDimension(R.dimen.margin_half)

                    bgCard.cardElevation = elev
                }
            } else {
                backgroundArea.setBackgroundResource(R.drawable.ripple_list_item)
                if (backgroundArea.background is RippleDrawable) {
                    (backgroundArea.background as RippleDrawable).setDrawable(
                        0,
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.bg_list_item,
                            requireContext().theme
                        )
                    )
                }

                val elev = resources.getDimension(R.dimen.margin_skinny)

                bgCard.cardElevation = elev
            }
        }
    }
}

interface ListItemType

interface ExpandableAdapter {
    var mExpandedPositions: MutableSet<Int>

    val adapterObserver: RecyclerView.AdapterDataObserver
        get() = object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val oldPositions = mutableListOf<Int>().apply {
                    mExpandedPositions.forEach { this.add(it) }
                }
                val newPositions: List<Int> = oldPositions.map {
                    if (it >= positionStart)
                        it + itemCount
                    else
                        it
                }
                mExpandedPositions = mutableSetOf<Int>().apply {
                    newPositions.forEach { this.add(it) }
                }
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                val oldPositions = mutableListOf<Int>().apply {
                    mExpandedPositions.forEach { this.add(it) }
                }
                val newPositions: List<Int> = oldPositions.mapNotNull {
                    if (it >= positionStart + itemCount)
                        it - itemCount
                    else if (it >= positionStart)
                        null
                    else
                        it
                }
                mExpandedPositions = mutableSetOf<Int>().apply {
                    newPositions.forEach { this.add(it) }
                }
            }
        }
}
