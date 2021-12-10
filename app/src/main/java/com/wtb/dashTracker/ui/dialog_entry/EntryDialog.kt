package com.wtb.dashTracker.ui.dialog_entry

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment
import com.wtb.dashTracker.views.FullWidthDialogFragment
import com.wtb.dashTracker.views.TableRadioButton
import com.wtb.dashTracker.views.TableRadioGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalCoroutinesApi
class EntryDialog(
    private var entry: DashEntry? = null
) : FullWidthDialogFragment(), TableRadioGroup.TableRadioGroupCallback {

    private val viewModel: EntryViewModel by viewModels()

    private lateinit var dateTextView: TextView
    private lateinit var startTimeTextView: TextView
    private lateinit var endTimeTextView: TextView
    private lateinit var endsNextDayCheckBox: CheckBox
    private lateinit var startEndOdoTableRadioButton: TableRadioButton
    private lateinit var tripOdoTableRadioButton: TableRadioButton
    private lateinit var startMileageEditText: EditText
    private lateinit var endMileageEditText: EditText
    private lateinit var totalMileageEditText: EditText
    private lateinit var payEditText: EditText
    private lateinit var otherPayEditText: EditText
    private lateinit var cashTipsEditText: EditText
    private lateinit var numDeliveriesEditText: EditText
    private lateinit var deleteButton: ImageButton
    private lateinit var cancelButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entryId = entry?.entryId

        viewModel.loadDataModel(entryId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_frag_entry, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dateTextView = view.findViewById<TextView>(R.id.frag_entry_date).apply {
            setOnClickListener {
                DatePickerFragment(this).show(parentFragmentManager, "date_picker")
            }
        }

        startTimeTextView = view.findViewById<TextView>(R.id.frag_entry_start_time).apply {
            setOnClickListener {
                TimePickerFragment(this).show(parentFragmentManager, "time_picker_start")
            }
        }

        endTimeTextView = view.findViewById<TextView>(R.id.frag_entry_end_time).apply {
            setOnClickListener {
                TimePickerFragment(this).show(parentFragmentManager, "time_picker_end")
            }
        }

        endsNextDayCheckBox = view.findViewById<CheckBox>(R.id.frag_entry_check_ends_next_day)

        view.findViewById<TableRadioButton>(R.id.frag_entry_trb_start_end_odometer)

        tripOdoTableRadioButton = view.findViewById(R.id.frag_entry_trb_trip_odometer)
        startEndOdoTableRadioButton =
            view.findViewById<TableRadioButton>(R.id.frag_entry_trb_start_end_odometer).apply {
                val radioGroup = this.getRadioGroup()
                radioGroup?.callback = this@EntryDialog
            }
        tripOdoTableRadioButton = view.findViewById(R.id.frag_entry_trb_trip_odometer)
        startMileageEditText = view.findViewById(R.id.frag_entry_start_mileage)
        endMileageEditText = view.findViewById(R.id.frag_entry_end_mileage)
        totalMileageEditText = view.findViewById(R.id.frag_entry_total_mileage)
        payEditText = view.findViewById(R.id.frag_entry_pay)
        otherPayEditText = view.findViewById(R.id.frag_entry_pay_other)
        cashTipsEditText = view.findViewById(R.id.frag_entry_cash_tips)
        numDeliveriesEditText = view.findViewById(R.id.frag_entry_num_deliveries)

        deleteButton = view.findViewById<ImageButton>(R.id.frag_entry_btn_delete).apply {
            setOnClickListener {
                entry?.let { e -> viewModel.delete(e) }
            }
        }

        cancelButton = view.findViewById<ImageButton>(R.id.frag_entry_btn_cancel).apply {
            setOnClickListener {
                viewModel.clearEntry()
                clearFields()
            }
        }

        updateUI()

        return view
    }

    private fun saveValues() {
        val date = dateTextView.text.toDateOrNull()
        val e = DashEntry(
            entryId = entry?.entryId ?: AUTO_ID,
            date = date ?: LocalDate.now(),
            endDate = (if (endsNextDayCheckBox.isChecked) date?.plusDays(1) else date)
                ?: LocalDate.now(),
            startTime = startTimeTextView.text.toTimeOrNull(),
            endTime = endTimeTextView.text.toTimeOrNull(),
            startOdometer = startMileageEditText.text.toFloatOrNull(),
            endOdometer = endMileageEditText.text.toFloatOrNull(),
            totalMileage = totalMileageEditText.text.toFloatOrNull(),
            pay = payEditText.text.toFloatOrNull(),
            otherPay = otherPayEditText.text.toFloatOrNull(),
            cashTips = cashTipsEditText.text.toFloatOrNull(),
            numDeliveries = numDeliveriesEditText.text.toIntOrNull()
        )

        viewModel.upsert(e)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.item.collectLatest {
                Log.d(TAG, "Changing entry: $it")
                entry = it
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
        (context as MainActivity?)?.runOnUiThread {
            val tempEntry = entry
            if (tempEntry != null) {
                dateTextView.text = tempEntry.date.format(dtfDate)
                tempEntry.startTime?.let { st -> startTimeTextView.text = st.format(dtfTime) }
                tempEntry.endTime?.let { et -> endTimeTextView.text = et.format(dtfTime) }
                endsNextDayCheckBox.isChecked =
                    tempEntry.endDate.minusDays(1L).equals(tempEntry.date)
                tempEntry.startOdometer?.let { so -> startMileageEditText.setText(so.toString()) }
                tempEntry.endOdometer?.let { eo -> endMileageEditText.setText(eo.toString()) }
                tempEntry.mileage?.let { m -> totalMileageEditText.setText(m.toString()) }
                tempEntry.pay?.let { p -> payEditText.setText(p.toString()) }
                tempEntry.otherPay?.let { op -> otherPayEditText.setText(op.toString()) }
                tempEntry.cashTips?.let { ct -> cashTipsEditText.setText(ct.toString()) }
                tempEntry.numDeliveries?.let { nd -> numDeliveriesEditText.setText(nd.toString()) }
            } else {
                clearFields()
            }
        }
    }

    private fun clearFields() {
        dateTextView.text = LocalDate.now().format(dtfDate)
        startTimeTextView.text = ""
        endTimeTextView.text = ""
        startMileageEditText.text.clear()
        endMileageEditText.text.clear()
        payEditText.text.clear()
        otherPayEditText.text.clear()
        cashTipsEditText.text.clear()
        numDeliveriesEditText.text.clear()
        startEndOdoTableRadioButton.let {
            it.getRadioGroup()?.setChecked(it)
        }
    }

    private fun isEmpty() =
        dateTextView.text == LocalDate.now().format(dtfDate) &&
                startTimeTextView.text.isBlank() &&
                endTimeTextView.text.isBlank() &&
                startMileageEditText.text.isBlank() &&
                endMileageEditText.text.isBlank() &&
                payEditText.text.isBlank() &&
                otherPayEditText.text.isBlank() &&
                cashTipsEditText.text.isBlank() &&
                numDeliveriesEditText.text.isBlank()

    companion object {

        private const val TAG = APP + "DetailFragment"
        private const val ARG_ENTRY_ID = "arg_entry_id"
    }

    override fun onCheckChanged(button: TableRadioButton) {
        if (button == startEndOdoTableRadioButton) {
            disableEntryView(totalMileageEditText)
            enableEntryView(startMileageEditText, endMileageEditText)
        } else if (button == tripOdoTableRadioButton) {
            disableEntryView(startMileageEditText, endMileageEditText)
            enableEntryView(totalMileageEditText)
        }
    }

    private fun enableEntryView(vararg view: TextView) {
        view.forEach {
            it.setBackgroundResource(R.drawable.background_edit_text)
            it.isEnabled = true
        }
    }

    private fun disableEntryView(vararg view: TextView) {
        view.forEach {
            it.setBackgroundResource(R.drawable.disabled_bg)
            it.isEnabled = false
        }
    }
}