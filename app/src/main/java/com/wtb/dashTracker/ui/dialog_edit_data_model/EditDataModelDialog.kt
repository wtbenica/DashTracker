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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
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
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.DataModel
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmResetDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmSaveDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm.SimpleConfirmationDialog.Companion.ARG_IS_CONFIRMED
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

    /**
     * if save button is pressed or is confirmed by save dialog, gets assigned true
     */
    protected var saveConfirmed: Boolean = false

    /**
     * if delete button is pressed, gets assigned false
     */
    protected var saveOnExit = true

    protected abstract fun getViewBinding(inflater: LayoutInflater): B
    protected abstract fun updateUI(firstRun: Boolean = false)
    protected abstract fun saveValues()

    // TODO: should the name here be changed?
    protected abstract fun clearFields()
    protected abstract fun isEmpty(): Boolean
    private fun isNotEmpty(): Boolean = !isEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getLong(ARG_ITEM_ID, -1L).let {
            if (it != null && it != -1L) {
                viewModel.loadDataModel(it)
            }
        }

        setDialogListeners()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

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
                    Log.d(TAG, "New item ${it?.id}")
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
            val result = bundle.getBoolean(ARG_IS_CONFIRMED)
            if (result) {
                onDeleteItem()
            }
        }

        setFragmentResultListener(
            ConfirmType.RESET.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_IS_CONFIRMED)
            if (result) {
                updateUI()
            }
        }

        setFragmentResultListener(
            ConfirmType.SAVE.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_IS_CONFIRMED)
            Toast.makeText(context, "Save? $result", Toast.LENGTH_LONG).show()
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
                requestKey = REQUEST_KEY_DATA_MODEL_DIALOG,
                result = bundleOf(
                    ARG_MODIFICATION_STATE to ModificationState.MODIFIED.name,
                    ARG_MODIFIED_ID to item?.id
                )
            )
            dismiss()
        }
    }

    protected fun ImageButton.setOnDeletePressed() {
        setOnClickListener {
            if (isNotEmpty()) {
                ConfirmDeleteDialog.newInstance(item?.id)
                    .show(parentFragmentManager, "delete_entry")
            } else {
                onDeleteItem()
            }
        }
    }

    /**
     * Sets fragment result. If item is not a [DashEntry], deletes item.
     */
    private fun onDeleteItem() {
        saveOnExit = false

        setFragmentResult(
            requestKey = REQUEST_KEY_DATA_MODEL_DIALOG,
            result = bundleOf(
                ARG_MODIFICATION_STATE to ModificationState.DELETED.name,
                ARG_MODIFIED_ID to item?.id
            )
        )

        dismiss()

        if (item !is DashEntry) {
            item?.let { viewModel.delete(it) }
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
        const val ARG_ITEM_ID: String = "item_id"

        /**
         * Fragment result has [ARG_MODIFICATION_STATE] and [ARG_MODIFIED_ID] set
         */
        const val REQUEST_KEY_DATA_MODEL_DIALOG: String = "result: modification state"

        /**
         * Set to a [ModificationState] if item has been modified or deleted
         */
        const val ARG_MODIFICATION_STATE: String = "arg modification state"

        /**
         * Set to th the item id if the item has been modified or deleted
         */
        const val ARG_MODIFIED_ID: String = "arg modified id"
    }
}