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

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.*
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.FragItemListBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.activity_main.ScrollableFragment
import com.wtb.dashTracker.ui.activity_main.debugLog
import com.wtb.dashTracker.ui.fragment_income.IncomeListItemFragment.IncomeItemListAdapter.Companion.PayloadField
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
abstract class ListItemFragment : Fragment(), ScrollableFragment {
    protected lateinit var binding: FragItemListBinding
    protected val recyclerView: RecyclerView
        get() = binding.itemListRecyclerView

    private val listItemFragmentCallback: ListItemFragmentCallback
        get() = requireContext() as ListItemFragmentCallback

    override val isAtTop: Boolean
        get() = (recyclerView.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() == 0

    private val allItemsAreShowing: Boolean
        get() = (recyclerView.layoutManager as? LinearLayoutManager)?.let {
            it.findLastCompletelyVisibleItemPosition() + 1 == it.itemCount
        } == true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragItemListBinding.inflate(inflater).apply {
            itemListRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)

                val height: Int =
                    (requireActivity() as MainActivity).binding.bottomAppBar.measuredHeight
                updatePadding(bottom = height + getDimen(R.dimen.margin_default).toInt())

                setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    with((requireActivity() as MainActivity)) {
                        if (!isShowingOrHidingToolbars) {
                            with(binding.fab) {
                                if (scrollY < oldScrollY && !isOrWillBeShown) {
                                    show()
                                } else if (scrollY > oldScrollY && !isOrWillBeHidden) {
                                    hide()
                                }
                            }
                        }
                    }
                }
            }
        }


        return binding.root
    }

    protected open fun onItemExpanded() {
        listItemFragmentCallback.hideToolbarsAndFab(hideToolbar = !allItemsAreShowing)
    }

    protected open fun onItemClosed() {
        listItemFragmentCallback.showToolbarsAndFab()
    }

    abstract class BaseItemListAdapter<ItemType : ListItemType, HolderType : BaseItemHolder<ItemType>>(
        diffCallback: DiffUtil.ItemCallback<ItemType>
    ) :
        ListAdapter<ItemType, HolderType>(diffCallback), ExpandableAdapter {

        override var mExpandedPosition: Int? = null

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)

            registerAdapterDataObserver(adapterObserver)
        }

        override fun onBindViewHolder(
            holder: HolderType,
            position: Int,
            payloads: List<Any>
        ) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                debugLog("onBindViewHolder | $payloads")
                holder.setExpandedFromPayloads(payloads)
            }
        }

        override fun onBindViewHolder(
            holder: HolderType,
            position: Int
        ) {
            getItem(position)?.let { holder.bind(it) }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): HolderType =
            getViewHolder(parent, viewType)

        abstract fun getViewHolder(
            parent: ViewGroup,
            viewType: Int? = null
        ): HolderType
    }

    abstract class BaseItemPagingDataAdapter<ItemType : ListItemType, HolderType : BaseItemHolder<ItemType>>(
        diffCallback: DiffUtil.ItemCallback<ItemType>
    ) :
        PagingDataAdapter<ItemType, HolderType>(diffCallback), ExpandableAdapter {

        override var mExpandedPosition: Int? = null
            set(value) {
                debugLog("Setting expanded position: $value")
                field = value
            }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)

            registerAdapterDataObserver(adapterObserver)
        }

        override fun onBindViewHolder(
            holder: HolderType,
            position: Int,
            payloads: List<Any>
        ) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                debugLog("onBindViewHolder | $payloads")
                holder.setExpandedFromPayloads(payloads)
            }
        }

        override fun onBindViewHolder(holder: HolderType, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderType =
            getViewHolder(parent, viewType)

        abstract fun getViewHolder(parent: ViewGroup, viewType: Int? = null): HolderType
    }

    /**
     * Implements collapsing-details-area for list items
     */
    @Suppress("LeakingThis")
    abstract class BaseItemHolder<T : ListItemType>(itemView: View) :
        RecyclerView.ViewHolder(itemView), OnClickListener {
        abstract val collapseArea: Array<View>
        abstract val backgroundArea: ViewGroup
        abstract val bgCard: CardView
        abstract val parentFrag: ListItemFragment

        internal lateinit var mItem: T
        protected var mIsExpanded: Boolean = false

        protected var mIsInitialized: Boolean = false
        private var mPrevPayload: List<Any>? = null

        abstract fun updateHeaderFields()
        abstract fun updateDetailsFields()

        init {
            itemView.setOnClickListener(this)
        }

        open fun bind(item: T, payloads: List<Any>? = null) {
            if (!mIsInitialized || this.mItem != item) {
                mIsInitialized = true
                this.mItem = item

                updateHeaderFields()
                updateDetailsFields()
            }

            setExpandedFromPayloads(emptyList())
        }

        override fun onClick(v: View?) {
            if (bindingAdapter is ExpandableAdapter) {

                (bindingAdapter as? ExpandableAdapter)?.let { adapter ->
                    val previouslyExpandedItemPosition: Int? = adapter.mExpandedPosition

                    if (bindingAdapterPosition == adapter.mExpandedPosition) { // item is expanded
                        parentFrag.onItemClosed()

                        adapter.mExpandedPosition = null

                        expandListItem(false)
                    } else { // item is not expanded
                        parentFrag.onItemExpanded()

                        adapter.mExpandedPosition = bindingAdapterPosition

                        expandListItem(true)

                        previouslyExpandedItemPosition?.let { pos -> // need to close previous item
                            (adapter as? RecyclerView.Adapter<*>)?.notifyItemChanged(
                                pos,
                                Pair(PayloadField.EXPANDED, false)
                            )
                        }
                    }
                }
            }
        }

        private fun expandListItem(shouldExpand: Boolean, onComplete: (() -> Unit)? = null) {
            fun updateBackground(isExpanded: Boolean) {
                if (shouldExpand) {
                    backgroundArea.transitionBackgroundTo(R.attr.colorListItemExpanded)
                } else {
                    backgroundArea.transitionBackgroundTo(R.attr.colorListItem)
                }
            }

            if (shouldExpand != mIsExpanded) {
                collapseArea.forEach {
                    it.showOrHide(
                        shouldExpand,
                        animate = false,
                        onComplete = if (shouldExpand && it == collapseArea.last()) {
                            fun() {
                                val scroller =
                                    object : LinearSmoothScroller(parentFrag.requireContext()) {

                                        override fun getVerticalSnapPreference(): Int {
                                            return SNAP_TO_START
                                        }

                                        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                                            return super.calculateSpeedPerPixel(displayMetrics)
                                        }

                                        override fun onStart() {
                                            super.onStart()
                                            debugLog("scroller onStart | target: $targetPosition")
                                        }

                                        override fun onStop() {
                                            super.onStop()

                                            debugLog("scroller onStop")
                                            onComplete?.invoke()
                                        }
                                    }.apply {
                                        targetPosition = this@BaseItemHolder.absoluteAdapterPosition
                                    }

                                parentFrag.recyclerView.layoutManager?.startSmoothScroll(scroller)

                                updateBackground(isExpanded = true)
                                mIsExpanded = true
                            }
                        } else {
                            fun() {
                                updateBackground(isExpanded = false)
                                mIsExpanded = false
                            }
                        })
                }
            }
        }

        internal fun setExpandedFromPayloads(payloads: List<Any>) {
            val shouldExpandFromPayload: Boolean? =
                (payloads.lastOrNull { it is Pair<*, *> && it.first == PayloadField.EXPANDED } as Pair<*, *>?)?.second as Boolean?

            val shouldExpand: Boolean =
                if (shouldExpandFromPayload is Boolean) {
                    shouldExpandFromPayload
                } else {
                    (bindingAdapter as? ExpandableAdapter)?.mExpandedPosition == bindingAdapterPosition
                }

            expandListItem(shouldExpand)
        }
    }

    interface ListItemFragmentCallback {
        fun hideToolbarsAndFab(hideToolbar: Boolean = true, hideFab: Boolean = true)
        fun showToolbarsAndFab(showToolbar: Boolean = true, showFab: Boolean = true)
    }
}

interface ListItemType

interface ExpandableAdapter {
    var mExpandedPosition: Int?

    val adapterObserver: RecyclerView.AdapterDataObserver
        get() = object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                debugLog("onItemRangeInserted")
                // update new expanded position
                mExpandedPosition = mExpandedPosition?.let {
                    if (it >= positionStart)
                        it + itemCount
                    else
                        it
                }
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                debugLog("onItemRangeRemoved")
                // update new expanded position
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
