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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.*
import com.wtb.dashTracker.ui.dialog_confirm.composables.HeaderText
import com.wtb.dashTracker.ui.dialog_confirm.composables.ValueText

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
            ) {
                { this }
            }
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
            @StringRes negButton: Int? = null,
            negAction: (LambdaWrapper)? = null,
            @StringRes posButton2: Int? = null,
            posAction2: (LambdaWrapper)? = null,
        ): SimpleComposeConfirmationDialog = SimpleComposeConfirmationDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_COMPOSE_CONTENT, ComposeWrapper(content))
                putString(ARG_REQ_KEY, requestKey)
                confirmId?.let { putLong(ARG_CONFIRM_ID, it) }
                putString(ARG_MESSAGE, title)
                putInt(ARG_POS_TEXT, posButton)
                putParcelable(ARG_POS_ACTION, posAction)
                negButton?.let { putInt(ARG_NEG_TEXT, it) }
                putParcelable(ARG_NEG_ACTION, negAction)
                posButton2?.let { putInt(ARG_POS_TEXT_2, it) }
                putParcelable(ARG_POS_ACTION_2, posAction2)
            }
        }
    }
}

class ConfirmationDialogUseTrackedMiles() {
    companion object {
        internal const val REQ_KEY_DIALOG_USE_TRACKED_MILES =
            "${PACKAGE_NAME}.req_key_use_tracked_miles"

        @Composable
        fun Content(startMileage: Int?, endMileage: Int?): Unit = Column {
            Row {
                HeaderText(
                    stringResource(id = R.string.lbl_start_mileage),
                    textAlign = TextAlign.Center,
                    padding = 0.dp
                )
                HeaderText(
                    stringResource(id = R.string.lbl_end_mileage),
                    textAlign = TextAlign.Center,
                    padding = 0.dp
                )
            }

            Row {
                ValueText(
                    startMileage?.toString() ?: "",
                    textAlign = TextAlign.Center,
                    padding = dimensionResource(id = R.dimen.margin_narrow)
                )
                ValueText(
                    endMileage?.toString() ?: "",
                    textAlign = TextAlign.Center,
                    padding = dimensionResource(id = R.dimen.margin_narrow)
                )
            }

            ValueText("Which value would you like to adjust?", textAlign = TextAlign.Start)
        }

        fun newInstance(startMileage: Int?, endMileage: Int?): SimpleComposeConfirmationDialog =
            SimpleComposeConfirmationDialog.newInstance(
                content = { Content(startMileage, endMileage) },
                requestKey = REQ_KEY_DIALOG_USE_TRACKED_MILES,
                title = "Adjust mileage",
                posButton = R.string.lbl_start_mileage,
                posAction = null,
                negButton = R.string.cancel,
                negAction = null,
                posButton2 = R.string.lbl_end_mileage,
                posAction2 = null,
            )
    }
}