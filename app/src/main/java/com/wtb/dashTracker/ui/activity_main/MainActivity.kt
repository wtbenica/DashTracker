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
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View.*
import androidx.activity.result.ActivityResultLauncher
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.*
import com.wtb.dashTracker.databinding.ActivityMainBinding
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.toggleButtonAnimatedVectorDrawable
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogExport
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogImport
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.EndDashDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.EntryDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog.Companion.ARG_ENTRY_ID
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog.Companion.ARG_RESULT
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog.Companion.REQ_KEY_START_DASH_DIALOG
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_expense.ExpenseDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.fragment_expenses.ExpenseListFragment.ExpenseListFragmentCallback
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import com.wtb.dashTracker.util.*
import com.wtb.dashTracker.views.ActiveDashBar
import com.wtb.notificationutil.NotificationUtils
import dev.benica.csvutil.CSVUtils
import dev.benica.csvutil.ModelMap
import dev.benica.csvutil.getConvertPackImport
import dev.benica.mileagetracker.LocationService
import dev.benica.mileagetracker.LocationService.ServiceState.STOPPED
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val APP = "GT_"

internal val Any.TAG: String
    get() = APP + this::class.simpleName

/**
 * Primary [AppCompatActivity] for DashTracker. Contains [BottomNavigationView] for switching
 * between [IncomeFragment], [com.wtb.dashTracker.ui.fragment_expenses.ExpenseListFragment], and
 * [com.wtb.dashTracker.ui.fragment_trends.ChartsFragment];
 * [FloatingActionButton] for starting/stopping [LocationService]; new item menu for
 * [EntryDialog], [WeeklyDialog], and [ExpenseDialog]; options menu for
 * [ConfirmationDialogImport], [ConfirmationDialogExport],
 * [OssLicensesMenuActivity].
 */
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), ExpenseListFragmentCallback,
    IncomeFragment.IncomeFragmentCallback, ActiveDashBar.ActiveDashBarCallback {

    override fun onStop() {
        Log.d("PAUSE", "onStop | activeDash? ${activeDash != null}")
        super.onStop()
    }


    internal val sharedPrefs
        get() = getSharedPreferences(DT_SHARED_PREFS, 0)

    private val viewModel: MainActivityViewModel by viewModels()

    // IncomeFragmentCallback
    private val deductionTypeViewModel: DeductionTypeViewModel by viewModels()

    override val deductionType: StateFlow<DeductionType>
        get() = deductionTypeViewModel.deductionType

    override fun setDeductionType(dType: DeductionType) {
        deductionTypeViewModel.setDeductionType(dType)
    }

    // Bindings
    internal lateinit var binding: ActivityMainBinding
    private lateinit var mAdView: AdView

    // State
    private var expectedExit = false
//    private var explicitlyStopped = false

    private var activeDash: ActiveDash? = null
        set(value) {
            Log.d(
                "PAUSE",
                "MainActivity | setting activeDash from ${field.hashCode()} to ${value.hashCode()}"
            )
            field = value
        }

    /**
     * An [ActivityResultLauncher] that calls [getBgLocationPermission] if the requested
     * permissions are granted
     */
    private val locationPermLauncher: ActivityResultLauncher<Array<String>> =
        registerMultiplePermissionsLauncher(onGranted = ::getBgLocationPermission)

    /**
     * An [ActivityResultLauncher] that calls [ActiveDash.resumeOrStartNewTrip] if the requested
     * permission is granted
     */
    private val bgLocationPermLauncher: ActivityResultLauncher<String> =
        registerSinglePermissionLauncher(onGranted = ::resumeMileageTracking)

    private fun resumeMileageTracking() = activeDash?.resumeOrStartNewTrip()


    private val csvImportLauncher: ActivityResultLauncher<String> =
        CSVUtils(activity = this).getContentLauncher(
            importPacks = convertPacksImport,
            action = this::insertOrReplace,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate    |")

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

            viewModel.cpm.observe(this) {
                activeDash?.updateCpm(it)
            }

            lifecycleScope.launch {
                this@MainActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.activeEntry.collectLatest {
                        Log.d(
                            "PAUSE", "Entry incoming | activeDash? ${activeDash != null} | entry:" +
                                    " ${it?.entry?.entryId}"
                        )
                        Log.d(TAG, "Big Big entry incoming $activeDash")
                        if (activeDash == null) activeDash = ActiveDash()
                        activeDash?.updateEntry(it)
                    }
                }
            }

            lifecycleScope.launch {
                this@MainActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.currentPause.collectLatest {
                        Log.d(
                            "PAUSE",
                            "Pause incoming | activeDash? ${activeDash != null} | " +
                                    "pauseId: ${it?.pauseId}"
                        )
                        activeDash?.updatePause(it)
                    }
                }
            }
        }

        /**
         * Creates a new [DashEntry] and saves it. Opens a [StartDashDialog] with the
         * [DashEntry.entryId]
         *
         */
        fun showStartDashDialog() {
            CoroutineScope(Dispatchers.Default).launch {
                val id = viewModel.upsertAsync(DashEntry())
                StartDashDialog.newInstance(id)
                    .show(supportFragmentManager, "start_dash_dialog")
            }
        }

        /**
         * Sets [FloatingActionButton]'s [OnClickListener] to start/stop dashes. Initializes
         * [ActiveDashBar].
         */
        fun initMainActivityBinding() {
            binding = ActivityMainBinding.inflate(layoutInflater)

            binding.fab.setOnClickListener {
                if (binding.fab.tag == null || binding.fab.tag == R.drawable.anim_stop_to_play) {
                    showStartDashDialog()
                } else {
                    endDash()
                }
            }

            binding.adb.initialize(this)
        }

        /**
         * Checks [ARG_RESULT] and [ARG_ENTRY_ID]. It loads [ARG_ENTRY_ID] as the active entry. If
         * [ARG_RESULT] is true, it calls [startNewDash] with [ARG_ENTRY_ID].
         */
        fun setStartDashDialogResultListener() {
            supportFragmentManager.setFragmentResultListener(
                REQ_KEY_START_DASH_DIALOG,
                this
            ) { _, bundle ->
                val result = bundle.getBoolean(ARG_RESULT)
                val eid = bundle.getLong(ARG_ENTRY_ID)

                viewModel.loadActiveEntry(eid)

                if (result) {
                    startNewDash(eid)
                }
            }
        }

        installSplashScreen()
        Repository.initialize(this)
        supportActionBar?.title = "DashTracker"

        initMainActivityBinding()

        setContentView(binding.root)

        initBiometrics()
        initMobileAds()
        initBottomNavBar()
        initObservers()
        activeDash?.initLocSvcObserver()

        setStartDashDialogResultListener()
    }


    override fun onResume() {
        /**
         * Deletes any files created from import/export
         */
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

        /**
         * Sets content to visible
         */
        fun onUnlock() {
            isAuthenticated = true
            this@MainActivity.binding.container.visibility = VISIBLE
        }

        /**
         * Authenticates user using [BiometricPrompt]
         */
        fun authenticate() {
            val executor = ContextCompat.getMainExecutor(this)

            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onUnlock()
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock to access DashTracker")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        }

        super.onResume()
        Log.d("PAUSE", "onResume    | activeDash? ${activeDash != null}")
        cleanupFiles()

//        if (activeDash == null) {
//            Log.d("PAUSE", "I'm getting a new activeDash")
//            activeDash = ActiveDash()
//        } else {
//            Log.d("PAUSE", "I've already got an activeDash you idiot")
//        }
//
        val endDashExtra = intent?.getBooleanExtra(EXTRA_END_DASH, false)
        intent.removeExtra(EXTRA_END_DASH)
        val tripId = intent.getLongExtra(EXTRA_TRIP_ID, -40L)
        intent.removeExtra(EXTRA_TRIP_ID)

        if (endDashExtra == true) {
            endDash(tripId)
            onUnlock()
        } else {
            expectedExit = false

            if (!isAuthenticated) {
                authenticate()
            } else {
                onUnlock()
            }
        }
    }

    override fun onPause() {
        Log.d("PAUSE", "onPause | activeDash: $activeDash")
        activeDash?.apply {
            Log.d("PAUSE", "onPause | activeDash is not null, unbind location service")
            unbindLocationService()
        }

        if (!expectedExit) {
            isAuthenticated = false
            binding.container.visibility = INVISIBLE
        }

        activeDash = null

        super.onPause()
    }

    override fun onDestroy() {
        Log.d("PAUSE", "onDestroy |")
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun showImportConfirmationDialog() {
            ConfirmationDialogImport {
                viewModel.import(csvImportLauncher)
            }.show(supportFragmentManager, null)
        }

        fun showExportConfirmationDialog() {
            ConfirmationDialogExport {
                viewModel.export()
            }.show(supportFragmentManager, null)
        }

        return when (item.itemId) {
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
            R.id.action_new_entry -> {
                CoroutineScope(Dispatchers.Default).launch {
                    val id = viewModel.upsertAsync(DashEntry())
                    EntryDialog.newInstance(id).show(supportFragmentManager, "new_entry_dialog")
                }
                true
            }
            R.id.action_new_weekly -> {
                WeeklyDialog.newInstance().show(supportFragmentManager, "new_adjust_dialog")
                true
            }
            R.id.action_new_expense -> {
                CoroutineScope(Dispatchers.Default).launch {
                    val id = viewModel.upsertAsync(Expense(isNew = true))
                    ExpenseDialog.newInstance(id).show(supportFragmentManager, "new_expense_dialog")
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPauseResumeButtonClicked() {
        Log.d("PAUSE", "onPauseResumeButtonClicked")
        activeDash?.togglePauseLocationService()
    }

    /**
     * Starts [LocationService]. Requests required permissions
     *
     * @param eid the [DashEntry.entryId] that is being started
     */
    private fun startNewDash(eid: Long) {
        Log.d(TAG, "startNewDash | entry: $eid")
        activeDash?.start(eid)
    }

    /**
     * Stops current dash by calling [ActiveDash.stop] on [entryId]
     */
    private fun endDash(entryId: Long? = null) {
        activeDash?.stop(entryId)
    }

    /**
     * Calls [ActiveDash.stopLocationService]
     *
     */
    fun stopMileageTracking() {
        activeDash?.stopLocationService()
    }

    private fun insertOrReplace(models: ModelMap) {
        viewModel.insertOrReplace(
            entries = models.get<DashEntry, DashEntry.Companion>().ifEmpty { null },
            weeklies = models.get<Weekly, Weekly.Companion>().ifEmpty { null },
            expenses = models.get<Expense, Expense.Companion>().ifEmpty { null },
            purposes = models.get<ExpensePurpose, ExpensePurpose.Companion>().ifEmpty { null },
            locationData = models.get<LocationData, LocationData.Companion>().ifEmpty { null }
        )
    }

    /**
     * Requests [ACCESS_BACKGROUND_LOCATION], if needed, and  calls [resumeMileageTracking].
     */
    private fun getBgLocationPermission() {
        when {
            sharedPrefs.getBoolean(PREFS_DONT_ASK_BG_LOCATION, false) -> {}
            hasPermissions(this@MainActivity, ACCESS_BACKGROUND_LOCATION) -> {
                resumeMileageTracking()
            }
            shouldShowRequestPermissionRationale(ACCESS_BACKGROUND_LOCATION) -> {
                showRationaleBgLocation {
                    bgLocationPermLauncher.launch(ACCESS_BACKGROUND_LOCATION)
                        .also { expectedExit = true }
                }
            }
            else -> {
                bgLocationPermLauncher.launch(ACCESS_BACKGROUND_LOCATION)
                expectedExit = true
            }
        }
    }

    /**
     * Keeps track of the current dash and current pause state.
     *
     */
    inner class ActiveDash() {
        // Active Entry
        private var activeEntry: FullEntry? = null
        internal val activeEntryId: Long?
            get() = activeEntry?.entry?.entryId
        private var activeCpm: Float? = 0f

        private var currentPause: Pause? = null

        // Location Service
        private var locationService: LocationService? = null
        private var locationServiceConnection: ServiceConnection? = null
            set(value) {
                Log.d(TAG, "locServConn | before: $field | after: $value")
                if (field != null) {
                    unbindLocationService()
                }
                field = value
            }
        private var locationServiceBound = false

        private var stopOnBind = false
        private var startOnBind = false
        private var startId: Long? = null

        init {
            Log.d("PAUSE", "It's a new ActiveDash")
            bindLocationService()
        }

        fun start(entryId: Long) {
            Log.d(TAG, "start | entry: $entryId")
            stopOnBind = false
            when {
                sharedPrefs.getBoolean(PREFS_DONT_ASK_LOCATION, false) -> {}
                hasPermissions(this@MainActivity, *REQUIRED_PERMISSIONS) -> {
                    startLocationService(entryId)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                    expectedExit = true
                    showRationaleLocation {
                        locationPermLauncher.launch(LOCATION_PERMISSIONS)
                            .also { expectedExit = true }
                    }
                }
                else -> {
                    locationPermLauncher.launch(LOCATION_PERMISSIONS)
                    expectedExit = true
                }
            }
        }

        /**
         * Opens [EndDashDialog] and calls [activeDash?.stopLocationService]
         *
         * @param entryId the id of the dash to stop
         */
        fun stop(entryId: Long?) {
            val id = entryId ?: activeEntryId ?: AUTO_ID
            Log.d("PAUSE", "stop | activeEntry? ${activeEntry != null} | currentPause? " +
                    "${currentPause != null}")
            viewModel.loadActiveEntry(null)
            EndDashDialog.newInstance(id)
                .show(supportFragmentManager, "end_dash_dialog")
            stopLocationService()
        }

        fun updateEntry(entry: FullEntry?) {
            Log.d(TAG, "Update entry")
            activeEntry = entry
            activeEntryId?.let { id ->
                Log.d(TAG, "EBLOW: updateLocationServiceNotificationData:649 | $id")
                updateLocationServiceNotificationData(id)
            }
            binding.adb.updateEntry(entry, activeCpm)
        }

        fun updatePause(pause: Pause?) {
            Log.d("PAUSE", "updatePause | old: ${currentPause?.pauseId} | new ${pause?.pauseId}")
            currentPause = pause
            binding.adb.isPaused = pause != null
        }

        fun updateCpm(cpm: Float) {
            activeCpm = cpm
        }

        val isPaused: Boolean
            get() = currentPause != null

        fun togglePauseLocationService() {
            Log.d("PAUSE", "togglePauseLocationService | isPaused: $isPaused")
            if (isPaused) {
                resume()
            } else {
                pause()
            }
        }

        private fun resume() {
            Log.d("PAUSE", "resume | activeEntry? ${activeEntry != null} | currentPause? " +
                    "${currentPause != null}")
            currentPause?.apply {
                end = LocalDateTime.now()
            }?.also {
                CoroutineScope(Dispatchers.Default).launch {
                    viewModel.upsertAsync(it)
                }
            }
            viewModel.loadCurrentPause(null)
        }

        private fun pause() {
            Log.d("PAUSE", "pause")
            activeEntryId?.let {
                CoroutineScope(Dispatchers.Default).launch {
                    val pauseId = viewModel.upsertAsync(
                        Pause(entry = it, start = LocalDateTime.now())
                    )
                    viewModel.loadCurrentPause(pauseId)
                }
            }
        }

        var startingService = false

        fun bindLocationService() {
            if (!locationServiceBound && !startingService) {
                startingService = true
                Log.d(TAG, "bindLocationServices | locServConn")
                val locationServiceIntent = Intent(applicationContext, LocationService::class.java)

                if (hasPermissions(this@MainActivity, *REQUIRED_PERMISSIONS)) {
                    Log.d(TAG, "bindLocationService | actually binding the service")
                    val onBind: (() -> Unit)? =
                        activeEntryId?.let { id -> { startLocationService(id) } }
                    val conn = getLocationServiceConnection(onBind)
                    Log.d(TAG, "locServConn | $conn")
                    locationServiceConnection = conn

                    bindService(
                        locationServiceIntent,
                        locationServiceConnection!!,
                        BIND_AUTO_CREATE
                    )
                } else {
                    Log.d(TAG, "bindLocationService | pretending to bind the service")
                }
            }
        }

        fun unbindLocationService() {
            Log.d(
                TAG,
                "unbindLocationServices | locServBound? $locationServiceBound $locationService"
            )
            if (locationServiceBound) {
                Log.d(TAG, "unbinding service, allegedly | locServConn: $locationServiceConnection")
                unbindService(locationServiceConnection!!)
                locationServiceBound = false
                locationServiceConnection = null
                locationService = null
//            }
            }
        }

        /**
         * Calls [LocationService.start] on [locationService] using [saveLocation] as the
         * locationHandler parameter.
         *
         * @param tripId passed to [LocationService.start] as the newTripId parameter
         * @return the id that [locationService] is using. If the service was not already started, it
         * will be the same as [tripId]. If the return value is not the same as [tripId], it means
         * that the service was already started with a different id
         */
        internal fun startLocationService(tripId: Long): Long? {
            Log.d(TAG, "startLocationService | entry: $tripId")
            startOnBind = true
            startId = tripId
            startingService = false

            return locationService?.start(tripId, saveLocation)
        }

        /**
         * If [locationService] is not null, calls [LocationService.stop]  If [locationService]
         * is null, meaning the service is not currently bound, the service will be stopped once
         * it is bound.
         */
        fun stopLocationService() {
            locationService.let {
                if (it != null) {
                    it.stop()
                } else {
                    stopOnBind = true
                }
            }
        }

        /**
         * Returns a [ServiceConnection] that, in [ServiceConnection.onServiceConnected] saves the
         * service binding to [locationService], initiates observers on it, and loads the
         * [activeEntry] if the [LocationService] is already started. It will stop the service is
         * [stopOnBind] is set to true.
         *
         * @param onBind any action to perform once the [LocationService] is bound
         * @return a [ServiceConnection]
         */
        internal fun getLocationServiceConnection(onBind: (() -> Unit)? = null): ServiceConnection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    fun syncActiveEntryId() {
                        var tripId: Long? = activeEntryId

                        locationService?.tripId?.value?.let {
                            viewModel.loadActiveEntry(it)
                            tripId = it
                        }

                        Log.d(TAG, "EBLOW: updateLocationServiceNotificationData:799 | $tripId")
                        updateLocationServiceNotificationData(tripId)
                    }

                    Log.d(TAG, "onServiceConnected")
                    val binder = service as LocationService.LocalBinder
                    locationService = binder.service
                    Log.d(TAG, "locServConn | bound <- true")
                    locationServiceBound = true

                    initLocSvcObserver()

                    if (stopOnBind) {
                        binder.service.stop()
                        stopOnBind = false
                    } else if (startOnBind) {
                        Log.d(TAG, "onServiceConnected | startOnBind: $activeEntryId")
                        (activeEntryId ?: startId)?.let { startLocationService(it) }
                        Log.d(TAG, "onServiceConnected | setting startOnBind false")
                        startOnBind = false
                        startId = null
                    } else {
                        syncActiveEntryId()
                    }

                    onBind?.invoke()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.d(TAG, "onServiceDisconnected")
                    locationService = null
                    locationServiceBound = false
                }
            }

        /**
         * Updates [locationService]'s ongoing notification data
         *
         * @param tripId the id of the active trip
         */
        private fun updateLocationServiceNotificationData(tripId: Long?) {
            fun getOpenActivityAction(context: Context): NotificationCompat.Action {
                fun getLaunchActivityPendingIntent(): PendingIntent? {
                    val launchActivityIntent = Intent(context, MainActivity::class.java)
                        .putExtra(EXTRA_END_DASH, false)
                        .putExtra(EXTRA_TRIP_ID, tripId ?: -35L)

                    return PendingIntent.getActivity(
                        context,
                        0,
                        launchActivityIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

                return NotificationCompat.Action(
                    R.drawable.ic_launch,
                    "Open",
                    getLaunchActivityPendingIntent()
                )
            }

            fun getStopServiceAction(ctx: Context): NotificationCompat.Action {
                fun getEndDashPendingIntent(id: Long?): PendingIntent? {
                    Log.d(TAG, "EBLOW: New End Dash Intent: $id")
                    val intent = Intent(ctx, MainActivity::class.java)
                        .putExtra(EXTRA_END_DASH, true)
                        .putExtra(EXTRA_TRIP_ID, id ?: -30L)

                    return PendingIntent.getActivity(
                        ctx,
                        1,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

                return NotificationCompat.Action(
                    R.drawable.ic_cancel,
                    "Stop",
                    getEndDashPendingIntent(tripId)
                )
            }

            fun getStop(entryId: Long): (Context) -> NotificationCompat.Action =
                { context: Context ->
                    fun getEndDashPendingIntent(id: Long?): PendingIntent? {
                        Log.d(TAG, "EBLOW: New End Dash Intent: $id")
                        val intent = Intent(context, MainActivity::class.java)
                            .putExtra(EXTRA_END_DASH, true)
                            .putExtra(EXTRA_TRIP_ID, id ?: -30L)

                        return PendingIntent.getActivity(
                            context,
                            1,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    }

                    NotificationCompat.Action(
                        R.drawable.ic_cancel,
                        "Stop",
                        getEndDashPendingIntent(entryId)
                    )
                }

            val locServiceOngoingNotificationData =
                NotificationUtils.NotificationData(
                    contentTitle = R.string.app_name,
                    bigContentTitle = R.string.app_name,
                    icon = R.mipmap.icon_c,
                    actions = listOf(
                        ::getOpenActivityAction,
                        ::getStopServiceAction
                    )
                )

            locationService?.apply {
                Log.d(
                    TAG,
                    "EBLOW: Initializing locServ $tripId"
                )
                initialize(
                    notificationData = locServiceOngoingNotificationData,
                    notificationChannel = notificationChannel,
                    notificationText = { "Mileage tracking is on. Background location is in use." }
                )
            }
        }

        /**
         * Monitors [locationService] and expands/collapses active entry bar, updates pause/resume
         * button and start/stop FAB.
         */
        fun initLocSvcObserver() {
            fun toggleFabToPlay() {
                binding.fab.apply {
                    if (tag == R.drawable.anim_play_to_stop) {
                        toggleButtonAnimatedVectorDrawable(
                            initialDrawable = R.drawable.anim_stop_to_play,
                            otherDrawable = R.drawable.anim_play_to_stop
                        )
                    }
                }
            }

            fun toggleFabToStop() {
                binding.fab.apply {
                    if (tag == null || tag == R.drawable.anim_stop_to_play) {
                        toggleButtonAnimatedVectorDrawable(
                            initialDrawable = R.drawable.anim_play_to_stop,
                            otherDrawable = R.drawable.anim_stop_to_play
                        )
                    }
                }
            }

            lifecycleScope.launchWhenStarted {
                locationService?.serviceState?.collectLatest { state ->
                    binding.adb.updateServiceState(state)

                    when (state) {
                        STOPPED -> toggleFabToPlay()
                        else -> toggleFabToStop()
                    }
                }
            }
        }

        fun resumeOrStartNewTrip() {
            val id = activeEntryId
            if (id != null) {
                startLocationService(id)
            } else {
                CoroutineScope(Dispatchers.Default).launch {
                    withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
                        val newEntry = DashEntry()
                        val newEntryId = viewModel.insertSus(newEntry)
                        newEntryId
                    }.let { newTripId ->
                        val currentTripFromService =
                            startLocationService(newTripId) ?: newTripId

                        if (currentTripFromService != newTripId) {
                            viewModel.loadActiveEntry(currentTripFromService)
                            viewModel.deleteEntry(newTripId)
                        } else {
                            viewModel.loadActiveEntry(newTripId)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private var isAuthenticated = false

        private const val DT_SHARED_PREFS = "dashtracker_prefs"
        internal const val PREFS_DONT_ASK_LOCATION = "Don't ask | location"
        internal const val PREFS_DONT_ASK_BG_LOCATION = "Don't ask | bg location"
        private const val LOC_SVC_CHANNEL_ID = "location_practice_0"
        private const val LOC_SVC_CHANNEL_NAME = "Mileage Tracking"
        private const val LOC_SVC_CHANNEL_DESC = "DashTracker mileage tracker is active"

        private const val EXTRA_END_DASH = "${BuildConfig.APPLICATION_ID}.End dash"
        private const val EXTRA_TRIP_ID = "${BuildConfig.APPLICATION_ID}.tripId"

        private const val ARG_EXPECTED_EXIT = "expected_exit"

        private val notificationChannel =
            NotificationChannel(
                LOC_SVC_CHANNEL_ID,
                LOC_SVC_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = LOC_SVC_CHANNEL_DESC
            }

        @ColorInt
        internal fun getColorFab(context: Context) = getAttrColor(context, R.attr.colorFab)

        @ColorInt
        internal fun getColorFabDisabled(context: Context) =
            getAttrColor(context, R.attr.colorFabDisabled)

        @ColorInt
        internal fun getAttrColor(context: Context, @AttrRes id: Int): Int {
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

        // Not sure why, but this doesn't work when it's a fun and passed as a lambda, but it does
        // work when it's a val
        private val saveLocation: (Location, Long, Int, Int, Int, Int) -> Unit =
            { loc: Location,
              entryId: Long,
              still: Int,
              car: Int,
              foot: Int,
              unknown: Int ->
                Repository.get().saveModel(
                    LocationData(
                        loc = loc,
                        entryId = entryId,
                        still = still,
                        car = car,
                        foot = foot,
                        unknown = unknown
                    )
                )
            }
    }
}

/**
 * Marker interface for a fragment that adjusts based on [DeductionType]. Does not currently
 * serve a purpose
 */
interface DeductionCallback


