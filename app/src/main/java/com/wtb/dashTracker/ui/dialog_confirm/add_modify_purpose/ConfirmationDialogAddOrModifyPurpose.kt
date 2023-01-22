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

@file:Suppress("RedundantNullableReturnType")

package com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.databinding.DialogFragConfirmAddPurposeBinding
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.fragment_trends.FullWidthDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class ConfirmationDialogAddOrModifyPurpose : FullWidthDialogFragment() {

    private val viewModel: ConfirmationDialogAddOrModifyPurposeViewModel by viewModels()
    private var expensePurpose: ExpensePurpose? = null
    private var isNew: Boolean = true

    private lateinit var binding: DialogFragConfirmAddPurposeBinding
    private var deleteButtonPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val purposeId = arguments?.getLong(ARG_PURPOSE_ID)
        arguments?.getBoolean(ARG_IS_NEW)?.let { isNew = it }
        viewModel.loadDataModel(purposeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragConfirmAddPurposeBinding.inflate(inflater)

        if (!isNew) {
            binding.newExpenseToolbar.title = getString(R.string.dialog_title_edit_expense_type)
            binding.theQuestion.setText(R.string.message_edit_expense)
        }

        binding.dialogPurposeEditText.doOnTextChanged { text: CharSequence?, _, _, _ ->
            binding.yesButton1.isEnabled = !text.isNullOrBlank()
        }

        binding.noButton.setOnClickListener {
            deleteButtonPressed = true
            setFragmentResult(REQUEST_KEY_DIALOG_ADD_OR_MODIFY_PURPOSE, Bundle().apply {
                putBoolean(RESULT_UPDATE_PURPOSE, false)
            })
            dismiss()
        }

        binding.yesButton1.setOnClickListener {
            setFragmentResult(
                REQUEST_KEY_DIALOG_ADD_OR_MODIFY_PURPOSE,
                Bundle().apply {
                    putBoolean(RESULT_UPDATE_PURPOSE, true)
                    expensePurpose?.id?.let { id -> putLong(RESULT_PURPOSE_ID, id) }
                }
            )
            dismiss()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.item.collectLatest {
                expensePurpose = it
                updateUI()
            }
        }
    }

    private fun updateUI() {
        (context as MainActivity).runOnUiThread {
            binding.dialogPurposeEditText.setText(expensePurpose?.name)
        }
    }

    /**
     * saves or deletes [expensePurpose]
     *
     */
    override fun onDestroy() {
        /**
         * Saves [expensePurpose] if not [deleteButtonPressed] && purpose name is not null/blank
         */
        fun saveValues() {
            val name = binding.dialogPurposeEditText.text
            if (!deleteButtonPressed && !name.isNullOrBlank()) {
                expensePurpose?.let { ep ->
                    ep.name = name.toString().replaceFirstChar { it.uppercase() }
                    viewModel.upsert(ep)
                }
            } else {
                if (isNew) expensePurpose?.let { viewModel.delete(it) }
            }
        }

        saveValues()

        super.onDestroy()
    }

    companion object {
        internal const val REQUEST_KEY_DIALOG_ADD_OR_MODIFY_PURPOSE: String = "add_purpose"
        internal val RESULT_PURPOSE_ID: String = "${BuildConfig.APPLICATION_ID}.${this::class.simpleName}.result_purpose_id"
        internal const val RESULT_UPDATE_PURPOSE = "${BuildConfig.APPLICATION_ID}.arg_update_purpose"

        private const val ARG_PURPOSE_ID: String = "arg_purpose_id"
        private const val ARG_IS_NEW = "arg_is_new"
        private const val ARG_PREV_PURPOSE = "arg_prev_purpose"

        fun newInstance(
            purposeId: Long,
            prevPurpose: Long? = null,
            isNew: Boolean = true,
        ): ConfirmationDialogAddOrModifyPurpose = ConfirmationDialogAddOrModifyPurpose().apply {
            arguments = Bundle().apply {
                putLong(ARG_PURPOSE_ID, purposeId)
                putBoolean(ARG_IS_NEW, isNew)
                prevPurpose?.let { putLong(ARG_PREV_PURPOSE, it) }
            }
        }
    }
}