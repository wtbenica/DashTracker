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
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.AnimatedVectorDrawable
import android.location.Location
import android.os.Bundle
import android.os.IBinder
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
import androidx.annotation.DrawableRes
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
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.EndDashDialog
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
import dev.benica.mileagetracker.LocationService.Companion.EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION
import dev.benica.mileagetracker.LocationService.ServiceState.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

const val TESTING = false

@ExperimentalCoroutinesApi
val Any.TAG: String
    get() = APP + this::class.simpleName

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), ExpenseListFragmentCallback,
    IncomeFragment.IncomeFragmentCallback {

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
    private lateinit var activeDashBinding: ActiveDashBarBinding
    private lateinit var mAdView: AdView

    private val menuItems: List<FabMenuButtonInfo> = listOf(
        FabMenuButtonInfo(
            "Start Dash",
            R.drawable.ic_play_arrow
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                val id = viewModel.upsertAsync(DashEntry())
                StartDashDialog.newInstance(id).show(supportFragmentManager, "start_dash_dialog")
            }
        },
        FabMenuButtonInfo(
            "Add Entry",
            R.drawable.ic_new_entry
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                val id = viewModel.upsertAsync(DashEntry())
                EntryDialog.newInstance(id).show(supportFragmentManager, "new_entry_dialog")
            }
        },
        FabMenuButtonInfo(
            "Add Adjustment",
            R.drawable.ic_new_adjust
        ) { WeeklyDialog.newInstance().show(supportFragmentManager, "new_adjust_dialog") },
        FabMenuButtonInfo(
            "Add Expense",
            R.drawable.ic_nav_daily
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                val id = viewModel.upsertAsync(Expense(isNew = true))
                ExpenseDialog.newInstance(id).show(supportFragmentManager, "new_expense_dialog")
            }
        },
        //        FabMenuButtonInfo(
        //            "Add Payout",
        //            R.drawable.chart
        //        ) { PayoutDialog().show(supportFragmentManager, "new_payout_dialog") }
    )

    // State
    private var expectedExit = false
    private var explicitlyStopped = false

    @DrawableRes
    private var trackingDrawable: Int = R.drawable.status_tracking

    // Active Entry
    private var activeEntry: FullEntry? = null
    private var activeEntryId: Long? = null

    // Location Service
    private var locationService: LocationService? = null
    private var locationServiceBound = false

    private val locationPermLauncher =
        registerMultiplePermissionsLauncher(onGranted = ::getBgLocationPermission)

    private val bgLocationPermLauncher =
        registerSinglePermissionLauncher(onGranted = ::loadNewTrip)

    private val csvImportLauncher =
        CSVUtils(activity = this).getContentLauncher(
            importPacks = convertPacksImport,
            action = this::insertOrReplace,
        )

    private val blinkAnimator: ValueAnimator = ValueAnimator.ofFloat(0.4f, 1f).apply {
        duration = 800
        repeatMode = ValueAnimator.REVERSE
        repeatCount = -1
    }

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

            lifecycleScope.launch {
                this@MainActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.activeEntry.collectLatest {
                        it.let { entry: FullEntry? ->
                            Log.d(TAG, "incoming entry | id: ${entry?.entry?.entryId}")
                            activeEntry = it
                            activeEntryId = it?.entry?.entryId
                            activeEntryId?.let { id -> updateLocationServiceNotificationData(id) }
                            Log.d(TAG, "Setting activeEntryId: $activeEntryId")

                            it?.let { e ->
                                activeDashBinding.valMileage.text =
                                    getString(R.string.mileage_fmt, e.distance)
                                val t: Float = ChronoUnit.MINUTES.between(
                                    e.entry.startDateTime,
                                    LocalDateTime.now()
                                ) / 60f
                                activeDashBinding.valElapsedTime.text =
                                    getString(R.string.float_fmt, t)
                            }
                        }
                    }
                }
            }
        }

        fun initMainActivityBinding() {
            binding = ActivityMainBinding.inflate(layoutInflater)
            binding.fab.initialize(menuItems, binding.container)
        }

        fun initActiveDashBinding() {
            activeDashBinding = binding.activeDashBar
            activeDashBinding.startButton.apply {
                tag = tag ?: R.drawable.anim_pause_to_play
                setOnClickListener {
                    locationService?.let {
                        Log.d(
                            TAG,
                            "startButton onClick: Service state = ${it.serviceState.value.name}"
                        )
                        when (it.serviceState.value) {
                            TRACKING_ACTIVE -> it.pause()
                            TRACKING_INACTIVE -> it.pause()
                            PAUSED -> it.start(AUTO_ID) { _, _, _, _, _, _ -> }
                            STOPPED -> {} // Do Nothing
                        }
                    }
                }
            }

            activeDashBinding.stopButton.setOnClickListener { endDash() }

            blinkAnimator.apply {
                addUpdateListener { animation ->
                    activeDashBinding.statusIndicator.alpha = animation.animatedValue as Float
                }
                blinkAnimator.start()
            }
        }

        installSplashScreen()
        Repository.initialize(this)
        supportActionBar?.title = "DashTracker"

        initMainActivityBinding()
        initActiveDashBinding()

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
            val eid = bundle.getLong(ARG_ENTRY_ID)

            Log.d(TAG, "load entry 2 | $eid")
            viewModel.loadEntry(eid)

            if (result) {
                explicitlyStopped = false
                when {
                    hasPermissions(this, *REQUIRED_PERMISSIONS) -> {
                        Log.d(TAG, "Result | loadNewTrip")
                        startLocationService(eid)
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
        }
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
         * Sets content to visible, bind [LocationService] and starts it if [activeEntry] is not
         * null.
         */
        fun onUnlock() {
            isAuthenticated = true
            this@MainActivity.binding.container.visibility = VISIBLE

            val locationServiceIntent = Intent(applicationContext, LocationService::class.java)

            val onBind: (() -> Unit)? = activeEntryId?.let { id -> { startLocationService(id) } }
            locationServiceConnection = getLocationServiceConnection(onBind)

            bindService(
                locationServiceIntent,
                locationServiceConnection!!,
                BIND_AUTO_CREATE
            )
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
        Log.d(TAG, "onResume    |")
        cleanupFiles()

        val endDashExtra = intent?.getBooleanExtra(EXTRA_END_DASH, false)
        intent.removeExtra(EXTRA_END_DASH)

        if (endDashExtra == true) {
            val tripId = intent.getLongExtra(EXTRA_TRIP_ID, -1L)
            intent.removeExtra(EXTRA_TRIP_ID)

            onUnlock()
            endDash(tripId)
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
        Log.d(TAG, "ON_PAUSE")
        if (locationServiceBound) {
            unbindService(locationServiceConnection!!)
            locationServiceBound = false
            locationServiceConnection = null
            locationService = null
        }

        super.onPause()
        if (!expectedExit) {
            isAuthenticated = false
            binding.container.visibility = INVISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(ARG_EXPECTED_EXIT, true)
        outState.putLong(ARG_ENTRY_ID, activeEntryId ?: -1L)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        binding.fab.interceptTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Clears [activeEntry], stops [locationService], and opens [EndDashDialog].
     *
     * @param entryId the id o
     */
    private fun endDash(entryId: Long? = activeEntryId) {
        Log.d(TAG, "endDash | load entry 1 | $entryId / null")
        viewModel.loadEntry(null)
        EndDashDialog.newInstance(entryId ?: AUTO_ID)
            .show(supportFragmentManager, "end_dash_dialog")

        stopLocationService()
    }

    /**
     * Requests [ACCESS_BACKGROUND_LOCATION], if needed, and  calls [loadNewTrip].
     */
    private fun getBgLocationPermission() {
        Log.d(TAG, "getBgLocationPermission")
        when {
            hasPermissions(this, ACCESS_BACKGROUND_LOCATION) -> {
                Log.d(TAG, "BgLocationPermissions | loadNewTrip")
                loadNewTrip()
            }
            shouldShowRequestPermissionRationale(ACCESS_BACKGROUND_LOCATION) -> {
                showRationaleBgLocation {
                    Log.d(TAG, "BgLocationPermissions | onGranted -> loadNewTrip")
                    bgLocationPermLauncher.launch(ACCESS_BACKGROUND_LOCATION)
                        .also { expectedExit = true }
                }
            }
            else -> {
                Log.d(TAG, "BgLocationPermissions | onGranted -> loadNewTrip")
                bgLocationPermLauncher.launch(ACCESS_BACKGROUND_LOCATION)
                expectedExit = true
            }
        }
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
     * If [activeEntry] is not null, [locationService] will be started using the [activeEntryId],
     * otherwise it will attempt to start [locationService] with a new trip/tripId. If
     * [locationService] has already been started with a different id, [activeEntry]/[activeEntryId]
     * will be updated to match.
     */
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
                    Log.d(TAG, "loc start 3")
                    val currentTripFromService =
                        startLocationService(newTripId) ?: newTripId

                    if (currentTripFromService != newTripId) {
                        Log.d(TAG, "load entry 4 | $currentTripFromService")
                        viewModel.loadEntry(currentTripFromService)
                        viewModel.deleteEntry(newTripId)
                    } else {
                        Log.d(TAG, "load entry 5 | $newTripId")
                        viewModel.loadEntry(newTripId)
                    }
                }
            }
        } else {
            Log.d(TAG, "loc start 4")
            startLocationService(activeEntryId ?: entry!!.id)
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
        Log.d(
            TAG,
            "startLocationService    | Service is ${if (locationService == null) "not " else ""}connected"
        )
        return locationService?.start(tripId, saveLocation)
    }

    // TODO: Is it necessary to check id when stopping? What happens when it doesn't match? I forgot, so check it out at some point
    /**
     * If [locationService] is not null, calls [LocationService.stop] with [id] as the parameter.
     * If [locationService] is null, meaning the service is not currently bound, the service will
     * be stopped once it is bound.
     *
     * @param id the tripId to stop using for location tracking
     */
    fun stopLocationService(id: Long? = null) {
        locationService.let {
            if (it != null)
                it.stop(id)
            else
                explicitlyStopped = true
        }
    }

    private var locationServiceConnection: ServiceConnection? = null

    /**
     * Returns a [ServiceConnection] that, in [ServiceConnection.onServiceConnected] saves the
     * service binding to [locationService], initiates observers on it, and loads the
     * [activeEntry] if the [LocationService] is already started. It will stop the service is
     * [explicitlyStopped] is set to true.
     *
     * @param onBind any action to perform once the [LocationService] is bound
     * @return a [ServiceConnection]
     */
    private fun getLocationServiceConnection(onBind: (() -> Unit)? = null): ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                fun syncActiveEntryId() {
                    var tripId: Long? = activeEntryId

                    locationService?.tripId?.value?.let {
                        Log.d(TAG, "load entry 6 | $it")
                        viewModel.loadEntry(it)
                        tripId = it
                    }

                    updateLocationServiceNotificationData(tripId)
                }

                Log.d(TAG, "onServiceConnected")
                val binder = service as LocationService.LocalBinder
                locationService = binder.service
                locationServiceBound = true
                initLocSvcObservers()

                syncActiveEntryId()

                onBind?.invoke()

                if (explicitlyStopped) {
                    stopLocationService()
                    explicitlyStopped = false
                }
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
        fun endDashPendingIntent(tripId: Long?): PendingIntent? {
            val intent = Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_END_DASH, true)
                .putExtra(EXTRA_TRIP_ID, tripId ?: AUTO_ID)

            return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun launchActivityPendingIntent(): PendingIntent? {
            val launchActivityIntent = Intent(this, MainActivity::class.java)

            return PendingIntent.getActivity(
                this,
                0,
                launchActivityIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun cancelServicePendingIntent(): PendingIntent? {
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

        fun locServiceOngoingNotificationData(tripId: Long? = null) =
            NotificationUtils.NotificationData(
                contentTitle = R.string.app_name,
                bigContentTitle = R.string.app_name,
                icon = R.mipmap.icon_c,
                actions = listOf(
                    NotificationCompat.Action(
                        R.drawable.ic_launch,
                        "Open",
                        launchActivityPendingIntent()
                    ),
                    NotificationCompat.Action(
                        R.drawable.ic_cancel,
                        "Stop",
                        endDashPendingIntent(tripId)
                    )
                )
            )

        locationService?.apply {
            initialize(
                notificationData = locServiceOngoingNotificationData(tripId),
                notificationChannel = getNotificationChannel(),
                notificationText = { "Mileage tracking is on. Background location is in use." }
            )

            if (TESTING)
                setIsTesting(true)
        }
    }

    /**
     * Monitors [locationService] and updates active entry box, play/pause button,
     * and tracking status indicator to match. Also updates still/in car/on foot confidence
     * numbers (this can/should be removed at some point)
     */
    private fun initLocSvcObservers() {
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

        lifecycleScope.launchWhenStarted {
            locationService?.serviceState?.collectLatest { state ->
                when (state) {
                    TRACKING_ACTIVE -> {
                        fun toggleIt() {
                            activeDashBinding.startButton.apply {
                                if (tag == R.drawable.anim_pause_to_play) {
                                    togglePlayPauseButton(this)
                                }
                            }
                        }

                        trackingDrawable = R.drawable.status_tracking
                        blinkAnimator.start()

                        if (activeDashBinding.root.visibility == GONE) {
                            activeDashBinding.root.expand { toggleIt() }
                        } else {
                            toggleIt()
                        }
                    }

                    TRACKING_INACTIVE -> {
                        fun toggleIt() {
                            activeDashBinding.startButton.apply {
                                if (tag == R.drawable.anim_pause_to_play) {
                                    togglePlayPauseButton(this)
                                }
                            }
                        }

                        trackingDrawable = R.drawable.status_inactive
                        blinkAnimator.pause()

                        if (activeDashBinding.root.visibility == GONE) {
                            activeDashBinding.root.expand { toggleIt() }
                        } else {
                            toggleIt()
                        }
                    }

                    PAUSED -> {
                        fun toggleIt() {
                            activeDashBinding.startButton.apply {
                                if (tag == R.drawable.anim_play_to_pause) {
                                    togglePlayPauseButton(this)
                                }
                            }
                        }

                        trackingDrawable = R.drawable.status_paused
                        blinkAnimator.pause()

                        if (activeDashBinding.root.visibility == GONE) {
                            activeDashBinding.root.expand { toggleIt() }
                        } else {
                            toggleIt()
                        }
                    }

                    STOPPED -> {
                        trackingDrawable = R.drawable.status_paused
                        blinkAnimator.pause()

                        if (activeDashBinding.root.visibility == VISIBLE) {
                            activeDashBinding.root.collapse()
                        }
                    }
                }

                activeDashBinding.statusIndicator.setImageResource(trackingDrawable)
            }
        }

//        lifecycleScope.launchWhenStarted {
//            locationService?.stillVal?.collectLatest {
//                activeDashBinding.stillValue.text = it.toString()
//            }
//        }
//
//        lifecycleScope.launchWhenStarted {
//            locationService?.carVal?.collectLatest {
//                activeDashBinding.carValue.text = it.toString()
//            }
//        }
//
//        lifecycleScope.launchWhenStarted {
//            locationService?.footVal?.collectLatest {
//                activeDashBinding.footValue.text = it.toString()
//            }
//        }
//
//        lifecycleScope.launchWhenStarted {
//            locationService?.unknownVal?.collectLatest {
//                activeDashBinding.unknownValue.text = it.toString()
//            }
//        }
    }

    companion object {
        const val APP = "GT_"
        var isAuthenticated = false

        private const val LOC_SVC_CHANNEL_ID = "location_practice_0"
        private const val LOC_SVC_CHANNEL_NAME = "dt_mileage_tracker"
        private const val LOC_SVC_CHANNEL_DESC = "Dashtracker mileage tracker is active"

        private const val EXTRA_NOTIFICATION_CHANNEL =
            "${BuildConfig.APPLICATION_ID}.NotificationChannel"
        private const val EXTRA_END_DASH = "${BuildConfig.APPLICATION_ID}.End dash"
        private const val EXTRA_TRIP_ID = "${BuildConfig.APPLICATION_ID}.tripId"

        private const val ARG_EXPECTED_EXIT = "expected_exit"

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

interface DeductionCallback


