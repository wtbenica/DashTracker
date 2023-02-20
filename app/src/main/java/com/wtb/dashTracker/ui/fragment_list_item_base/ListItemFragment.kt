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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.*
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragItemListBinding.inflate(inflater)
        binding.itemListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)

            val height = (requireActivity() as MainActivity).binding.bottomAppBar.measuredHeight
            updatePadding(bottom = height + 16)

            this.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                if (scrollY < oldScrollY) {
                    (requireActivity() as MainActivity).binding.fab.show()
                } else if (scrollY > oldScrollY) {
                    (requireActivity() as MainActivity).binding.fab.hide()
                }
            }
        }


        return binding.root
    }

    abstract inner class BaseItemListAdapter<ItemType : ListItemType>(diffCallback: DiffUtil.ItemCallback<ItemType>) :
        ListAdapter<ItemType, BaseItemHolder<ItemType>>(diffCallback),
        ExpandableAdapter {

        override var mExpandedPosition: Int? = null

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
            val itemIsExpanded = if (position == mExpandedPosition) {
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

        override var mExpandedPosition: Int? = null

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
            val itemIsExpanded = if (position == mExpandedPosition) {
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
                var prev: Int? = null

                val adapter: ExpandableAdapter? = when (bindingAdapter) {
                    is ListItemFragment.BaseItemPagingDataAdapter<*> -> bindingAdapter as ListItemFragment.BaseItemPagingDataAdapter<*>
                    is ListItemFragment.BaseItemListAdapter<*> -> bindingAdapter as ListItemFragment.BaseItemListAdapter<*>
                    else -> null
                }

                adapter?.let {
                    prev = it.mExpandedPosition
                    if (bindingAdapterPosition == it.mExpandedPosition) {
                        onItemClosed()

                        it.mExpandedPosition = null
                    } else {
                        onItemExpanded()

                        it.mExpandedPosition = (bindingAdapterPosition)
                        val scroller = object : LinearSmoothScroller(context) {
                            override fun getVerticalSnapPreference(): Int {
                                return SNAP_TO_START
                            }
                        }.apply {
                            targetPosition = bindingAdapterPosition
                        }

                        recyclerView.layoutManager?.startSmoothScroll(scroller)
                    }
                }

                prev?.let { bindingAdapter?.notifyItemChanged(it) }
                bindingAdapter?.notifyItemChanged(bindingAdapterPosition)
            }
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

                    val elev = resources.getDimension(R.dimen.margin_narrow)

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

                bgCard.cardElevation = 0f
            }
        }
    }

    protected open fun onItemExpanded() {
        (requireContext() as MainActivity).hideStuff()
    }

    protected open fun onItemClosed() {
        (requireContext() as MainActivity).showStuff()
    }
}

interface ListItemType

interface ExpandableAdapter {
    var mExpandedPosition: Int?

    val adapterObserver: RecyclerView.AdapterDataObserver
        get() = object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                mExpandedPosition = mExpandedPosition?.let {
                    if (it >= positionStart)
                        it + itemCount
                    else
                        it
                }
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                mExpandedPosition = mExpandedPosition?.let {
                    if (it >= positionStart + itemCount)
                        it - itemCount
                    else if (it >= positionStart)
                        null
                    else
                        it
                }
            }
        }
}
