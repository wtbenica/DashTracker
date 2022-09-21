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

@file:Suppress("RedundantNullableReturnType", "RedundantNullableReturnType")

package com.wtb.dashTracker.ui.dialog_confirm.mileage_breakdown

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.Drive
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.DialogFragConfirmDashActivityBinding
import com.wtb.dashTracker.databinding.PauseRowBinding
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_drive.DriveDialog
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ConfirmationDialogDrivesList private constructor() : FullWidthDialogFragment() {

    private val viewModel: ConfirmationDialogMileageStuffViewModel by viewModels()
    private var entry: FullEntry? = null

    private lateinit var binding: DialogFragConfirmDashActivityBinding
    private var deleteButtonPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = arguments?.getLong(ARG_ENTRY_ID)
        viewModel.loadEntry(id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding =
            DialogFragConfirmDashActivityBinding.inflate(inflater)

        binding.noButton.setOnClickListener {
            deleteButtonPressed = true
            dismiss()
        }

        binding.yesButton1.apply {
            setOnClickListener {
                dismiss()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.fullEntry.collectLatest {
                entry = it
                updateUI()
            }
        }
    }

    private fun updateUI() {
        (context as MainActivity).runOnUiThread {
            entry?.let {
                this.binding.mileageStuff.spookyTomato.apply {
                    removeAllViews()

                    it.drives.forEach { drive: Drive ->
                        val newPauseRow = PauseRow.newInstance(requireContext(), drive)
                        addView(newPauseRow)
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_ENTRY_ID = "arg_purpose_id"

        fun newInstance(
            entryId: Long
        ): ConfirmationDialogDrivesList {
            return ConfirmationDialogDrivesList().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ENTRY_ID, entryId)
                }
            }
        }
    }
}

@ExperimentalCoroutinesApi
private class PauseRow(context: Context) : GridLayout(context) {
    private val binding: PauseRowBinding
    private var drive: Drive? = null

    init {
        val view = inflate(context, R.layout.pause_row, this)
        binding = PauseRowBinding.bind(view)
        isClickable = true
        setOnClickListener {
            DriveDialog.newInstance(drive?.driveId ?: AUTO_ID)
                .show((context as MainActivity).supportFragmentManager, null)
        }
    }

    fun updateUI() {
        binding.pauseRowTime.text = drive?.getTimeRange()

        binding.pauseRowOdometers.text =
            context.getString(
                R.string.odometer_range,
                drive?.startOdometer?.toFloat(),
                drive?.endOdometer?.toFloat()
            )

        binding.pauseRowMileage.text = context.getString(
            R.string.odometer_fmt,
            ((drive?.endOdometer ?: drive?.startOdometer ?: 0)
                    - (drive?.startOdometer ?: 0)).toFloat()
        )
    }

    companion object {
        fun newInstance(context: Context, entry: Drive): PauseRow {
            return PauseRow(context).apply {
                drive = entry
                updateUI()
            }
        }
    }
}