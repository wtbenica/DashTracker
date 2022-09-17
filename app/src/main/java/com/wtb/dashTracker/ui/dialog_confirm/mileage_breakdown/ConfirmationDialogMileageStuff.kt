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

package com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.database.models.FullEntry.DriveOld
import com.wtb.dashTracker.databinding.DialogFragConfirmDashActivityBinding
import com.wtb.dashTracker.databinding.PauseRowBinding
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.ui.dialog_confirm.mileage_breakdown.ConfirmationDialogMileageStuffViewModel
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class ConfirmationDialogMileageStuff private constructor() : FullWidthDialogFragment() {

    private val viewModel: ConfirmationDialogMileageStuffViewModel by viewModels()
    private var entry: FullEntry? = null

    private lateinit var binding: DialogFragConfirmDashActivityBinding
    private var deleteButtonPressed = false

    private val prevPurpose: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = arguments?.getLong(ARG_PURPOSE_ID)
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
            Log.d(TAG, "Cancelling, reverting to old purposeId: $prevPurpose")
            deleteButtonPressed = true
            setFragmentResult(RK_ADD_PURPOSE, Bundle().apply {
                putBoolean(ARG_CONFIRM, true)
                prevPurpose?.let { purpose -> putInt(ARG_PURPOSE_ID, purpose) }
            })
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

                    it.drives.forEach { drive: DriveOld ->
                        val newPauseRow = PauseRow.newInstance(requireContext(), drive)
                        addView(newPauseRow)
                    }
                }
            }
        }
    }

    //    private fun saveValues() {
//        val name = binding.dialogPurposeEditText.text
//        if (!deleteButtonPressed && !name.isNullOrBlank()) {
//            entry?.let { ep ->
//                ep.name = name.toString().replaceFirstChar { it.uppercase() }
//                viewModel.upsert(ep)
//            }
//        } else {
//            entry?.let { viewModel.delete(it) }
//        }
//    }
//
    override fun onDestroy() {
//        saveValues()

        super.onDestroy()
    }

    companion object {
        const val ARG_PURPOSE_NAME = "arg_purpose_name"
        const val ARG_PURPOSE_ID = "arg_purpose_id"
        const val RK_ADD_PURPOSE = "add_purpose"

        fun newInstance(
            purposeId: Long
        ): ConfirmationDialogMileageStuff {
            Log.d(TAG, "Making a new mileage stuff")
            return ConfirmationDialogMileageStuff().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PURPOSE_ID, purposeId)
                }
            }
        }
    }
}

@ExperimentalCoroutinesApi
private class PauseRow(context: Context) : TableRow(context) {
    private val binding: PauseRowBinding

    init {
        val view = inflate(context, R.layout.pause_row, this)
        binding = PauseRowBinding.bind(view)
    }

    fun updateUI(drive: DriveOld) {
        binding.pauseRowTime.text = drive.getTimeRange()

        binding.pauseRowOdometers.text =
            context.getString(
                R.string.odometer_range,
                drive.startOdometer?.toFloat(),
                drive.endOdometer?.toFloat()
            )

        binding.pauseRowMileage.text = context.getString(
            R.string.odometer_fmt,
            ((drive.endOdometer ?: drive.startOdometer ?: 0) - (drive.startOdometer ?: 0)).toFloat()
        )

        binding.pauseRowIsPaused.text = if (drive.isPaused) "paused" else "driving"
    }

    companion object {
        fun newInstance(context: Context, entry: DriveOld): PauseRow {
            return PauseRow(context).apply {
                updateUI(entry)
            }
        }
    }
}