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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DialogFragConfirm2ButtonBinding
import com.wtb.dashTracker.databinding.DialogFragConfirm3ButtonBinding
import com.wtb.dashTracker.extensions.getIntNotZero

open class SimpleViewConfirmationDialog :
    SimpleConfirmationDialog<TextView, Int, DialogFragConfirm2ButtonBinding, DialogFragConfirm3ButtonBinding>() {

    override val twoButtonBinding: (LayoutInflater) -> DialogFragConfirm2ButtonBinding =
        { DialogFragConfirm2ButtonBinding.inflate(it) }

    override lateinit var content: () -> Int?

    override var toolbarTwoButton: (DialogFragConfirm2ButtonBinding) -> Toolbar =
        { it.fragEntryToolbar }

    override var contentAreaTwoButton: (DialogFragConfirm2ButtonBinding) -> TextView =
        { it.theQuestion }

    override var noButtonTwoButton: (DialogFragConfirm2ButtonBinding) -> Button = { it.noButton }

    override var noDividerTwoButton: (DialogFragConfirm2ButtonBinding) -> View = { it.dividerVert }

    override var yesButton1TwoButton: (DialogFragConfirm2ButtonBinding) -> Button =
        { it.yesButton1 }

    override val threeButtonBinding: (LayoutInflater) -> DialogFragConfirm3ButtonBinding =
        { DialogFragConfirm3ButtonBinding.inflate(it) }

    override var toolbarThreeButton: (DialogFragConfirm3ButtonBinding) -> Toolbar =
        { it.fragEntryToolbar }

    override var contentAreaThreeButton: (DialogFragConfirm3ButtonBinding) -> TextView =
        { it.theQuestion }

    override var noButtonThreeButton: (DialogFragConfirm3ButtonBinding) -> Button = { it.noButton }

    override var noDividerThreeButton: (DialogFragConfirm3ButtonBinding) -> View =
        { it.dividerVert }

    override var yesButton1ThreeButton: (DialogFragConfirm3ButtonBinding) -> Button =
        { it.yesButton1 }
    override var yesButton2ThreeButton: (DialogFragConfirm3ButtonBinding) -> Button =
        { it.yesButton2 }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            content = with(getIntNotZero(ARG_TEXT_CONTENT)) {
                { this }
            }
        }
    }

    override fun setContent(contentArea: TextView, @StringRes contentValue: Int) {
        contentArea.setText(contentValue)
    }

    companion object {
        private const val ARG_TEXT_CONTENT = "dialog_text_content"

        fun newInstance(
            @StringRes text: Int?,
            requestKey: String,
            confirmId: Long? = null,
            title: String? = null,
            @StringRes posButton: Int = R.string.yes,
            posAction: LambdaWrapper? = null,
            @StringRes negButton: Int? = null,
            negAction: LambdaWrapper? = null,
            @StringRes posButton2: Int? = null,
            posAction2: LambdaWrapper? = null,
        ): SimpleViewConfirmationDialog = SimpleViewConfirmationDialog().apply {
            arguments = Bundle().apply {
                text?.let { putInt(ARG_TEXT_CONTENT, it) }
                putString(ARG_REQ_KEY, requestKey)
                confirmId?.let { putLong(ARG_CONFIRM_ID, it) }
                putString(ARG_MESSAGE, title)
                putInt(ARG_POS_TEXT, posButton)
                putParcelable(ARG_POS_ACTION, posAction)
                negButton?.let { putInt(ARG_NEG_TEXT, it) }
                putParcelable(ARG_NEG_ACTION, negAction)
                putParcelable(ARG_NEG_ACTION, negAction)
                posButton2?.let { putInt(ARG_POS_TEXT_2, it) }
                putParcelable(ARG_POS_ACTION_2, posAction2)
            }
        }
    }
}

class ConfirmDeleteDialog {
    companion object {
        fun newInstance(
            confirmId: Long? = null,
            @StringRes text: Int? = null,
        ): SimpleViewConfirmationDialog = SimpleViewConfirmationDialog.newInstance(
            text = text ?: R.string.confirm_delete,
            requestKey = ConfirmDialog.DELETE.key,
            confirmId = confirmId,
            posButton = R.string.delete
        )
    }
}

class ConfirmResetDialog {
    companion object {
        fun newInstance(
            @StringRes text: Int? = null,
        ): SimpleViewConfirmationDialog = SimpleViewConfirmationDialog.newInstance(
            text = text ?: R.string.confirm_reset,
            requestKey = ConfirmDialog.RESET.key,
            posButton = R.string.reset
        )
    }
}

class ConfirmSaveDialog {
    companion object {
        fun newInstance(@StringRes text: Int? = null): SimpleViewConfirmationDialog =
            SimpleViewConfirmationDialog.newInstance(
                text = text ?: R.string.dialog_restart,
                requestKey = ConfirmDialog.SAVE.key,
                posButton = R.string.save,
                negButton = R.string.cancel,
                posButton2 = R.string.delete,
            )
    }
}

class ConfirmRestartDialog {
    companion object {
        fun newInstance(
            @StringRes text: Int? = null,
        ): SimpleViewConfirmationDialog = SimpleViewConfirmationDialog.newInstance(
            text = text ?: R.string.dialog_restart,
            requestKey = ConfirmDialog.RESTART.key,
            posButton = R.string.restart,
            negButton = R.string.later
        )
    }
}