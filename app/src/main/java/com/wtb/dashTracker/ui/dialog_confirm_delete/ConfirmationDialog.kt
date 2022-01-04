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

class ConfirmationDialog(val type: ConfirmationType, private val confirmId: Int? = null) :
    FullWidthDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: DialogFragConfirmBinding = DialogFragConfirmBinding.inflate(inflater)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.fragEntryToolbar.title =
            getString(R.string.confirm_dialog, getString(type.posButton))
        binding.theQuestion.setVisibleIfTrue(type.text != null)
        type.text?.let { binding.theQuestion.setText(it) }
        binding.noButton.apply {
            setText(type.negButton)
            setOnClickListener {
                dismiss()
                setFragmentResult(type.requestKey, bundleOf(ARG_CONFIRM to false))
            }
        }

        binding.yesButton.apply {
            setText(type.posButton)
            setOnClickListener {
                dismiss()
                val bundlePairs = bundleOf()
                bundlePairs.putBoolean(ARG_CONFIRM, true)
                confirmId?.let { bundlePairs.putInt(ARG_EXTRA, it) }
                if (confirmId == null) {
                    setFragmentResult(type.requestKey, bundleOf(ARG_CONFIRM to true))
                } else {
                    setFragmentResult(type.requestKey, bundleOf(ARG_CONFIRM to true, ARG_EXTRA to confirmId))
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

    enum class ConfirmationType(
        @StringRes val text: Int?,
        val requestKey: String,
        @StringRes val posButton: Int = R.string.yes,
        @StringRes val negButton: Int = R.string.cancel
    ) {
        DELETE(R.string.confirm_delete, "confirmDelete", R.string.delete),
        RESET(R.string.confirm_delete, "confirmReset", R.string.reset),
        SAVE(null, "confirmSave", R.string.save, R.string.keep)
    }
}