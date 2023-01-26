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

package com.wtb.dashTracker.ui.dialog_confirm

import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DialogFragConfirmExpenseBreakdownBinding
import com.wtb.dashTracker.extensions.getAttributeColor
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.ui.dialog_confirm.composables.HeaderText
import com.wtb.dashTracker.ui.dialog_confirm.composables.ValueText
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_yearlies.Yearly
import com.wtb.dashTracker.ui.fragment_trends.FullWidthDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi

// TODO: selecting a purpose to edit and then cancelling deletes the purpose
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class ConfirmationDialogExpenseBreakdown(private val yearly: Yearly) : FullWidthDialogFragment() {

    private lateinit var binding: DialogFragConfirmExpenseBreakdownBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(TRANSPARENT))

        binding = DialogFragConfirmExpenseBreakdownBinding.inflate(inflater)

        binding.noButton.setOnClickListener {
            dismiss()
        }

        binding.expenseBreakdownTable.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Bottom
                    ) {
                        HeaderText(text = "Category")
                        HeaderText(text = "Business", textAlign = TextAlign.End)
                        HeaderText(text = "Total", textAlign = TextAlign.End)
                    }

                    Divider(
                        thickness = 1.dp,
                        color = Color(context.getAttributeColor(R.attr.colorPrimaryDark))
                    )

                    yearly.expenses?.let {
                        it.toSortedMap { a, b ->
                            val first: Float? = it[a]
                            val second: Float? = it[b]
                            when {
                                first == null && second == null -> {
                                    (b.name ?: "").compareTo(a.name ?: "")
                                }
                                first == null -> -1
                                second == null -> 1
                                else -> second.compareTo(first)
                            }
                        }.forEach {
                            Row(
                                modifier = Modifier,
                                verticalAlignment = Bottom
                            ) {
                                HeaderText(text = it.key.name ?: "")
                                ValueText(
                                    text = getCurrencyString(it.value * yearly.businessMileagePercent),
                                )
                                ValueText(
                                    text = getCurrencyString(it.value),
                                )
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }
}

