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
import android.os.PersistableBundle
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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
import com.wtb.dashTracker.ui.activity_authenticated.AuthenticatedActivity
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity.Companion.ACTIVITY_RESULT_NEEDS_RESTART
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
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
import com.wtb.dashTracker.util.PermissionsHelper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.LOCATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_ONBOARD_INTRO
import com.wtb.dashTracker.util.REQUIRED_PERMISSIONS
import com.wtb.dashTracker.util.hasPermissions
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
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.system.exitProcess

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
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class MainActivity : AuthenticatedActivity(), ExpenseListFragmentCallback,
    IncomeFragment.IncomeFragmentCallback, ActiveDashBar.ActiveDashBarCallback {

    internal val permissionsHelper = PermissionsHelper(this)

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
    private var activeDash: ActiveDash? = null

    // Launchers
    private val onboardMileageTrackingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(TAG, "onboardMileageTracking result")
            if (this.hasPermissions(*REQUIRED_PERMISSIONS)
                && sharedPrefs.getBoolean(LOCATION_ENABLED, false)
            ) {
                Log.d(TAG, "onboardMileageTracking result | true")
                resumeMileageTracking()
            } else {
                Log.d(TAG, "onboardMileageTracking result | false")
            }
        }

    /**
     * Settings activity launcher - for launching [SettingsActivity] for result. checks data
     * intent for [ACTIVITY_RESULT_NEEDS_RESTART]. If true, settings which require the app to
     * restart were changed.
     */
    private val settingsActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val booleanExtra = it.data?.getBooleanExtra(ACTIVITY_RESULT_NEEDS_RESTART, false)
            Log.d(TAG, "activityResult | confirmRestart: $booleanExtra")
            if (booleanExtra == true) {
                restartApp()
            }
        }

    private val csvImportLauncher: ActivityResultLauncher<String> =
        CSVUtils(activity = this).getContentLauncher(
            importPacks = convertPacksImport,
            action = this::insertOrReplace,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState?.getBoolean(EXPECTED_EXIT) == true) {
            isAuthenticated = true
            expectedExit = false
        }

        if (intent.getBooleanExtra(EXTRA_SETTINGS_RESTART_APP, false)) {
            Log.d(TAG, "onCreate    | Restarted!")
            isAuthenticated = true
            expectedExit = true
            val intent = Intent(this, SettingsActivity::class.java)
            settingsActivityLauncher.launch(intent)
        }

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
                    ) { }.launch(enrollIntent)
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
                activeDash?.activeCpm = it
            }

            lifecycleScope.launch {
                this@MainActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.activeEntry.collectLatest {
                        if (activeDash == null) activeDash = ActiveDash()
                        activeDash?.activeEntry = it
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
         * Checks [ARG_RESULT] and [ARG_ENTRY_ID]. It loads [ARG_ENTRY_ID] as the active entry. If
         * [ARG_RESULT] is true, it passes [ARG_ENTRY_ID] to [ActiveDash.startTracking].
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
                    activeDash?.startTracking()
                }
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

        fun onFirstRun() {
            expectedExit = true
            startActivity((Intent(this, WelcomeActivity::class.java)))
        }

        super.onResume()
        Log.d(TAG, "onResume    | activeDash? ${activeDash != null}")

        if (sharedPrefs.getBoolean(PREF_SHOW_ONBOARD_INTRO, true)) {
            onFirstRun()
        } else {
            cleanupFiles()

            authenticate()
        }

        invalidateOptionsMenu()
    }

    override fun onPause() {
        Log.d(TAG, "onPause | activeDash: $activeDash | expectedExit? $expectedExit")
        activeDash?.apply {
            unbindLocationService()
        }

        activeDash = null

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putBoolean(EXPECTED_EXIT, expectedExit)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy |")
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val boolean = sharedPrefs.getBoolean(PREF_SHOW_BASE_PAY_ADJUSTS, true)
        Log.d(TAG, "onPrepareOptionsMenu | show? $boolean")
        menu?.findItem(R.id.action_new_weekly)?.isVisible =
            boolean

        return super.onPrepareOptionsMenu(menu)
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
                expectedExit = true
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
            R.id.action_settings -> {
                expectedExit = true
                val intent = Intent(this, SettingsActivity::class.java)
                settingsActivityLauncher.launch(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Calls [ActiveDash.resumeOrStartNewTrip] on [activeDash]
     *
     * @return null if [activeDash] is null
     */
    private fun resumeMileageTracking() {
        activeDash?.bindLocationService()
        activeDash?.resumeOrStartNewTrip()
    }

    /**
     * Calls [ActiveDash.stopLocationService]
     *
     */
    fun stopMileageTracking() {
        activeDash?.stopLocationService()
    }

    /**
     * Stops current dash by calling [ActiveDash.stopDash] on [entryId]
     */
    private fun endDash(entryId: Long? = null) {
        activeDash?.stopDash(entryId)
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

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val mainIntent =
            Intent.makeRestartActivityTask(intent?.component)
                .putExtra(EXTRA_SETTINGS_RESTART_APP, true)
        startActivity(mainIntent)
        exitProcess(0)
    }

    // AuthenticatedActivity
    override val onAuthentication: () -> Unit
        get() {
            fun unlockScreen() {
                binding.container.visibility = VISIBLE
                supportActionBar?.show()
            }

            /**
             * Sets content to visible
             */
            fun onUnlock() {
                Log.d(TAG, "login | onUnlock")
                isAuthenticated = true
                expectedExit = false
                unlockScreen()
            }

            fun onEndDashIntent(tripId: Long): () -> Unit = fun() {
                endDash(tripId)
                onUnlock()
            }

            val endDashExtra = intent?.getBooleanExtra(EXTRA_END_DASH, false)
            intent.removeExtra(EXTRA_END_DASH)
            val tripId = intent.getLongExtra(EXTRA_TRIP_ID, -40L)
            intent.removeExtra(EXTRA_TRIP_ID)

            return if (endDashExtra == true) {
                onEndDashIntent(tripId)
            } else {
                ::onUnlock
            }
        }

    override val onAuthFailed: (() -> Unit)? = null

    override val onAuthError: (() -> Unit)? = null

    override fun lockScreen() {
        binding.container.visibility = INVISIBLE
        supportActionBar?.hide()
    }

    /**
     * Keeps track of the current dash and current pause state.
     *
     */
    inner class ActiveDash {
        // Active Entry
        internal var activeEntry: FullEntry? = null
            set(value) {
                field = value
                value?.entry?.entryId?.let { id ->
                    updateLocationServiceNotificationData(id)
                }
                binding.adb.updateEntry(value, activeCpm)
            }
        private val activeEntryId: Long?
            get() = activeEntry?.entry?.entryId
        internal var activeCpm: Float? = 0f


        // Location Service
        private var locationService: LocationService? = null
        private var locationServiceConnection: ServiceConnection? = null
            set(value) {
                if (field != null) {
                    unbindLocationService()
                }
                field = value
            }
        private var locationServiceBound = false

        /**
         * lock to prevent location service from being started multiple times
         */
        private var startingService = false

        private var stopOnBind = false
        private var startOnBind = false
        private var startOnBindId: Long? = null

        init {
            bindLocationService()
        }

        /**
         * Checks for [REQUIRED_PERMISSIONS]. If they have already been granted, calls
         * [startLocationService], otherwise it requests the permissions using
         * [onboardMileageTrackingLauncher]
         */
        fun startTracking() {
            stopOnBind = false
            expectedExit = true
            onboardMileageTrackingLauncher.launch(
                Intent(this@MainActivity, OnboardingMileageActivity::class.java)
            )
        }

        /**
         * Opens [EndDashDialog] and calls [activeDash?.stopLocationService]
         *
         * @param entryId the id of the dash to stop
         */
        fun stopDash(entryId: Long?) {
            val id = entryId ?: activeEntryId ?: AUTO_ID
            viewModel.loadActiveEntry(null)
            EndDashDialog.newInstance(id)
                .show(supportFragmentManager, "end_dash_dialog")
            stopLocationService()
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

        /**
         * If [activeEntryId] is not null, calls [startLocationService] to start/restart the
         * location service. If [activeEntryId] is null, the location service is started with a new
         * [DashEntry]. If the location service is already started with a different [DashEntry],
         * that [DashEntry] is set as [activeEntry], otherwise the new [DashEntry] is.
         */
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

        /**
         * Calls [unbindService] on [locationServiceConnection], sets [locationServiceBound] to
         * false, [locationServiceConnection] to null, and [locationService] to null.
         */
        internal fun unbindLocationService() {
            if (locationServiceBound) {
                Log.d(TAG, "unbinding service, allegedly | locServConn: $locationServiceConnection")
                unbindService(locationServiceConnection!!)
                locationServiceBound = false
                locationServiceConnection = null
                locationService = null
//            }
            }
        }

        internal fun bindLocationService() {
            if (!locationServiceBound && !startingService) {
                startingService = true
                Log.d(TAG, "bindLocationServices | locServConn")
                val locationServiceIntent = Intent(applicationContext, LocationService::class.java)

                if (this@MainActivity.hasPermissions(*REQUIRED_PERMISSIONS)) {
                    val onBind: (() -> Unit)? =
                        activeEntryId?.let { id -> { startLocationService(id) } }
                    val conn = getLocationServiceConnection(onBind)
                    locationServiceConnection = conn

                    bindService(
                        locationServiceIntent,
                        locationServiceConnection!!,
                        BIND_AUTO_CREATE
                    )
                } else {
                    Log.d(TAG, "bindLocationService | missing permissions")
                    startingService = false
                }
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
        private fun startLocationService(tripId: Long): Long? {
            startOnBind = true
            startOnBindId = tripId
            startingService = false

            return locationService?.start(tripId, saveLocation)
        }

        /**
         * Returns a [ServiceConnection] that, in [ServiceConnection.onServiceConnected] saves the
         * service binding to [locationService], initiates observers on it, and loads the
         * [activeEntry] if the [LocationService] is already started. It will stop the service is
         * [stopOnBind] is set to true.
         *
         * @param onBind any actions to perform once the [LocationService] is bound
         * @return a [ServiceConnection]
         */
        private fun getLocationServiceConnection(onBind: (() -> Unit)? = null): ServiceConnection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    fun syncActiveEntryId() {
                        var tripId: Long? = activeEntryId

                        locationService?.tripId?.value?.let {
                            viewModel.loadActiveEntry(it)
                            tripId = it
                        }

                        updateLocationServiceNotificationData(tripId)
                    }

                    Log.d(TAG, "onServiceConnected")
                    val binder = service as LocationService.LocalBinder
                    locationService = binder.service
                    locationServiceBound = true

                    initLocSvcObserver()

                    if (stopOnBind) {
                        binder.service.stop()
                        stopOnBind = false
                    } else if (startOnBind) {
                        Log.d(TAG, "onServiceConnected | startOnBind: $activeEntryId")
                        (activeEntryId ?: startOnBindId)?.let { startLocationService(it) }
                        startOnBind = false
                        startOnBindId = null
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
                initialize(
                    notificationData = locServiceOngoingNotificationData,
                    notificationChannel = notificationChannel,
                    notificationText = { "Mileage tracking is on. Background location is in use." }
                )
            }
        }
    }

    companion object {
        private const val LOC_SVC_CHANNEL_ID = "location_practice_0"
        private const val LOC_SVC_CHANNEL_NAME = "Mileage Tracking"
        private const val LOC_SVC_CHANNEL_DESC = "DashTracker mileage tracker is active"

        private const val EXPECTED_EXIT = "expected_exit"

        //        internal const val ACTIVITY_RESULT_NEEDS_RESTART =
//            "${BuildConfig.APPLICATION_ID}.result_needs_restart"
        private const val EXTRA_SETTINGS_RESTART_APP =
            "${BuildConfig.APPLICATION_ID}.restart_settings"
        private const val EXTRA_END_DASH = "${BuildConfig.APPLICATION_ID}.End dash"
        private const val EXTRA_TRIP_ID = "${BuildConfig.APPLICATION_ID}.tripId"

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

/**
 * Authenticates user using [BiometricPrompt]
 */
fun FragmentActivity.authenticate(
    onSuccess: () -> Unit,
    onError: (() -> Unit)? = null,
    onFailed: (() -> Unit)? = null
) {
    val executor = ContextCompat.getMainExecutor(this)

    val biometricPrompt = BiometricPrompt(this, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.d(TAG, "login | error")
                onError?.invoke()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d(TAG, "login | failed")
                onFailed?.invoke()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock to access DashTracker")
        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(promptInfo)
}




