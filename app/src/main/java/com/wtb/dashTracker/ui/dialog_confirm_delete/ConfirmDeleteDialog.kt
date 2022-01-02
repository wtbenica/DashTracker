package com.wtb.dashTracker.ui.dialog_confirm_delete

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.views.FullWidthDialogFragment

class ConfirmDeleteDialog : FullWidthDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.confirm_delete))
            .setPositiveButton(R.string.delete) { _, _ ->
                Log.d(TAG, "setting fragment result to true")
                setFragmentResult("confirmDelete", bundleOf("confirm" to true))
            }
            .setNegativeButton(R.string.keep) { _, _ ->
                Log.d(TAG, "setting fragment result to false")
                setFragmentResult("confirmDelete", bundleOf("confirm" to false))
            }
            .create()

    companion object {
        const val TAG = APP + "ConfirmDeleteDialog"
    }
}