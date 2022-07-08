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

package com.wtb.dashTracker.ui.activity_main

import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.IBinder
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View.*
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.*
import com.wtb.dashTracker.databinding.ActiveDashBarBinding
import com.wtb.dashTracker.databinding.ActivityMainBinding
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.expand
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.activity_main.MainActivity.Companion.APP
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogExport
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogImport
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.EntryDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog.Companion.ARG_ENTRY_ID
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog.Companion.ARG_RESULT
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog.Companion.REQ_KEY_START_DASH
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_expense.ExpenseDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.fragment_expenses.ExpenseListFragment.ExpenseListFragmentCallback
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import com.wtb.dashTracker.util.*
import com.wtb.dashTracker.views.FabMenuButtonInfo
import com.wtb.notificationutil.NotificationUtils
import dev.benica.csvutil.CSVUtils
import dev.benica.csvutil.ModelMap
import dev.benica.csvutil.getConvertPackImport
import dev.benica.mileagetracker.LocationService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@ExperimentalCoroutinesApi
val Any.TAG: String
    get() = APP + this::class.simpleName

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), ExpenseListFragmentCallback,
    IncomeFragment.IncomeFragmentCallback {

    private val viewModel: MainActivityViewModel by viewModels()
    private val deductionTypeViewModel: DeductionTypeViewModel by viewModels()

    override val deductionType: StateFlow<DeductionType>
        get() = deductionTypeViewModel.deductionType

    internal lateinit var binding: ActivityMainBinding
    private lateinit var activeDashBinding: ActiveDashBarBinding
    private lateinit var mAdView: AdView

    private val contentZipLauncher =
        CSVUtils(activity = this).getContentLauncher(
            importPacks = convertPacksImport,
            action = this::insertOrReplace,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun initBiometrics() {
            val biometricManager = BiometricManager.from(this)
            when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                        )
                    }
                    registerForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) {}.launch(enrollIntent)
                }
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    // "App can authenticate using biometrics.")
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    // "No biometric features available on this device.")
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    // "Biometric features are currently unavailable.")
                }
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    // "Biometric features are currently unavailable. (update required)")
                }
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    // "Biometric features are currently unavailable. (unsupported)")
                }
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    // "Biometric features are currently unavailable. (unknown)")
                }
            }
        }

        fun initMobileAds() {
            MobileAds.initialize(this@MainActivity)

            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(
                        listOf(
                            "B7667F22237B480FF03CE252659EAA82",
                            "04CE17DF0350024007F75AE926597C03"
                        )
                    ).build()
            )

            mAdView = binding.adView
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
        }

        fun initBottomNavBar() {
            val navView: BottomNavigationView = binding.navView
            navView.background = null

            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_income,
                    R.id.navigation_expenses,
                    R.id.navigation_insights
                )
            )

            val navHostFragment: Fragment? =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
            val navController: NavController? = navHostFragment?.findNavController()

            navController?.let {
                setupActionBarWithNavController(it, appBarConfiguration)
                navView.setupWithNavController(it)
                it.addOnDestinationChangedListener { _, destination, _ ->
                    when (destination.id) {
                        R.id.navigation_income -> binding.appBarLayout.setExpanded(true)
                        R.id.navigation_expenses -> binding.appBarLayout.setExpanded(true)
                        R.id.navigation_insights -> binding.appBarLayout.setExpanded(false)
                    }
                }
            }
        }

        fun initObservers() {
            viewModel.hourly.observe(this) {
                binding.actMainHourly.text =
                    getCurrencyString(it)
            }

            viewModel.thisWeek.observe(this) {
                binding.actMainThisWeek.text =
                    getCurrencyString(it)
            }

            viewModel.lastWeek.observe(this) {
                binding.actMainLastWeek.text =
                    getCurrencyString(it)
            }


            lifecycleScope.launch {
                this@MainActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.activeEntry.collectLatest {
                        it?.let { entry: FullEntry ->
                            Log.d(TAG, "New active entry incoming")
                            activeEntry = it
                            activeEntryId = it.entry.entryId
                            activeDashBinding.valMileage.text =
                                getString(R.string.mileage_fmt, entry.distance)
                            val t: Float = ChronoUnit.MINUTES.between(
                                entry.entry.startDateTime,
                                LocalDateTime.now()
                            ) / 60f
                            activeDashBinding.valElapsedTime.text =
                                getString(R.string.float_fmt, t)
                        }
                    }
                }
            }
        }

        installSplashScreen()
        Repository.initialize(this)
        supportActionBar?.title = "DashTracker"

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.fab.initialize(getMenuItems(supportFragmentManager), binding.container)

        activeDashBinding = binding.activeDashBar
        activeDashBinding.startButton.apply {
            tag = tag ?: R.drawable.anim_pause_to_play
            setOnClickListener {
                locationService?.let {
                    Log.d(TAG, "startButton onClick: Service state = ${it.serviceState.value.name}")
                    when (it.serviceState.value) {
                        LocationService.ServiceState.TRACKING -> it.pause()
                        LocationService.ServiceState.PAUSED -> it.start(0) { _, _ -> }
                        LocationService.ServiceState.STOPPED -> {} // Do Nothing
                    }
                }

//            togglePlayPauseButton(it as ImageButton)
            }
        }

        activeDashBinding.stopButton.setOnClickListener {
            locationService?.stop()
        }

        setContentView(binding.root)

        initBiometrics()
        initMobileAds()
        initBottomNavBar()
        initObservers()
        initLocSvcObservers()

        supportFragmentManager.setFragmentResultListener(
            REQ_KEY_START_DASH,
            this
        ) { requestKey, bundle ->
            val result = bundle.getBoolean(ARG_RESULT)
            activeEntryId = bundle.getLong(ARG_ENTRY_ID)
            Log.d(TAG, "THIS IS ENTRY ID: $activeEntryId")
            viewModel.loadEntry(activeEntryId)

            if (result) {
                Log.d(TAG, "Start Dash received by activity")
                when {
                    hasPermissions(this, *REQUIRED_PERMISSIONS) -> {
                        Log.d(TAG, "has all location permissions")
                        loadNewTrip()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                        Log.d(TAG, "needs some permissions, needs rationale")
                        expectedExit = true
                        showRationaleLocation {
                            locationPermLauncher.launch(LOCATION_PERMISSIONS)
                        }
                    }
                    else -> {
                        expectedExit = true
                        Log.d(TAG, "needs some permissions, no rationale")
                        locationPermLauncher.launch(LOCATION_PERMISSIONS)
                    }
                }
            }
        }
    }

    fun togglePlayPauseButton(btn: ImageButton) {
        btn.run {
            when (tag ?: R.drawable.anim_play_to_pause) {
                R.drawable.anim_pause_to_play -> {
                    setImageResource(R.drawable.anim_play_to_pause)
                    tag = R.drawable.anim_play_to_pause
                }
                R.drawable.anim_play_to_pause -> {
                    setImageResource(R.drawable.anim_pause_to_play)
                    tag = R.drawable.anim_pause_to_play
                }
            }
            when (val d = drawable) {
                is AnimatedVectorDrawableCompat -> d.start()
                is AnimatedVectorDrawable -> d.start()
            }
        }
    }

    private fun initLocSvcObservers() {
        lifecycleScope.launch {
            this@MainActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                locationService?.serviceState?.collectLatest {
                    when (it) {
                        LocationService.ServiceState.TRACKING -> {
                            fun toggleIt() {
                                activeDashBinding.startButton.apply {
                                    if (tag == R.drawable.anim_pause_to_play) {
                                        togglePlayPauseButton(this)
                                    }
                                }
                            }
                            if (activeDashBinding.root.visibility == GONE)
                                activeDashBinding.root.expand { toggleIt() }
                            else
                                toggleIt()
                        }
                        LocationService.ServiceState.PAUSED -> {
                            fun toggleIt() {
                                activeDashBinding.startButton.apply {
                                    if (tag == R.drawable.anim_play_to_pause) {
                                        togglePlayPauseButton(this)
                                    }
                                }
                            }

                            if (activeDashBinding.root.visibility == GONE)
                                activeDashBinding.root.expand { toggleIt() }
                            else
                                toggleIt()
                        }
                        LocationService.ServiceState.STOPPED -> {
                            if (activeDashBinding.root.visibility == VISIBLE)
                                activeDashBinding.root.collapse()
                        }
                        else -> {
                            // Do nothing, I don't care
                        }
                    }
                }
            }
        }
    }

    private var expectedExit = false
    private val locationPermLauncher =
        registerMultiplePermissionsLauncher(onGranted = ::getBgLocationPermission)

    private val bgLocationPermLauncher =
        registerSinglePermissionLauncher(onGranted = ::loadNewTrip)

    private fun getBgLocationPermission() {
        Log.d(TAG, "getBgLocationPermission")
        when {
            hasPermissions(this, ACCESS_BACKGROUND_LOCATION) -> {
                Log.d(TAG, "has bg location permission")
                loadNewTrip()
            }
            shouldShowRequestPermissionRationale(ACCESS_BACKGROUND_LOCATION) -> {
                Log.d(TAG, "needs bg location permission, needs rationale")
                showRationaleBgLocation { bgLocationPermLauncher.launch(ACCESS_BACKGROUND_LOCATION) }
            }
            else -> {
                Log.d(TAG, "needs bg location permission, no rationale")
                bgLocationPermLauncher.launch(ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    override fun onResume() {
        fun cleanupFiles() {
            if (fileList().isNotEmpty()) {
                fileList().forEach { name ->
                    val date = Regex("^[A-Za-z_]*_").replace(name.split(".")[0], "")
                    try {
                        val parsedDate = LocalDate.parse(
                            date, DateTimeFormatter.ofPattern("yyyy_MM_dd")
                        )
                        if (parsedDate <= LocalDate.now().minusDays(2)) {
                            val file = File(filesDir, name)
                            file.delete()
                        }
                    } catch (e: DateTimeParseException) {
                        // continue
                    }
                }
            }
        }

        fun authenticate() {
            val executor = ContextCompat.getMainExecutor(this)

            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticated = true
                        this@MainActivity.binding.container.visibility = VISIBLE


                        val locationServiceIntent =
                            Intent(this@MainActivity, LocationService::class.java)

                        bindService(
                            locationServiceIntent,
                            locationServiceConnection,
                            Context.BIND_AUTO_CREATE
                        )
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock to access DashTracker")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        }

        super.onResume()
        Log.d(TAG, "onResume")
        cleanupFiles()
        expectedExit = false
        if (!isAuthenticated) {
            authenticate()
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        if (locationServiceBound) {
            unbindService(locationServiceConnection)
            locationServiceBound = false
            locationService = null
        }

        super.onPause()
        if (!expectedExit) {
            isAuthenticated = false
            binding.container.visibility = INVISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_licenses -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            R.id.action_export_to_csv -> {
                showExportConfirmationDialog()
                true
            }
            R.id.action_import_from_csv -> {
                showImportConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun showImportConfirmationDialog() {
        ConfirmationDialogImport {
            viewModel.import(contentZipLauncher)
        }.show(supportFragmentManager, null)
    }

    private fun showExportConfirmationDialog() {
        ConfirmationDialogExport {
            viewModel.export()
        }.show(supportFragmentManager, null)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        binding.fab.interceptTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun insertOrReplace(models: ModelMap) {
        viewModel.insertOrReplace(
            entries = models.get<DashEntry, DashEntry.Companion>().ifEmpty { null },
            weeklies = models.get<Weekly, Weekly.Companion>().ifEmpty { null },
            expenses = models.get<Expense, Expense.Companion>().ifEmpty { null },
            purposes = models.get<ExpensePurpose, ExpensePurpose.Companion>()
                .ifEmpty { null }
        )
    }

    private fun getMenuItems(fm: FragmentManager): List<FabMenuButtonInfo> = listOf(
        FabMenuButtonInfo(
            "Start Dash",
            R.drawable.ic_play_arrow
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                val id = viewModel.upsertAsync(DashEntry())
                StartDashDialog.newInstance(id).show(fm, "start_dash_dialog")
            }
        },
        FabMenuButtonInfo(
            "Add Entry",
            R.drawable.ic_new_entry
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                val id = viewModel.upsertAsync(DashEntry())
                EntryDialog.newInstance(id).show(fm, "new_entry_dialog")
            }
        },
        FabMenuButtonInfo(
            "Add Adjustment",
            R.drawable.ic_new_adjust
        ) { WeeklyDialog.newInstance().show(fm, "new_adjust_dialog") },
        FabMenuButtonInfo(
            "Add Expense",
            R.drawable.ic_nav_daily
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                val id = viewModel.upsertAsync(Expense(isNew = true))
                ExpenseDialog.newInstance(id).show(fm, "new_expense_dialog")
            }
        },
        //        FabMenuButtonInfo(
        //            "Add Payout",
        //            R.drawable.chart
        //        ) { PayoutDialog().show(fm, "new_payout_dialog") }
    )

    override fun setDeductionType(dType: DeductionType) {
        deductionTypeViewModel.setDeductionType(dType)
    }

    companion object {
        const val APP = "GT_"
        var isAuthenticated = false

        private const val LOC_SVC_CHANNEL_ID = "location_practice_0"
        private const val LOC_SVC_CHANNEL_NAME = "dt_mileage_tracker"
        private const val LOC_SVC_CHANNEL_DESC = "Dashtracker mileage tracker is active"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "${ContactsContract.Directory.PACKAGE_NAME}.extra.EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICAITON"
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

        @ColorInt
        fun getColorFab(context: Context) = getAttrColor(context, R.attr.colorFab)

        @ColorInt
        fun getColorFabDisabled(context: Context) =
            getAttrColor(context, R.attr.colorFabDisabled)

        @ColorInt
        fun getAttrColor(context: Context, @AttrRes id: Int): Int {
            val tv = TypedValue()
            val arr = context.obtainStyledAttributes(tv.data, intArrayOf(id))
            @ColorInt val color = arr.getColor(0, 0)
            arr.recycle()
            return color
        }

        private val convertPacksImport = listOf(
            DashEntry.getConvertPackImport(),
            Weekly.getConvertPackImport(),
            Expense.getConvertPackImport(),
            ExpensePurpose.getConvertPackImport(),
            LocationData.getConvertPackImport()
        )
    }

    var activeEntry: FullEntry? = null
    var activeEntryId: Long? = null
    private var locationService: LocationService? = null
    private var locationServiceBound = false

    private fun loadNewTrip() {
        Log.d(TAG, "loadNewTrip")
        val entry: DashEntry? = activeEntry?.entry
        if (entry == null && activeEntryId == null) {
            Log.d(TAG, "loadNewTrip | new entry")
            CoroutineScope(Dispatchers.Default).launch {
                withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
                    val newEntry = DashEntry()
                    val newEntryId = viewModel.insertSus(newEntry)
                    newEntryId
                }.let { newTripId ->
                    val currentTripFromService =
                        locationService?.start(newTripId) { loc, entryId ->
                            Repository.get()
                                .saveModel(LocationData(loc = loc, entryId = entryId))
                        } ?: newTripId
                    if (currentTripFromService != newTripId) {
                        viewModel.loadEntry(currentTripFromService)
                        viewModel.deleteEntry(newTripId)
                    } else {
                        viewModel.loadEntry(newTripId)
                    }
                }
            }
        } else {
            Log.d(TAG, "loadNewTrip | existing entry")
            locationService?.start(activeEntryId ?: entry!!.id) { loc, id ->
                Repository.get().saveModel(LocationData(loc, id))
            }
        }
    }

    private val locationServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocalBinder

            locationService = binder.service
            locationServiceBound = true
            locationService?.tripId?.value.let { viewModel.loadEntry(it) }
            locationService?.apply {
                initialize(
                    notificationData = locationServiceNotifData,
                    notificationChannel = getNotificationChannel(),
                    notificationText = { "Mileage tracking is on. Background location is in use." }
                )
//                setIsTesting(true)
            }
            initLocSvcObservers()
        }


        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            locationServiceBound = false
        }
    }

    val locationServiceNotifData
        get() = NotificationUtils.NotificationData(
            contentTitle = R.string.app_name,
            bigContentTitle = R.string.app_name,
            icon = R.mipmap.icon_c,
            actions = listOf(
                NotificationCompat.Action(
                    R.drawable.ic_launch,
                    "Open",
                    launchActivityPendingIntent
                ),
                NotificationCompat.Action(
                    R.drawable.ic_cancel,
                    "Stop",
                    cancelServicePendingIntent
                )
            )
        )

    private val cancelServicePendingIntent: PendingIntent?
        get() {
            val cancelIntent = Intent(this, LocationService::class.java).apply {
                putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)
            }
            return PendingIntent.getService(
                this,
                0,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

    private val launchActivityPendingIntent: PendingIntent?
        get() {
            val launchActivityIntent = Intent(this, MainActivity::class.java)

            return PendingIntent.getActivity(
                this,
                0,
                launchActivityIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
}

interface DeductionCallback


