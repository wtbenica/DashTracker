/*
 * Copyright 2023 Wesley T. Benica
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

import android.graphics.Typeface.*
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.viewbinding.ViewBinding
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.getAttrColor
import com.wtb.dashTracker.extensions.getIntNotZero
import com.wtb.dashTracker.extensions.getLongNotZero
import com.wtb.dashTracker.extensions.setVisibleIfTrue
import com.wtb.dashTracker.ui.fragment_trends.FullWidthDialogFragment
import kotlinx.parcelize.Parcelize

@Parcelize
data class LambdaWrapper(val action: () -> Unit) : Parcelable

@Parcelize
data class ComposeWrapper<T : SimpleConfirmationDialog<*, *, *, *>>(
    val action: (T) -> @Composable () -> Unit
) : Parcelable

enum class ConfirmDialog(val key: String) {
    DELETE("confirmDelete"),
    RESET("confirmReset"),
    SAVE("confirmSave"),
    RESTART("confirmRestart")
}

abstract class SimpleConfirmationDialog<ContentArea : View, ContentType : Any, TwoButtonBinding : ViewBinding, ThreeButtonBinding : ViewBinding> :
    FullWidthDialogFragment() {
    private lateinit var binding: ViewBinding

    internal lateinit var requestKey: String
    internal var confirmId: Long? = null
    private var message: String? = null

    @StringRes
    var posButton: Int? = null
    private var posAction: (() -> Unit)? = null
    private var posButtonIsDefault = true
    private var pos2ButtonIsDefault = false

    @StringRes
    var negButton: Int? = null
    private var negAction: (() -> Unit)? = null

    @StringRes
    var posButton2: Int? = null
    private var posAction2: (() -> Unit)? = null

    abstract val twoButtonBinding: (LayoutInflater) -> TwoButtonBinding
    abstract val threeButtonBinding: (LayoutInflater) -> ThreeButtonBinding

    abstract var content: () -> ContentType?

    abstract var toolbarTwoButton: (TwoButtonBinding) -> Toolbar
    abstract var contentAreaTwoButton: (TwoButtonBinding) -> ContentArea
    abstract var noButtonTwoButton: (TwoButtonBinding) -> Button
    abstract var noDividerTwoButton: (TwoButtonBinding) -> View
    abstract var yesButton1TwoButton: (TwoButtonBinding) -> Button

    abstract var toolbarThreeButton: (ThreeButtonBinding) -> Toolbar
    abstract var contentAreaThreeButton: (ThreeButtonBinding) -> ContentArea
    abstract var noButtonThreeButton: (ThreeButtonBinding) -> Button
    abstract var noDividerThreeButton: (ThreeButtonBinding) -> View
    abstract var yesButton1ThreeButton: (ThreeButtonBinding) -> Button
    abstract var yesButton2ThreeButton: (ThreeButtonBinding) -> Button

    abstract fun setContent(contentArea: ContentArea, contentValue: ContentType)

    private lateinit var mTwoButtonBinding: TwoButtonBinding
    private lateinit var mThreeButtonBinding: ThreeButtonBinding
    private var mContent: ContentType? = null
    private lateinit var mToolbar: Toolbar
    private lateinit var mContentArea1: ContentArea
    private lateinit var mNoButton: Button
    private lateinit var mNoDivider: View
    private lateinit var mYesButton1: Button
    private lateinit var mYesButton2: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            getString(ARG_REQ_KEY)?.let { requestKey = it }

            confirmId = getLongNotZero(ARG_CONFIRM_ID)

            getString(ARG_MESSAGE)?.let { message = it }

            posButton = getIntNotZero(ARG_POS_TEXT)
            posAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelable(ARG_POS_ACTION, LambdaWrapper::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelable(ARG_POS_ACTION)
            }?.action
            posButtonIsDefault = getBoolean(ARG_POS_IS_DEFAULT, true)

            negButton = getIntNotZero(ARG_NEG_TEXT) ?: R.string.cancel
            negAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelable(ARG_NEG_ACTION, LambdaWrapper::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelable(ARG_NEG_ACTION)
            }?.action

            posButton2 = getIntNotZero(ARG_POS_TEXT_2)
            posAction2 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelable(ARG_POS_ACTION_2, LambdaWrapper::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelable(ARG_POS_ACTION_2)
            }?.action
            pos2ButtonIsDefault = getBoolean(ARG_POS_2_IS_DEFAULT, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        if (posButton2 == null) { // use 2 button dialog
            mTwoButtonBinding = twoButtonBinding(inflater)
            mToolbar = toolbarTwoButton(mTwoButtonBinding)
            mContentArea1 = contentAreaTwoButton(mTwoButtonBinding)
            mContent = content()
            mNoButton = noButtonTwoButton(mTwoButtonBinding)
            mNoDivider = noDividerTwoButton(mTwoButtonBinding)
            mYesButton1 = yesButton1TwoButton(mTwoButtonBinding)

            setToolbarTitle(mToolbar)

            setDialogContent(
                contentArea = mContentArea1,
                dialogContent = mContent,
                setContent = { contentArea: ContentArea, contentValue: ContentType ->
                    setContent(contentArea, contentValue)
                }
            )

            updateNegButton(mNoButton, mNoDivider)

            updateYesButton1(mYesButton1)

            binding = mTwoButtonBinding
        } else {
            mThreeButtonBinding = threeButtonBinding(inflater)
            mToolbar = toolbarThreeButton(mThreeButtonBinding)
            mContentArea1 = contentAreaThreeButton(mThreeButtonBinding)
            mContent = content()
            mNoButton = noButtonThreeButton(mThreeButtonBinding)
            mNoDivider = noDividerThreeButton(mThreeButtonBinding)
            mYesButton1 = yesButton1ThreeButton(mThreeButtonBinding)
            mYesButton2 = yesButton2ThreeButton(mThreeButtonBinding)

            setToolbarTitle(mToolbar)

            setDialogContent(
                contentArea = mContentArea1,
                dialogContent = mContent,
                setContent = { contentArea: ContentArea, content: ContentType ->
                    setContent(contentArea, content)
                }
            )

            updateNegButton(mNoButton, mNoDivider)

            updateYesButton1(mYesButton1)

            updateYesButton2(mYesButton2)

            binding = mThreeButtonBinding
        }

        return binding.root
    }

    private fun setToolbarTitle(toolbar: Toolbar) {
        toolbar.title =
            message ?: getString(R.string.confirm_dialog, posButton?.let { getString(it) } ?: "")
    }

    private fun <ContentArea : View, ContentType : Any> setDialogContent(
        contentArea: ContentArea,
        dialogContent: ContentType?,
        setContent: (ContentArea, ContentType) -> Unit
    ) {
        contentArea.setVisibleIfTrue(dialogContent != null)

        dialogContent?.let { setContent(contentArea, it) }
    }

    private fun updateYesButton1(button: Button) {
        button.apply {
            val pos = posButton
            if (pos != null) {
                visibility = VISIBLE
                setText(pos)
                setOnClickListener {
                    dismiss()
                    posAction?.invoke()

                    val bundle = bundleOf(ARG_IS_CONFIRMED to true).apply {
                        confirmId?.let {
                            putLong(ARG_EXTRA_ITEM_ID, it)
                        }
                    }

                    parentFragmentManager.setFragmentResult(requestKey, bundle)
                }

                setAsDefault(posButtonIsDefault)
            } else {
                visibility = GONE
            }
        }
    }

    private fun updateYesButton2(button: Button) {
        button.apply {
            posButton2?.let { setText(it) }
            setOnClickListener {
                dismiss()
                posAction2?.invoke()

                val bundle = bundleOf(ARG_IS_CONFIRMED_2 to true).apply {
                    confirmId?.let {
                        putLong(ARG_EXTRA_ITEM_ID, it)
                    }
                }

                parentFragmentManager.setFragmentResult(requestKey, bundle)
            }

            setAsDefault(pos2ButtonIsDefault)
        }
    }

    private fun updateNegButton(button: Button, divider: View) {
        if (negButton != null) {
            button.apply {
                negButton?.let { setText(it) }
                setOnClickListener {
                    dismiss()
                    negAction?.invoke()

                    val bundle = bundleOf(ARG_IS_CONFIRMED to false).apply {
                        confirmId?.let {
                            putLong(ARG_EXTRA_ITEM_ID, it)
                        }
                    }

                    parentFragmentManager.setFragmentResult(requestKey, bundle)
                }
            }
        } else {
            button.visibility = GONE
            divider.visibility = GONE
        }
    }

    fun Button.setAsDefault(isDefault: Boolean) {
        setTypeface(typeface, if (isDefault) BOLD else NORMAL)
        setTextColor(
            context.getAttrColor(
                if (isDefault) R.attr.textColorDialogButtonDefault else R.attr.colorDialogText
            )
        )
    }

    companion object {
        const val ARG_IS_CONFIRMED: String = "${BuildConfig.APPLICATION_ID}.confirm"
        const val ARG_IS_CONFIRMED_2: String = "${BuildConfig.APPLICATION_ID}.confirm2"
        const val ARG_EXTRA_ITEM_ID: String = "${BuildConfig.APPLICATION_ID}.extra_id"

        @JvmStatic
        protected val ARG_REQ_KEY: String = "${BuildConfig.APPLICATION_ID}.request_key"

        @JvmStatic
        protected val ARG_CONFIRM_ID: String = "${BuildConfig.APPLICATION_ID}.confirm_id"

        @JvmStatic
        protected val ARG_MESSAGE: String = "${BuildConfig.APPLICATION_ID}.message"

        @JvmStatic
        protected val ARG_POS_TEXT: String = "${BuildConfig.APPLICATION_ID}.pos_btn_text"

        @JvmStatic
        protected val ARG_POS_ACTION: String = "${BuildConfig.APPLICATION_ID}.pos_action"

        @JvmStatic
        protected val ARG_POS_IS_DEFAULT: String = "${BuildConfig.APPLICATION_ID}.pos_is_default"

        @JvmStatic
        protected val ARG_NEG_TEXT: String = "${BuildConfig.APPLICATION_ID}.neg_btn_txt"

        @JvmStatic
        protected val ARG_NEG_ACTION: String = "${BuildConfig.APPLICATION_ID}.neg_action"

        @JvmStatic
        protected val ARG_POS_TEXT_2: String = "${BuildConfig.APPLICATION_ID}.pos_btn_text_2"

        @JvmStatic
        protected val ARG_POS_ACTION_2: String = "${BuildConfig.APPLICATION_ID}.pos_action_2"

        @JvmStatic
        protected val ARG_POS_2_IS_DEFAULT: String = "${BuildConfig.APPLICATION_ID}.pos_2_is_default"
    }
}