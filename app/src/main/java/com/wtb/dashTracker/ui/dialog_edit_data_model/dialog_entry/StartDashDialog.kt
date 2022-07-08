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

package com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.provider.ContactsContract.Directory.PACKAGE_NAME
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.databinding.DialogFragStartDashBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalCoroutinesApi
class StartDashDialog : EditDataModelDialog<DashEntry, DialogFragStartDashBinding>() {
    override var item: DashEntry? = null
    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragStartDashBinding

    private var startTimeChanged = false

    override fun getViewBinding(inflater: LayoutInflater): DialogFragStartDashBinding =
        DialogFragStartDashBinding.inflate(layoutInflater).apply {

            fragEntryDate.apply {
                setOnClickListener {
                    DatePickerFragment.newInstance(
                        R.id.frag_entry_date,
                        this.text.toString(),
                        DatePickerFragment.REQUEST_KEY_DATE
                    ).show(parentFragmentManager, "entry_date_picker")
                }
            }

            fragEntryStartTime.apply {
                setOnClickListener {
                    TimePickerFragment.newInstance(
                        R.id.frag_entry_start_time,
                        this.text.toString(),
                        TimePickerFragment.REQUEST_KEY_TIME
                    ).show(childFragmentManager, "time_picker_start")
                    startTimeChanged = true
                }
            }

            fragEntryBtnDelete.apply {
                setOnDeletePressed()
            }

            fragEntryBtnCancel.apply {
                setOnResetPressed()
            }

            fragEntryBtnStart.apply {
                setOnClickListener {
                    saveConfirmed = true
                    setFragmentResult(
                        requestKey = REQ_KEY_START_DASH,
                        result = bundleOf(
                            ARG_RESULT to true,
                            ARG_ENTRY_ID to (item?.entryId ?: AUTO_ID)
                        )
                    )
                    dismiss()
//                    when {
//                        hasPermissions(activity as Context, *REQUIRED_PERMISSIONS) -> loadNewTrip()
//                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
//                            showRationaleLocation { locationPermLauncher.launch(LOCATION_PERMISSIONS) }
//                        else -> locationPermLauncher.launch(LOCATION_PERMISSIONS)
//                    }
                }
            }
        }

    override fun updateUI() {
        val tempEntry = item
        if (tempEntry != null) {
            binding.fragEntryDate.text = tempEntry.date.format(dtfDate)
            tempEntry.startTime?.let { st ->
                binding.fragEntryStartTime.text = st.format(dtfTime)
            }
            tempEntry.startOdometer?.let { so -> binding.fragEntryStartMileage.setText(so.toString()) }
        } else {
            clearFields()
        }
    }

    override fun saveValues() {
        val currDate = binding.fragEntryDate.text.toDateOrNull()
        val e = DashEntry(
            entryId = item?.entryId ?: AUTO_ID,
            date = currDate ?: LocalDate.now(),
            startTime = binding.fragEntryStartTime.text.toTimeOrNull(),
            startOdometer = binding.fragEntryStartMileage.text.toFloatOrNull(),
        )

        viewModel.upsert(e)
    }

    override fun clearFields() {
        binding.apply {
            fragEntryDate.text = LocalDate.now().format(dtfDate)
            fragEntryStartTime.text = LocalDateTime.now().format(dtfTime)
            fragEntryStartMileage.text.clear()
        }
    }

    override fun isEmpty(): Boolean {
        val isTodaysDate = binding.fragEntryDate.text == LocalDate.now().format(dtfDate)
        return isTodaysDate &&
                !startTimeChanged &&
                binding.fragEntryStartMileage.text.isBlank()
    }

    override fun setDialogListeners() {
        super.setDialogListeners()

        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _, bundle ->
            val year = bundle.getInt(DatePickerFragment.ARG_NEW_YEAR)
            val month = bundle.getInt(DatePickerFragment.ARG_NEW_MONTH)
            val dayOfMonth = bundle.getInt(DatePickerFragment.ARG_NEW_DAY)
            when (bundle.getInt(DatePickerFragment.ARG_DATE_TEXTVIEW)) {
                R.id.frag_entry_date -> {
                    binding.fragEntryDate.text =
                        LocalDate.of(year, month, dayOfMonth).format(dtfDate).toString()
                }
            }
        }

        childFragmentManager.setFragmentResultListener(
            TimePickerFragment.REQUEST_KEY_TIME,
            this
        ) { _, bundle ->
            val hour = bundle.getInt(TimePickerFragment.ARG_NEW_HOUR)
            val minute = bundle.getInt(TimePickerFragment.ARG_NEW_MINUTE)
            when (bundle.getInt(TimePickerFragment.ARG_TIME_TEXTVIEW)) {
                R.id.frag_entry_start_time -> {
                    binding.fragEntryStartTime.text =
                        LocalTime.of(hour, minute).format(dtfTime).toString()
                }
            }
        }
    }

    // Location stuff
//    private var locationService: LocationService? = null
//    private var locationServiceBound = false
//
//    private val locationPermLauncher =
//        registerMultiplePermissionsLauncher(onGranted = ::getBgLocationPermission)
//    private val bgLocationPermLauncher = registerSinglePermissionLauncher(onGranted = ::loadNewTrip)
//
//    private val locationServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            val binder = service as LocationService.LocalBinder
//
//            locationService = binder.service
//            locationServiceBound = true
//            locationService?.tripId?.value.let { viewModel.loadDataModel(it) }
//            locationService?.apply {
//                initialize(
//                    notificationData = locationServiceNotifData,
//                    notificationChannel = getNotificationChannel(),
//                    notificationText = { "Mileage tracking is on. Background location is in use." }
//                )
//                setIsTesting(true)
//            }
//            initLocSvcObservers()
//        }
//
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            locationService = null
//            locationServiceBound = false
//        }
//    }
//
//    private fun initLocSvcObservers() {
//        lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                locationService?.serviceState?.collectLatest {
//
//// TODO: This might be useful
////
////                    when (it) {
////                        LocationService.ServiceState.TRACKING -> binding?.startButton?.isChecked =
////                            true
////                        LocationService.ServiceState.PAUSED -> binding?.pauseButton?.isChecked =
////                            true
////                        LocationService.ServiceState.STOPPED -> binding?.stopButton?.isChecked =
////                            true
////                        else -> {
////                            // Do nothing, I don't care
////                        }
////                    }
//                }
//            }
//        }
//
//// TODO: These probably aren't useful
////
////        lifecycleScope.launch {
////            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
////                locationService?.isStill?.collectLatest {
////                    binding?.isStillValue?.text = it.toString()
////                }
////            }
////        }
////
////        lifecycleScope.launch {
////            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
////                locationService?.inSelectedTransport?.collectLatest {
////                    binding?.inVehicleValue?.text = it.toString()
////                }
////            }
////        }
////
////        lifecycleScope.launch {
////            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
////                locationService?.isTracking?.collectLatest {
////                    binding?.isTrackingValue?.text = it.toString()
////                }
////            }
////        }
//    }
//
//    val locationServiceNotifData
//        get() = NotificationUtils.NotificationData(
//            contentTitle = R.string.app_name,
//            bigContentTitle = R.string.app_name,
//            icon = R.mipmap.icon_c,
//            actions = listOf(
//                NotificationCompat.Action(
//                    R.drawable.ic_launch,
//                    "Open",
//                    launchActivityPendingIntent
//                ),
//                NotificationCompat.Action(
//                    R.drawable.ic_cancel,
//                    "Stop",
//                    cancelServicePendingIntent
//                )
//            )
//        )
//
//    private val cancelServicePendingIntent: PendingIntent?
//        get() {
//            val cancelIntent = Intent(requireContext(), LocationService::class.java).apply {
//                putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)
//            }
//            return PendingIntent.getService(
//                requireContext(),
//                0,
//                cancelIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//        }
//
//    private val launchActivityPendingIntent: PendingIntent?
//        get() {
//            val launchActivityIntent = Intent(requireActivity(), MainActivity::class.java)
//
//            return PendingIntent.getActivity(
//                requireContext(),
//                0,
//                launchActivityIntent,
//                PendingIntent.FLAG_IMMUTABLE
//            )
//        }
//
//    private fun getBgLocationPermission() {
//        when {
//            hasPermissions(
//                activity as Context,
//                Manifest.permission.ACCESS_BACKGROUND_LOCATION
//            ) -> loadNewTrip()
//            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ->
//                showRationaleBgLocation { bgLocationPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
//            else -> bgLocationPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//        }
//    }
//
//    private fun loadNewTrip() {
//        val entry: DashEntry? = item
//        if (entry == null) {
//            CoroutineScope(Dispatchers.Default).launch {
//                withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
//                    val newTrip = DashEntry()
//                    val newTripId = viewModel.insertSus(newTrip)
//                    newTripId
//                }.let { newTripId ->
//                    val currentTripFromService = locationService?.start(newTripId) { loc, trip ->
//                        Repository.get().saveModel(LocationData(loc = loc, entryId = trip))
//                    } ?: newTripId
//                    if (currentTripFromService != newTripId) {
//                        viewModel.loadDataModel(currentTripFromService)
//                        viewModel.deleteTrip(newTripId)
//                    } else {
//                        viewModel.loadDataModel(newTripId)
//                    }
//                }
//            }
//        } else {
//            locationService?.start(entry.id) { loc, tripId ->
//                Repository.get().saveModel(LocationData(loc, tripId))
//            }
//        }
//    }

    companion object {
        private const val LOC_SVC_CHANNEL_ID = "location_practice_0"
        private const val LOC_SVC_CHANNEL_NAME = "dt_mileage_tracker"
        private const val LOC_SVC_CHANNEL_DESC = "Dashtracker mileage tracker is active"

        internal const val REQ_KEY_START_DASH = "result: start dash dialog"
        internal const val ARG_RESULT = "arg: start dash dialog result"
        internal const val ARG_ENTRY_ID = "arg: start dash entry id"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "${PACKAGE_NAME}.extra.EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICAITON"
        private const val EXTRA_NOTIFICATION_CHANNEL =
            "${BuildConfig.APPLICATION_ID}.NotificationChannel"

        private fun getNotificationChannel() =
            NotificationChannel(
                LOC_SVC_CHANNEL_ID,
                LOC_SVC_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = LOC_SVC_CHANNEL_DESC
            }

        fun newInstance(entryId: Long): StartDashDialog =
            StartDashDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, entryId)
                }
            }
    }
}