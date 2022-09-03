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
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.viewbinding.ViewBinding
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DialogFragConfirm2ButtonBinding
import com.wtb.dashTracker.databinding.DialogFragConfirm3ButtonBinding
import com.wtb.dashTracker.extensions.setVisibleIfTrue
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.parcelize.Parcelize


open class ConfirmationDialog : FullWidthDialogFragment() {
    // TODO: Some of these need better names-- what is confirmId?
    @StringRes
    var text: Int? = null
    private lateinit var requestKey: String
    private var confirmId: Long? = null
    private var message: String? = null

    @StringRes
    var posButton: Int = R.string.yes
    private var posAction: (() -> Unit)? = null

    @StringRes
    var negButton: Int? = null
    private var negAction: (() -> Unit)? = null

    @StringRes
    var posButton2: Int? = null
    private var posAction2: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            fun getIntNotZero(key: String): Int? = getInt(key).let {
                if (it != 0) {
                    it
                } else {
                    null
                }
            }

            fun getLongNotZero(key: String): Long? = getLong(key).let {
                if (it != 0L) {
                    it
                } else {
                    null
                }
            }

            text = getIntNotZero(ARG_TEXT)
            getString(ARG_REQ_KEY)?.let { requestKey = it }
            confirmId = getLongNotZero(ARG_CONFIRM_ID)
            getString(ARG_MESSAGE)?.let { message = it }
            posButton = getIntNotZero(ARG_POS_TEXT) ?: R.string.yes
            posAction = getParcelable<LambdaWrapper?>(ARG_POS_ACTION)?.action
            negButton = getIntNotZero(ARG_NEG_TEXT) ?: R.string.cancel
            negAction = getParcelable<LambdaWrapper?>(ARG_NEG_ACTION)?.action
            posButton2 = getIntNotZero(ARG_POS_TEXT_2)
            posAction2 = getParcelable<LambdaWrapper?>(ARG_POS_ACTION_2)?.action
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: ViewBinding

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        if (posButton2 == null || posAction2 == null) {
            binding = DialogFragConfirm2ButtonBinding.inflate(inflater)

            binding.fragEntryToolbar.title =
                message ?: getString(R.string.confirm_dialog, getString(posButton))

            binding.theQuestion.setVisibleIfTrue(text != null)

            text?.let { binding.theQuestion.setText(it) }

            if (negButton != null) {
                val mNegAction = negAction ?: {
                    parentFragmentManager.setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to false))
                }

                binding.noButton.apply {
                    negButton?.let { setText(it) }
                    setOnClickListener {
                        dismiss()
                        mNegAction()
                    }
                }
            } else {
                binding.noButton.visibility = View.GONE
                binding.dividerVert.visibility = View.GONE
            }

            val mPosAction = posAction ?: {
                val bundlePairs = bundleOf()
                bundlePairs.putBoolean(ARG_CONFIRM, true)
                confirmId?.let { bundlePairs.putLong(ARG_EXTRA, it) }
                if (confirmId == null) {
                    parentFragmentManager.setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to true))
                } else {
                    parentFragmentManager.setFragmentResult(
                        requestKey,
                        bundleOf(ARG_CONFIRM to true, ARG_EXTRA to confirmId)
                    )
                }
            }

            binding.yesButton1.apply {
                setText(posButton)
                setOnClickListener {
                    dismiss()
                    mPosAction()
                }
            }
        } else {
            binding = DialogFragConfirm3ButtonBinding.inflate(inflater)

            binding.fragEntryToolbar.title =
                message ?: getString(R.string.confirm_dialog, getString(posButton))

            binding.theQuestion.setVisibleIfTrue(text != null)

            text?.let { binding.theQuestion.setText(it) }

            if (negButton != null) {
                val mNegAction = negAction ?: {
                    parentFragmentManager.setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to false))
                }

                binding.noButton.apply {
                    negButton?.let { setText(it) }
                    setOnClickListener {
                        dismiss()
                        mNegAction()
                    }
                }
            } else {
                binding.noButton.visibility = View.GONE
                binding.dividerVert.visibility = View.GONE
            }

            val mPosAction = posAction ?: {
                if (confirmId == null) {
                    parentFragmentManager.setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to true))
                } else {
                    parentFragmentManager.setFragmentResult(
                        requestKey,
                        bundleOf(ARG_CONFIRM to true, ARG_EXTRA to confirmId)
                    )
                }
            }

            binding.yesButton1.apply {
                setText(posButton)
                setOnClickListener {
                    dismiss()
                    mPosAction()
                }
            }

            val mPosAction2: (() -> Unit)? = posAction2

            binding.yesButton2.apply {
                posButton2?.let { setText(it) }
                mPosAction2?.let {
                    setOnClickListener {
                        dismiss()
                        it()

                        if (confirmId == null) {
                            parentFragmentManager.setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to true))
                        } else {
                            parentFragmentManager.setFragmentResult(
                                requestKey,
                                bundleOf(ARG_CONFIRM to true, ARG_EXTRA to confirmId)
                            )
                        }
                    }
                }
            }
        }

        return binding.root
    }

    companion object {
        const val ARG_CONFIRM = "confirm"
        const val ARG_EXTRA = "extra"
        private const val ARG_TEXT = "dialog_text"
        private const val ARG_REQ_KEY = "request_key"
        private const val ARG_CONFIRM_ID = "confirm_id"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POS_TEXT = "pos_btn_text"
        private const val ARG_POS_ACTION = "pos_action"
        private const val ARG_NEG_TEXT = "neg_btn_txt"
        private const val ARG_NEG_ACTION = "neg_action"
        private const val ARG_POS_TEXT_2 = "pos_btn_text_2"
        private const val ARG_POS_ACTION_2 = "pos_action_2"

        fun newInstance(
            @StringRes text: Int?,
            requestKey: String,
            confirmId: Long? = null,
            title: String? = null,
            @StringRes posButton: Int = R.string.yes,
            posAction: LambdaWrapper? = null,
            @StringRes negButton: Int? = null,
            negAction: (LambdaWrapper)? = null,
            @StringRes posButton2: Int? = null,
            posAction2: (LambdaWrapper)? = null,
        ) = ConfirmationDialog().apply {
            arguments = Bundle().apply {
                text?.let { putInt(ARG_TEXT, it) }
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

@Parcelize
data class LambdaWrapper(val action: () -> Unit) : Parcelable

enum class ConfirmType(val key: String) {
    DELETE("confirmDelete"),
    RESET("confirmReset"),
    SAVE("confirmSave")
}

class ConfirmDeleteDialog {
    companion object {
        fun newInstance(
            confirmId: Long? = null,
            @StringRes text: Int? = null,
        ) = ConfirmationDialog.newInstance(
            text = text ?: R.string.confirm_delete,
            requestKey = ConfirmType.DELETE.key,
            confirmId = confirmId,
            posButton = R.string.delete
        )
    }
}

class ConfirmResetDialog {
    companion object {
        fun newInstance(
            @StringRes text: Int? = null,
        ) = ConfirmationDialog.newInstance(
            text = text ?: R.string.confirm_reset,
            requestKey = ConfirmType.RESET.key,
            posButton = R.string.reset
        )
    }
}

class ConfirmSaveDialog {
    companion object {
        fun newInstance(@StringRes text: Int? = null) =
            ConfirmationDialog.newInstance(
                text = text,
                requestKey = ConfirmType.SAVE.key,
                posButton = R.string.save,
                negButton = R.string.no
            )
    }
}