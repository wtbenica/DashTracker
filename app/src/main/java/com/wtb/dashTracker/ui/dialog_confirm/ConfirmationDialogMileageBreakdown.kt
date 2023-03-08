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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DialogFragConfirmMileageBreakdownBinding
import com.wtb.dashTracker.ui.activity_main.debugLog
import com.wtb.dashTracker.ui.dialog_confirm.composables.HeaderText
import com.wtb.dashTracker.ui.dialog_confirm.composables.ValueText
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_yearlies.Monthly
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_yearlies.Yearly
import com.wtb.dashTracker.ui.fragment_trends.FullWidthDialogFragment
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Month

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class ConfirmationDialogMileageBreakdown(private val yearly: Yearly) : FullWidthDialogFragment() {

    private lateinit var binding: DialogFragConfirmMileageBreakdownBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragConfirmMileageBreakdownBinding.inflate(inflater)

        binding.noButton.setOnClickListener {
            dismiss()
        }

        binding.mileageBreakdownTable.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DashTrackerTheme {
                    MileageBreakdown(yearly)
                }
            }
        }

        return binding.root
    }
}

@ExperimentalCoroutinesApi
@Composable
private fun MileageBreakdown(yearly: Yearly) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        debugLog("${yearly.businessMileagePercent}")
        Row(
            modifier = Modifier,
            verticalAlignment = Bottom
        ) {
            HeaderText(
                text = "Business",
                modifier = Modifier.weight(.4f),
            )
            ValueText(
                text = "${
                    stringResource(
                        R.string.float_fmt,
                        yearly.businessMileagePercent * 100
                    )
                }%",
                modifier = Modifier.weight(.3f),
            )
            ValueText(
                text = stringResource(R.string.odometer_fmt, yearly.mileage),
                modifier = Modifier.weight(.3f),
            )
        }

        Row(
            modifier = Modifier,
            verticalAlignment = Bottom
        ) {
            HeaderText(
                text = "Non-business", modifier = Modifier.weight(.4f),
            )
            ValueText(
                text = "${
                    stringResource(
                        R.string.float_fmt,
                        100f - (yearly.businessMileagePercent * 100f)
                    )
                }%",
                modifier = Modifier.weight(.3f),
            )
            val text = stringResource(R.string.odometer_fmt, yearly.nonBusinessMiles)
            debugLog("showing this $text")
            ValueText(
                text = text,
                modifier = Modifier.weight(.3f),
            )
        }

        Row(
            modifier = Modifier,
            verticalAlignment = Bottom
        ) {
            HeaderText(text = "Total miles")
            ValueText(text = stringResource(R.string.odometer_fmt, yearly.totalMiles))
        }
    }
}

@ExperimentalTextApi
@ExperimentalCoroutinesApi
@Composable
@Preview
fun MileageBreakdownPreview() {
    DashTrackerTheme {
        MileageBreakdown(yearly = Yearly(2020).apply {
            basePayAdjustment = 3.4f
            startOdometer = 1000f
            endOdometer = 1500f
            monthlies[Month.APRIL] =
                Monthly(mileage = 100f, pay = 100f, otherPay = 50f, cashTips = 50f, hours = 25f)
        })
    }
}


