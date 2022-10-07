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
import android.os.Build
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
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
import com.wtb.dashTracker.ui.activity_get_permissions.GetPermissionsActivity
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.MileageTrackingOptIn
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.MileageTrackingOptIn.OPT_IN
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.MileageTrackingOptIn.OPT_OUT
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
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val APP = "GT_"
private const val IS_TESTING = false
private var IS_FIRST = true

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
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), ExpenseListFragmentCallback,
    IncomeFragment.IncomeFragmentCallback, ActiveDashBar.ActiveDashBarCallback {

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
     * An [ActivityResultLauncher] that calls [getBgLocationPermResumeTracking] if the requested
     * permissions are granted
     */
    private val locationPermResumeTrackingLauncher: ActivityResultLauncher<Array<String>> =
        registerMultiplePermissionsLauncher(onGranted = ::getBgLocationPermResumeTracking)

    private val locationPermLauncher: ActivityResultLauncher<Array<String>> =
        registerMultiplePermissionsLauncher(onGranted = ::getBgLocationPerm)

    private val bgLocationPermLauncher: ActivityResultLauncher<String> =
        registerSinglePermissionLauncher()

    /**
     * An [ActivityResultLauncher] that calls [ActiveDash.resumeOrStartNewTrip] if the requested
     * permission is granted
     */
    private val bgLocationPermResumeTrackingLauncher: ActivityResultLauncher<String> =
        registerSinglePermissionLauncher(onGranted = ::resumeMileageTracking)

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->

            @Suppress("DEPRECATION")
            val optInExtra =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    activityResult?.data?.getSerializableExtra(
                        ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN,
                        MileageTrackingOptIn::class.java
                    )
                } else {
                    activityResult?.data?.getSerializableExtra(
                        ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN
                    ) as MileageTrackingOptIn?
                }

            if (activityResult?.resultCode == RESULT_OK) {
                sharedPrefs.edit().putBoolean(PREFS_OPT_OUT_LOCATION, optInExtra == OPT_OUT).apply()
            }
        }

    private val welcomeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            onUnlock()

            @Suppress("DEPRECATION")
            val optInExtra =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    activityResult?.data?.getSerializableExtra(
                        ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN,
                        MileageTrackingOptIn::class.java
                    )
                } else {
                    activityResult?.data?.getSerializableExtra(
                        ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN
                    ) as MileageTrackingOptIn?
                }

            Log.d(
                TAG,
                "resultCode: ${activityResult?.resultCode == RESULT_OK} | optInExtra: ${optInExtra?.name}"
            )
            if (activityResult?.resultCode == RESULT_OK) {
                sharedPrefs.edit().putBoolean(PREFS_SHOULD_SHOW_INTRO, false).apply()
                sharedPrefs.edit().putBoolean(PREFS_OPT_OUT_LOCATION, optInExtra == OPT_OUT).apply()

                if (optInExtra == OPT_IN) {
                    permissionsLauncher.launch(
                        Intent(this, GetPermissionsActivity::class.java)
                    )
                }
            }
        }

    /**
     * Calls [ActiveDash.resumeOrStartNewTrip] on [activeDash]
     *
     * @return null if [activeDash] is null
     */
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
                    activeDash?.startTracking(eid)
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
            fun getPermissions() {
                when {
                    sharedPrefs.getBoolean(PREFS_OPT_OUT_LOCATION, false) -> {}
                    hasPermissions(this@MainActivity, *REQUIRED_PERMISSIONS) -> {}
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                        showRationaleLocation {
                            locationPermLauncher.launch(LOCATION_PERMISSIONS)
                        }
                        expectedExit = true
                    }
                    else -> {
                        locationPermLauncher.launch(LOCATION_PERMISSIONS)
                        expectedExit = true
                    }
                }
            }

            welcomeLauncher.launch(Intent(this, WelcomeActivity::class.java))

//                // run intro: initial settings
//                //      - Use authentication?
//                //      - Use mileage tracking
//                //          - ask for permissions
//                ConfirmationDialog.newInstance(
//                    text = R.string.whats_new,
//                    requestKey = "Wubba Wubba",
//                    title = "What's new",
//                    posButton = R.string.enable,
//                    posAction = LambdaWrapper {
//                        sharedPrefs.edit().putBoolean(PREFS_SHOULD_SHOW_INTRO, false).apply()
//                        getPermissions()
//                    },
//                    negButton = R.string.decline,
//                    negAction = LambdaWrapper {
//                        sharedPrefs.edit().putBoolean(PREFS_DONT_ASK_LOCATION, true).apply()
//                        sharedPrefs.edit().putBoolean(PREFS_SHOULD_SHOW_INTRO, false).apply()
//                    }
//                ).show(supportFragmentManager, null)
        }

        fun onEndDashIntent(tripId: Long): () -> Unit = fun() {
            endDash(tripId)
            onUnlock()
        }


        /**
         * Authenticates user using [BiometricPrompt]
         */
        fun authenticate(onSuccess: () -> Unit) {
            Log.d(TAG, "login | authenticate")
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
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Log.d(TAG, "login | failed")
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

        if ((IS_TESTING && IS_FIRST) || sharedPrefs.getBoolean(PREFS_SHOULD_SHOW_INTRO, true)) {
            IS_FIRST = false

            onFirstRun()
        } else {
            cleanupFiles()

            val endDashExtra = intent?.getBooleanExtra(EXTRA_END_DASH, false)
            intent.removeExtra(EXTRA_END_DASH)
            val tripId = intent.getLongExtra(EXTRA_TRIP_ID, -40L)
            intent.removeExtra(EXTRA_TRIP_ID)

            if (!isAuthenticated) {

                val onSuccess: () -> Unit =
                    if (endDashExtra == true) {
                        onEndDashIntent(tripId)
                    } else {
                        ::onUnlock
                    }

                authenticate(onSuccess)

            } else {

                if (endDashExtra == true) {
                    onEndDashIntent(tripId)
                } else {
                    onUnlock()
                }

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
            lockScreen()
        }

        activeDash = null

        super.onPause()
    }

    /**
     * Sets content to visible
     */
    private fun onUnlock() {
        Log.d(TAG, "login | onUnlock")
        isAuthenticated = true
        expectedExit = false
        unlockScreen()
    }

    private fun lockScreen() {
        binding.container.visibility = INVISIBLE
    }

    private fun unlockScreen() {
        binding.container.visibility = VISIBLE
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
            else -> super.onOptionsItemSelected(item)
        }
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

    /**
     * Requests [ACCESS_BACKGROUND_LOCATION], if needed, and  calls [resumeMileageTracking].
     */
    private fun getBgLocationPermResumeTracking() {
        when {
            sharedPrefs.getBoolean(PREFS_DONT_ASK_BG_LOCATION, false) -> {}
            hasPermissions(this@MainActivity, ACCESS_BACKGROUND_LOCATION) -> {
                resumeMileageTracking()
            }
            shouldShowRequestPermissionRationale(ACCESS_BACKGROUND_LOCATION) -> {
                showRationaleBgLocation {
                    bgLocationPermResumeTrackingLauncher.launch(ACCESS_BACKGROUND_LOCATION)
                        .also { expectedExit = true }
                }
            }
            else -> {
                bgLocationPermResumeTrackingLauncher.launch(ACCESS_BACKGROUND_LOCATION)
                expectedExit = true
            }
        }
    }

    private fun getBgLocationPerm() {
        when {
            sharedPrefs.getBoolean(PREFS_DONT_ASK_BG_LOCATION, false) -> {}
            hasPermissions(this@MainActivity, ACCESS_BACKGROUND_LOCATION) -> {}
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
                Log.d(TAG, "locServConn | before: $field | after: $value")
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
         * [startLocationService] on [entryId], otherwise it requests the permissions using
         * [locationPermResumeTrackingLauncher]
         *
         * @param entryId passed as the argument to [startLocationService] if permissions have
         * already been granted
         */
        fun startTracking(entryId: Long) {
            stopOnBind = false
            when {
                sharedPrefs.getBoolean(PREFS_OPT_OUT_LOCATION, false) -> {}
                hasPermissions(this@MainActivity, *REQUIRED_PERMISSIONS) -> {
                    startLocationService(entryId)
                }
//                hasPermissions(this@MainActivity, *LOCATION_PERMISSIONS) -> {
//                    getBgLocationPermission()
//                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                    showRationaleLocation {
                        locationPermResumeTrackingLauncher.launch(LOCATION_PERMISSIONS)
                    }
                    expectedExit = true
                }
                else -> {
                    locationPermResumeTrackingLauncher.launch(LOCATION_PERMISSIONS)
                    expectedExit = true
                }
            }
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
        fun unbindLocationService() {
            if (locationServiceBound) {
                Log.d(TAG, "unbinding service, allegedly | locServConn: $locationServiceConnection")
                unbindService(locationServiceConnection!!)
                locationServiceBound = false
                locationServiceConnection = null
                locationService = null
//            }
            }
        }

        private fun bindLocationService() {
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
                        (activeEntryId ?: startOnBindId)?.let { startLocationService(it) }
                        Log.d(TAG, "onServiceConnected | setting startOnBind false")
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
    }

    companion object {
        private var isAuthenticated = false

        internal const val DT_SHARED_PREFS = "dashtracker_prefs"
        internal const val PREFS_OPT_OUT_LOCATION = "Don't ask | location"
        internal const val PREFS_DONT_ASK_BG_LOCATION = "Don't ask | bg location"
        internal const val PREFS_OPT_OUT_NOTIFICATION = "Don't ask | notifications"
        internal const val PREFS_SHOULD_SHOW_INTRO = "Run Intro"
        private const val LOC_SVC_CHANNEL_ID = "location_practice_0"
        private const val LOC_SVC_CHANNEL_NAME = "Mileage Tracking"
        private const val LOC_SVC_CHANNEL_DESC = "DashTracker mileage tracker is active"

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


