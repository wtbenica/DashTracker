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

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.expand
import com.wtb.dashTracker.extensions.transitionBackground

abstract class ListItemFragment : Fragment()

interface ListItemType

fun toggleListItemVisibility(collapseArea: View, backgroundArea: View, isVisible: Boolean? = null) {
    if (collapseArea.visibility == VISIBLE || isVisible == false) {
        collapseArea.collapse()
        backgroundArea.transitionBackground(
            R.attr.colorListItemExpanded,
            R.attr.colorListItem
        )
    } else {
        collapseArea.expand()
        backgroundArea.transitionBackground(
            R.attr.colorListItem,
            R.attr.colorListItemExpanded
        )
    }
}

// Just contains boilerplate--nothing special
abstract class BaseItemAdapter<T : ListItemType>(diffCallback: DiffUtil.ItemCallback<T>) :
    PagingDataAdapter<T, BaseItemHolder<T>>(diffCallback) {

    var mExpandedPosition = mutableSetOf<Int>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val oldPositions = mutableListOf<Int>().apply {
                    mExpandedPosition.forEach { this.add(it) }
                }
                val newPositions: List<Int> = oldPositions.map {
                    if (it >= positionStart)
                        it + itemCount
                    else
                        it
                }
                mExpandedPosition = mutableSetOf<Int>().apply {
                    newPositions.forEach { this.add(it) }
                }
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                val oldPositions = mutableListOf<Int>().apply {
                    mExpandedPosition.forEach { this.add(it) }
                }
                val newPositions: List<Int> = oldPositions.mapNotNull {
                    if (it >= positionStart + itemCount)
                        it - itemCount
                    else if (it >= positionStart)
                        null
                    else
                        it
                }
                mExpandedPosition = mutableSetOf<Int>().apply {
                    newPositions.forEach { this.add(it) }
                }
            }
        })
    }

    override fun onBindViewHolder(
        holder: BaseItemHolder<T>,
        position: Int,
        payloads: List<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)
        val tt = listOf(
            if (position in mExpandedPosition) {
                VISIBLE
            } else {
                GONE
            }
        ) + payloads
        getItem(position)?.let {
            holder.bind(it, tt)
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
abstract class BaseItemHolder<T : ListItemType>(itemView: View) :
    RecyclerView.ViewHolder(itemView), View.OnClickListener {
    protected lateinit var item: T
    abstract val collapseArea: ViewGroup
    abstract val backgroundArea: ViewGroup

    abstract fun bind(item: T, payloads: List<Any>? = null)

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (bindingAdapter is BaseItemAdapter<*>) {
            if (collapseArea.visibility == VISIBLE) {
                (bindingAdapter as BaseItemAdapter<*>).mExpandedPosition.remove(
                    bindingAdapterPosition
                )
            } else {
                (bindingAdapter as BaseItemAdapter<*>).mExpandedPosition.add(bindingAdapterPosition)
            }
        }
        bindingAdapter?.notifyItemChanged(bindingAdapterPosition)
    }

    protected fun setPayloadVisibility(payloads: List<Any>?) {
        val listItemDetailsVisibility = (payloads?.let {
            if (it.size >= 1 && it[0] in listOf(
                    VISIBLE,
                    GONE
                )
            ) it[0] else null
        } ?: GONE) as Int

        backgroundArea.setBackgroundResource(
            if (listItemDetailsVisibility == VISIBLE)
                R.drawable.bg_list_item_expanded
            else
                R.drawable.bg_list_item
        )

        collapseArea.visibility = listItemDetailsVisibility
    }
}