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

package com.wtb.dashTracker.ui.fragment_base_list

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.expand
import com.wtb.dashTracker.extensions.transitionBackground

class BaseListFragment : Fragment()

interface ListItemType

fun toggleListItemVisibility(collapseArea: View, backgroundArea: View) {
    if (collapseArea.visibility == View.VISIBLE) {
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
    override fun onBindViewHolder(
        holder: BaseItemHolder<T>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)
        getItem(position)?.let { holder.bind(it, payloads) }
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

    abstract fun bind(item: T, payloads: MutableList<Any>? = null)

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        toggleListItemVisibility(collapseArea, backgroundArea)
    }

    protected fun setPayloadVisibility(payloads: MutableList<Any>?) {
        val listItemDetailsVisibility = (payloads?.let {
            if (it.size == 1 && it[0] in listOf(
                    View.VISIBLE,
                    View.GONE
                )
            ) it[0] else null
        } ?: View.GONE) as Int

        backgroundArea.setBackgroundResource(
            if (listItemDetailsVisibility == View.VISIBLE)
                R.drawable.bg_list_item_expanded
            else
                R.drawable.bg_list_item
        )

        collapseArea.visibility = listItemDetailsVisibility
    }
}