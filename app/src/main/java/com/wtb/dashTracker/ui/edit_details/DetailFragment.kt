package com.wtb.dashTracker.ui.edit_details

import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.AUTO_ID
import com.wtb.gigtracker.ui.main.DetailViewModel
import com.wtb.dashTracker.database.DashEntry
import com.wtb.dashTracker.ui.daily.DailyFragment.Companion.dtfDate
import com.wtb.dashTracker.ui.daily.DailyFragment.Companion.dtfDateThisYear
import com.wtb.dashTracker.ui.daily.DailyFragment.Companion.dtfTime
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

fun CharSequence.toTimeOrNull() =
    if (this.isNotEmpty()) LocalTime.parse(this, dtfTime) else null

fun CharSequence.toDateOrNull() =
    if (this.isNotEmpty()) {
        try {
            val df = dtfDate
            LocalDate.parse(this, df)
        } catch (e: Exception) {
            try {
                val df = dtfDateThisYear
                LocalDate.parse(this, df)
            } catch (e: Exception) {
                null
            }
        }
    } else {
        null
    }

fun Editable.toFloatOrNullB() = if (this.isNotEmpty()) this.toString().toFloatOrNull() else null

fun Editable.toIntOrNullB() = if (this.isNotEmpty()) this.toString().toIntOrNull() else null

@ExperimentalCoroutinesApi
class DetailFragment(
    private var entry: DashEntry? = null
) : DialogFragment() {

    private val detailViewModel: DetailViewModel by viewModels()

    private lateinit var dateTextView: TextView

    private lateinit var startTimeTextView: TextView
    private lateinit var endTimeTextView: TextView
    private lateinit var endsNextDayCheckBox: CheckBox
    private lateinit var startMileageEditText: EditText
    private lateinit var endMileageEditText: EditText
    private lateinit var totalMileageEditText: EditText
    private lateinit var payEditText: EditText
    private lateinit var otherPayEditText: EditText
    private lateinit var cashTipsEditText: EditText
    private lateinit var numDeliveriesEditText: EditText
    private lateinit var deleteButton: ImageButton
    private lateinit var cancelButton: ImageButton

    override fun onResume() {
        super.onResume()
        val params: ViewGroup.LayoutParams = dialog!!.window!!.attributes
        params.width = MATCH_PARENT
        params.height = WRAP_CONTENT
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entryId = entry?.entryId

        detailViewModel.loadEntry(entryId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_entry, container, false)

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

        endsNextDayCheckBox =
            view.findViewById<CheckBox>(R.id.frag_entry_check_ends_next_day).apply {
                setOnCheckedChangeListener { buttonView, isChecked ->

                }
            }

        startMileageEditText = view.findViewById(R.id.frag_entry_start_mileage)
        endMileageEditText = view.findViewById(R.id.frag_entry_end_mileage)
        totalMileageEditText = view.findViewById(R.id.frag_entry_total_mileage)
        payEditText = view.findViewById(R.id.frag_entry_pay)
        otherPayEditText = view.findViewById(R.id.frag_entry_pay_other)
        cashTipsEditText = view.findViewById(R.id.frag_entry_cash_tips)
        numDeliveriesEditText = view.findViewById(R.id.frag_entry_num_deliveries)

        deleteButton = view.findViewById<ImageButton>(R.id.frag_entry_btn_delete).apply {
            setOnClickListener {
                entry?.let { e -> detailViewModel.delete(e) }
            }
        }

        cancelButton = view.findViewById<ImageButton>(R.id.frag_entry_btn_cancel).apply {
            setOnClickListener {
                detailViewModel.clearEntry()
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
            startOdometer = startMileageEditText.text.toFloatOrNullB(),
            endOdometer = endMileageEditText.text.toFloatOrNullB(),
            totalMileage = totalMileageEditText.text.toFloatOrNullB(),
            pay = payEditText.text.toFloatOrNullB(),
            otherPay = otherPayEditText.text.toFloatOrNullB(),
            cashTips = cashTipsEditText.text.toFloatOrNullB(),
            numDeliveries = numDeliveriesEditText.text.toIntOrNullB()
        )

        detailViewModel.upsert(e)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            detailViewModel.entry.collectLatest {
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
                dateTextView.setText(tempEntry.date.format(dtfDate))
                tempEntry.startTime?.let { st -> startTimeTextView.setText(st.format(dtfTime)) }
                tempEntry.endTime?.let { et -> endTimeTextView.setText(et.format(dtfTime)) }
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
        dateTextView.setText(LocalDate.now().format(dtfDate))
        startTimeTextView.text = ""
        endTimeTextView.text = ""
        startMileageEditText.text.clear()
        endMileageEditText.text.clear()
        payEditText.text.clear()
        otherPayEditText.text.clear()
        cashTipsEditText.text.clear()
        numDeliveriesEditText.text.clear()
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
}