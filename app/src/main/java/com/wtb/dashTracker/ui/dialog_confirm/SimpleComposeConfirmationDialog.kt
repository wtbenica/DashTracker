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

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.ContactsContract.Directory.PACKAGE_NAME
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.*
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.DefaultSpacer
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogUseTrackedMiles.Companion.Content
import com.wtb.dashTracker.ui.dialog_confirm.composables.HeaderText
import com.wtb.dashTracker.ui.dialog_confirm.composables.ValueText
import com.wtb.dashTracker.ui.theme.DashTrackerTheme

open class SimpleComposeConfirmationDialog :
    SimpleConfirmationDialog<ComposeView, @Composable () -> Unit, DialogFragComposeConfirm2ButtonBinding, DialogFragComposeConfirm3ButtonBinding>() {
    override lateinit var content: () -> ((() -> Unit)?)

    override val twoButtonBinding: (LayoutInflater) -> DialogFragComposeConfirm2ButtonBinding =
        { DialogFragComposeConfirm2ButtonBinding.inflate(it) }

    override var toolbarTwoButton: (DialogFragComposeConfirm2ButtonBinding) -> Toolbar =
        { it.fragEntryToolbar }

    override var contentAreaTwoButton: (DialogFragComposeConfirm2ButtonBinding) -> ComposeView =
        { it.theQuestion }

    override var noButtonTwoButton: (DialogFragComposeConfirm2ButtonBinding) -> Button =
        { it.noButton }

    override var noDividerTwoButton: (DialogFragComposeConfirm2ButtonBinding) -> View =
        { it.dividerVert }

    override var yesButton1TwoButton: (DialogFragComposeConfirm2ButtonBinding) -> Button =
        { it.yesButton1 }

    override val threeButtonBinding: (LayoutInflater) -> DialogFragComposeConfirm3ButtonBinding =
        { DialogFragComposeConfirm3ButtonBinding.inflate(it) }

    override var toolbarThreeButton: (DialogFragComposeConfirm3ButtonBinding) -> Toolbar =
        { it.fragEntryToolbar }

    override var contentAreaThreeButton: (DialogFragComposeConfirm3ButtonBinding) -> ComposeView =
        { it.theQuestion }

    override var noButtonThreeButton: (DialogFragComposeConfirm3ButtonBinding) -> Button =
        { it.noButton }

    override var noDividerThreeButton: (DialogFragComposeConfirm3ButtonBinding) -> View =
        { it.dividerVert }

    override var yesButton1ThreeButton: (DialogFragComposeConfirm3ButtonBinding) -> Button =
        { it.yesButton1 }

    override var yesButton2ThreeButton: (DialogFragComposeConfirm3ButtonBinding) -> Button =
        { it.yesButton2 }


    override fun setContent(contentArea: ComposeView, contentValue: @Composable () -> Unit) {
        contentArea.setContent(contentValue)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            content = with(
                if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                    getParcelable(ARG_COMPOSE_CONTENT, ComposeWrapper::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    getParcelable(ARG_COMPOSE_CONTENT) as ComposeWrapper?
                }?.action
            ) { { this } }
        }
    }

    companion object {
        private const val ARG_COMPOSE_CONTENT = "dialog_compose_content"

        fun newInstance(
            content: @Composable () -> Unit,
            requestKey: String,
            confirmId: Long? = null,
            title: String? = null,
            @StringRes posButton: Int = R.string.yes,
            posAction: LambdaWrapper? = null,
            posIsDefault: Boolean = true,
            @StringRes negButton: Int? = null,
            negAction: LambdaWrapper? = null,
            @StringRes posButton2: Int? = null,
            posAction2: LambdaWrapper? = null,
            pos2IsDefault: Boolean = false
        ): SimpleComposeConfirmationDialog = SimpleComposeConfirmationDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_COMPOSE_CONTENT, ComposeWrapper(content))
                putString(ARG_REQ_KEY, requestKey)
                confirmId?.let { putLong(ARG_CONFIRM_ID, it) }
                putString(ARG_MESSAGE, title)
                putInt(ARG_POS_TEXT, posButton)
                putParcelable(ARG_POS_ACTION, posAction)
                putBoolean(ARG_POS_IS_DEFAULT, posIsDefault)
                negButton?.let { putInt(ARG_NEG_TEXT, it) }
                putParcelable(ARG_NEG_ACTION, negAction)
                posButton2?.let { putInt(ARG_POS_TEXT_2, it) }
                putParcelable(ARG_POS_ACTION_2, posAction2)
                putBoolean(ARG_POS_2_IS_DEFAULT, pos2IsDefault)
            }
        }
    }
}

class ConfirmationDialogUseTrackedMiles() {
    companion object {
        internal const val REQ_KEY_DIALOG_USE_TRACKED_MILES =
            "${PACKAGE_NAME}.req_key_use_tracked_miles"

        @Composable
        fun Content(startMileage: Int?, endMileage: Int?, distance: Int): Unit =
            Column {
                Row {
                    HeaderText(
                        stringResource(id = R.string.lbl_start_mileage_adjusted),
                        textAlign = TextAlign.Center,
                        padding = 0.dp
                    )
                    HeaderText(
                        stringResource(id = R.string.lbl_end_mileage_adjusted),
                        textAlign = TextAlign.Center,
                        padding = 0.dp
                    )
                }

                Row {
                    ValueText(
                        text = startMileage?.let {
                            stringResource(
                                id = R.string.odometer_range_int,
                                it, it + distance
                            )
                        } ?: "-",
                        textAlign = TextAlign.Center,
                        padding = dimensionResource(id = R.dimen.margin_narrow)
                    )
                    ValueText(
                        text = endMileage?.let {
                            stringResource(
                                id = R.string.odometer_range_int,
                                maxOf(0, it - distance), maxOf(distance, it)
                            )
                        } ?: "-",
                        textAlign = TextAlign.Center,
                        padding = dimensionResource(id = R.dimen.margin_narrow)
                    )
                }

                DefaultSpacer()
                
                ValueText(
                    text = "Which values would you prefer to use?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    padding = 0.dp
                )
            }

        fun newInstance(
            startMileage: Int?,
            endMileage: Int?,
            distance: Int
        ): SimpleComposeConfirmationDialog =
            SimpleComposeConfirmationDialog.newInstance(
                content = { Content(startMileage, endMileage, distance) },
                requestKey = REQ_KEY_DIALOG_USE_TRACKED_MILES,
                title = "Adjust mileage",
                posButton = R.string.lbl_start_mileage_adjusted,
                posAction = null,
                posIsDefault = true,
                negButton = R.string.cancel,
                negAction = null,
                posButton2 = R.string.lbl_end_mileage_adjusted,
                posAction2 = null,
                pos2IsDefault = true
            )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
@Preview(showBackground = true)
fun ContentPreview() {
    DashTrackerTheme() {
        Content(1, 30, 12)
    }
}