package com.wtb.dashTracker.ui.dialog_confirm_delete

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.viewbinding.ViewBinding
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DialogFragConfirm2ButtonBinding
import com.wtb.dashTracker.databinding.DialogFragConfirm3ButtonBinding
import com.wtb.dashTracker.extensions.setVisibleIfTrue
import com.wtb.dashTracker.views.FullWidthDialogFragment

open class ConfirmationDialog(
    @StringRes val text: Int?,
    val requestKey: String,
    private val confirmId: Int? = null,
    private val message: String? = null,
    @StringRes val posButton: Int = R.string.yes,
    var posAction: (() -> Unit)? = null,
    @StringRes val negButton: Int = R.string.cancel,
    var negAction: (() -> Unit)? = null,
    @StringRes val posButton2: Int? = null,
    val posAction2: (() -> Unit)? = null,
) : FullWidthDialogFragment() {

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

            val mNegAction = negAction ?: {
                setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to false))
            }

            binding.noButton.apply {
                setText(negButton)
                setOnClickListener {
                    dismiss()
                    mNegAction()
                }
            }

            val mPosAction = posAction ?: {
                val bundlePairs = bundleOf()
                bundlePairs.putBoolean(ARG_CONFIRM, true)
                confirmId?.let { bundlePairs.putInt(ARG_EXTRA, it) }
                if (confirmId == null) {
                    setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to true))
                } else {
                    setFragmentResult(
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

            val mNegAction = negAction ?: {
                setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to false))
            }

            binding.noButton.apply {
                setText(negButton)
                setOnClickListener {
                    dismiss()
                    mNegAction()
                }
            }

            val mPosAction = posAction ?: {
                if (confirmId == null) {
                    setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to true))
                } else {
                    setFragmentResult(
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

            val mPosAction2 = posAction2

            binding.yesButton2.apply {
                setText(posButton2)
                setOnClickListener {
                    dismiss()
                    mPosAction2()

                    if (confirmId == null) {
                        setFragmentResult(requestKey, bundleOf(ARG_CONFIRM to true))
                    } else {
                        setFragmentResult(
                            requestKey,
                            bundleOf(ARG_CONFIRM to true, ARG_EXTRA to confirmId)
                        )
                    }
                }
            }
        }

        return binding.root
    }

    companion object {
        private const val TAG = APP + "ConfirmDeleteDialog"
        const val ARG_CONFIRM = "confirm"
        const val ARG_EXTRA = "extra"
    }
}

enum class ConfirmType(val key: String) {
    DELETE("confirmDelete"),
    RESET("confirmReset"),
    SAVE("confirmSave")
}

class ConfirmDeleteDialog(confirmId: Int? = null) : ConfirmationDialog(
    text = R.string.confirm_delete,
    requestKey = ConfirmType.DELETE.key,
    confirmId = confirmId,
    posButton = R.string.delete
)

class ConfirmResetDialog : ConfirmationDialog(
    text = R.string.confirm_reset,
    requestKey = ConfirmType.RESET.key,
    posButton = R.string.reset
)

class ConfirmSaveDialog(
    @StringRes text: Int? = null,
) : ConfirmationDialog(
    text = text,
    requestKey = ConfirmType.SAVE.key,
    posButton = R.string.save,
    negButton = R.string.no
)