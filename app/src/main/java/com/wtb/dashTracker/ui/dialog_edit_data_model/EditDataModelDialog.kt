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

package com.wtb.dashTracker.ui.dialog_edit_data_model

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DataModel
import com.wtb.dashTracker.ui.dialog_confirm.*
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemViewModel
import com.wtb.dashTracker.ui.fragment_trends.FullWidthDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
abstract class EditDataModelDialog<M : DataModel, B : ViewBinding> : FullWidthDialogFragment() {
    protected abstract var item: M?
    protected abstract var binding: B
    protected abstract val viewModel: ListItemViewModel<M>

    // if save button is pressed or is confirmed by save dialog, gets assigned true
    protected var saveConfirmed = false

    // if delete button is pressed, gets assigned false
    private var saveOnExit = true

    protected abstract fun getViewBinding(inflater: LayoutInflater): B
    protected abstract fun updateUI()
    protected abstract fun saveValues()
    protected abstract fun clearFields()
    protected abstract fun isEmpty(): Boolean
    private fun isNotEmpty(): Boolean = !isEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getLong(ARG_ITEM_ID, -1L)?.let {
            if (it != -1L) {
                viewModel.loadDataModel(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setDialogListeners()

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = getViewBinding(inflater)

        updateUI()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.item.collectLatest {
                    item = it
                    updateUI()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        ComponentDialog(requireContext()).also {
            val onBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isEmpty() && !saveConfirmed) {
                        ConfirmSaveDialog.newInstance(
                            text = R.string.confirm_save_entry_incomplete
                        ).show(parentFragmentManager, null)
                    } else {
                        isEnabled = false
                        it.onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
            it.onBackPressedDispatcher.addCallback(onBackPressedCallback)
        }

    override fun onDestroy() {
        if (saveOnExit && (isNotEmpty() || saveConfirmed)) {
            saveValues()
        }

        super.onDestroy()
    }

    open fun setDialogListeners() {
        setFragmentResultListener(
            ConfirmType.DELETE.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ConfirmationDialog.ARG_CONFIRM)
            if (result) {
                saveOnExit = false

                // TODO: Is this necessary? Doesn't appear to get used anywhere
                setFragmentResult(
                    REQUEST_KEY_ENTRY_DIALOG,
                    bundleOf(
                        ARG_MODIFICATION_STATE to ModificationState.DELETED.ordinal
                    )
                )
                dismiss()
                item?.let { e -> viewModel.delete(e) }
            }
        }

        setFragmentResultListener(
            ConfirmType.RESET.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ConfirmationDialog.ARG_CONFIRM)
            if (result) {
                updateUI()
            }
        }

        setFragmentResultListener(
            ConfirmType.SAVE.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ConfirmationDialog.ARG_CONFIRM)
            if (result) {
                saveConfirmed = true
                dismiss()
            } else {
                saveOnExit = false
                dismiss()
                item?.let { e -> viewModel.delete(e) }
            }
        }
    }

    protected fun ImageButton.setOnSavePressed() {
        setOnClickListener {
            saveConfirmed = true
            setFragmentResult(
                requestKey = REQUEST_KEY_ENTRY_DIALOG,
                result = bundleOf(
                    ARG_MODIFICATION_STATE to ModificationState.MODIFIED.ordinal
                )
            )
            dismiss()
        }
    }

    protected fun ImageButton.setOnDeletePressed() {
        setOnClickListener {
            if (isNotEmpty()) {
                ConfirmDeleteDialog.newInstance(null).show(parentFragmentManager, null)
            } else {
                saveOnExit = false
                dismiss()
                item?.let { viewModel.delete(it) }
            }
        }
    }

    protected fun ImageButton.setOnResetPressed() {
        setOnClickListener {
            ConfirmResetDialog.newInstance().show(parentFragmentManager, null)
        }
    }

    enum class ModificationState(val key: String) {
        DELETED("deleted"),
        MODIFIED("modified")
    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
        const val REQUEST_KEY_ENTRY_DIALOG = "result: modification state"
        const val ARG_MODIFICATION_STATE = "arg modification state"
    }
}