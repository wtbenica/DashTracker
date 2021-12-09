package com.wtb.dashTracker.ui.dialog_base_pay_adjustment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.BasePayAdjustment
import com.wtb.dashTracker.databinding.DialogFragAdjustBinding
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalCoroutinesApi
class BasePayAdjustDialog(
    private var basePayAdjustment: BasePayAdjustment? = null
) : FullWidthDialogFragment() {

    private lateinit var binding: DialogFragAdjustBinding
    private val viewModel: BasePayAdjustViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = basePayAdjustment?.id

        viewModel.loadDataModel(id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        binding = DialogFragAdjustBinding.inflate(layoutInflater)

        val adapter = ArrayAdapter(
            context!!,
            android.R.layout.simple_spinner_item,
            getListOfWeeks()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.fragAdjustDate.adapter = adapter

        binding.fragAdjustBtnDelete.setOnClickListener {
            basePayAdjustment?.let { e -> viewModel.delete(e) }
        }

        binding.fragAdjustBtnCancel.setOnClickListener {
            viewModel.clearEntry()
            clearFields()
        }

        updateUI()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.item.collectLatest {
                Log.d(TAG, "Changing adjust: $it")
                basePayAdjustment = it
                updateUI()
            }
        }
    }

    override fun onDestroy() {
        if (!isEmpty())
            saveValues()
        super.onDestroy()
    }

    private fun updateUI() {
        val tempAdjust = basePayAdjustment
        if (tempAdjust != null) {
            binding.fragAdjustDate.apply {
                getSpinnerIndex(tempAdjust.date)?.let { setSelection(it) }
            }
            binding.fragAdjustAmount.setText(getString(R.string.currency_unit, tempAdjust.amount))
        }
    }

    private fun getSpinnerIndex(date: LocalDate): Int? {
        (0..binding.fragAdjustDate.count).forEach { i ->
            if (binding.fragAdjustDate.adapter.getItem(i) == date) {
                return i
            }
        }
        return null
    }

    private fun saveValues() {
        val date = binding.fragAdjustDate.selectedItem as LocalDate
        val adjust = BasePayAdjustment(
            adjustmentId = basePayAdjustment?.adjustmentId ?: AUTO_ID,
            date = date,
            amount = binding.fragAdjustAmount.text.toString().toFloat()
        )
        viewModel.upsert(adjust)
    }

    private fun clearFields() {
        binding.fragAdjustDate.setSelection(0)
        binding.fragAdjustAmount.text.clear()
    }

    private fun isEmpty(): Boolean = binding.fragAdjustAmount.text.isEmpty()

    companion object {
        private const val TAG = APP + "BasePayAdjustDialog"
        fun getListOfWeeks(): Array<LocalDate> {
            val res = arrayListOf<LocalDate>()
            var endOfWeek = MainActivity.getNextEndOfWeek().minusDays(7)
            while (endOfWeek > LocalDate.now().minusYears(1)) {
                res.add(endOfWeek)
                endOfWeek = endOfWeek.minusDays(7)
            }
            return res.toTypedArray()
        }
    }
}