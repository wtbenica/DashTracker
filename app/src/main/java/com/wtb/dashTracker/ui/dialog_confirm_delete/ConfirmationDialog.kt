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
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DialogFragConfirmBinding
import com.wtb.dashTracker.extensions.setVisibleIfTrue
import com.wtb.dashTracker.views.FullWidthDialogFragment

open class ConfirmationDialog(
    @StringRes val text: Int?,
    val requestKey: String,
    @StringRes val posButton: Int = R.string.yes,
    @StringRes val negButton: Int = R.string.cancel,
    private val confirmId: Int? = null,
    private val message: String? = null,
    var posAction: (() -> Unit)? = null,
    var negAction: (() -> Unit)? = null,
) :
    FullWidthDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: DialogFragConfirmBinding = DialogFragConfirmBinding.inflate(inflater)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

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

        binding.yesButton.apply {
            setText(posButton)
            setOnClickListener {
                dismiss()
                mPosAction()
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
    posButton = R.string.delete,
    confirmId = confirmId
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