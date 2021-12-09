package com.wtb.dashTracker.views

import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment

open class FullWidthDialogFragment: DialogFragment() {
    override fun onResume() {
        super.onResume()
        val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
    }
}