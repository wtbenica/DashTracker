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
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DialogFragConfirmExportBinding
import com.wtb.dashTracker.extensions.setVisibleIfTrue
import com.wtb.dashTracker.ui.fragment_trends.FullWidthDialogFragment


open class ConfirmationDialogExport(
    val actionExport: () -> Unit,
) : FullWidthDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val binding = DialogFragConfirmExportBinding.inflate(inflater)

        binding.exportNotesBtn.setOnClickListener {
            binding.exportNotesText.setVisibleIfTrue(binding.exportNotesText.visibility == GONE)
            val rs = if (binding.exportNotesText.visibility == GONE)
                R.drawable.ic_arrow_expand
            else
                R.drawable.ic_arrow_collapse

            binding.exportNotesBtn.icon = AppCompatResources.getDrawable(requireContext(), rs)
        }

        binding.noButton.apply {
            setOnClickListener {
                dismiss()
            }
        }

        binding.yesButton1.apply {
            setOnClickListener {
                dismiss()
                actionExport()
            }
        }

        return binding.root
    }
}