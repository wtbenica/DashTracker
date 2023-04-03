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

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.View.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.updatePadding
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
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.*
import com.wtb.dashTracker.databinding.ActivityMainBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.activity_authenticated.AuthenticatedActivity
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity.Companion.ACTIVITY_RESULT_NEEDS_RESTART
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity.Companion.EXTRA_SETTINGS_ACTIVITY_IS_AUTHENTICATED
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity.Companion.INTENT_EXTRA_PRE_AUTH
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogExport
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogImport
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog.Companion.ARG_MODIFICATION_STATE
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog.Companion.REQUEST_KEY_DATA_MODEL_DIALOG
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog.ModificationState
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.EndDashDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.EntryDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog.Companion.ARG_ENTRY_ID
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog.Companion.REQ_KEY_START_DASH_DIALOG
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.StartDashDialog.Companion.RESULT_START_DASH_CONFIRM_START
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_expense.ExpenseDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.fragment_expenses.fragment_dailies.ExpenseListFragment
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment.IncomeFragmentCallback
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import com.wtb.dashTracker.util.PermissionsHelper.Companion.LOCATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_ONBOARD_INTRO
import com.wtb.dashTracker.util.REQUIRED_PERMISSIONS
import com.wtb.dashTracker.util.hasPermissions
import com.wtb.dashTracker.views.ActiveDashBar
import com.wtb.dashTracker.views.ActiveDashBar.ActiveDashBarCallback
import com.wtb.dashTracker.views.ActiveDashBar.Companion.ADBState
import com.wtb.dashTracker.views.DTFloatingActionButton
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState.EXPANDED
import com.wtb.notificationutil.NotificationUtils
import dev.benica.csvutil.CSVUtils
import dev.benica.csvutil.ModelMap
import dev.benica.csvutil.getConvertPackImport
import dev.benica.mileagetracker.LocationService
import dev.benica.mileagetracker.LocationService.ServiceState
import dev.benica.mileagetracker.LocationService.ServiceState.*
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

private var IS_TESTING = true

private const val DEBUGGING = true
internal fun Any.debugLog(message: String, condition: Boolean = true) {
    if (DEBUGGING && condition) Log.d(TAG, message)
}

internal fun Any.errorLog(message: String) {
    if (DEBUGGING) Log.e(TAG, message)
}

/**
 * Primary [AppCompatActivity] for DashTracker. Contains [BottomNavigationView] for switching
 * between [IncomeFragment], [ExpenseListFragment], and
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
class MainActivity : AuthenticatedActivity(),
    IncomeFragmentCallback,
    ActiveDashBarCallback,
    ListItemFragment.ListItemFragmentCallback {
    private val viewModel: MainActivityViewModel by viewModels()

    private val deductionTypeViewModel: DeductionTypeViewModel by viewModels()
    override val deductionType: StateFlow<DeductionType>
        get() = deductionTypeViewModel.deductionType

    // Bindings
    internal lateinit var binding: ActivityMainBinding
    private lateinit var mAdView: AdView

    // State
    private val activeDash: ActiveDash = ActiveDash()
    private var showingWelcomeScreen = false

    @IdRes
    private var currDestination: Int = R.id.navigation_insights

    /**
     * Flag that prevents [EndDashDialog] from being shown if the active entry was deleted, as
     * opposed to when it was stopped
     */
    private var activeEntryDeleted: Boolean = false

    /**
     * Flag to differentiate user-input scrolling vs programmatic scrolling
     */
    internal var isShowingOrHidingToolbars: Boolean = false

    // TODO: This is in permissionsHelper, no?
    /**
     * Has [REQUIRED_PERMISSIONS] && pref [LOCATION_ENABLED] is true
     */
    private val trackingEnabled: Boolean
        get() {
            val hasPermissions = this.hasPermissions(*REQUIRED_PERMISSIONS)
            val isEnabled = sharedPrefs.getBoolean(LOCATION_ENABLED, false)

            return hasPermissions && isEnabled
        }
    private var trackingEnabledPrevious: Boolean = false

    // Launchers
    /**
     * Launcher for [OnboardingMileageActivity]. On result, calls [ActiveDash.resumeOrStartNewTrip]
     */
    private val onboardMileageTrackingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (trackingEnabled) {
                activeDash.resumeOrStartNewTrip()
            }
        }

    /**
     * Settings activity launcher - for launching [SettingsActivity] for result. checks data
     * intent for [ACTIVITY_RESULT_NEEDS_RESTART]. If true, settings which require the app to
     * restart were changed.
     */
    private val settingsActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val needsRestart = it.data?.getBooleanExtra(ACTIVITY_RESULT_NEEDS_RESTART, false)

            if (needsRestart == true) {
                restartApp()
            } else {
                val auth = it.data?.getBooleanExtra(EXTRA_SETTINGS_ACTIVITY_IS_AUTHENTICATED, false)

                if (auth != true) {
                    authenticate()
                } else {
                    onUnlock()
                }
            }
        }

    private val csvImportLauncher: ActivityResultLauncher<String> =
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
                            "738E1F6D1C2057B3E7863A4081A77CB5", // Pixel 6a
                            "B7667F22237B480FF03CE252659EAA82",
                            "04CE17DF0350024007F75AE926597C03"
                        )
                    ).build()
            )

            mAdView = binding.adView
            @SuppressLint("VisibleForTests") val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
        }

        fun initBottomNavBar() {
            val navView: BottomNavigationView = binding.navView

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
                    currDestination = destination.id
                    runOnUiThread {
                        when (destination.id) {
                            R.id.navigation_income -> {
                                binding.fab.show()
                            }
                            R.id.navigation_expenses -> {
                                binding.fab.show()
                            }
                            R.id.navigation_insights -> {
                                binding.fab.hide()
                            }
                        }

                        activeDash.apply {
                            serviceState = updateActiveDashState(
                                afterId = activeDash.activeEntry?.entry?.entryId,
                                beforeId = activeDash.activeEntry?.entry?.entryId,
                                showMini = destination.id != R.id.navigation_income
                            )
                        }
                    }
                }
            }
        }

        fun initObservers() {
            viewModel.thisWeekHourly.observe(this) {
                binding.summaryBar.actMainThisWeekHourly.text =
                    getCurrencyString(it)
            }

            viewModel.lastWeekHourly.observe(this) {
                binding.summaryBar.actMainLastWeekHourly.text =
                    getCurrencyString(it)
            }

            viewModel.thisWeekEarnings.observe(this) {
                binding.summaryBar.actMainThisWeekTotal.text =
                    getCurrencyString(it)
            }

            viewModel.lastWeekEarnings.observe(this) {
                binding.summaryBar.actMainLastWeekTotal.text =
                    getCurrencyString(it)
            }

            viewModel.cpm.observe(this) {
                activeDash.activeCpm = it
            }

            lifecycleScope.launch {
                this@MainActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.activeEntry.collectLatest {
                        activeDash.activeEntry = it
                    }
                }
            }
        }

        /**
         * Creates a new [DashEntry] and saves it. Opens a [StartDashDialog] with the
         * [DashEntry.entryId]
         */
        fun showStartDashDialog() {
            CoroutineScope(Dispatchers.Default).launch {
                val id = viewModel.upsertAsync(DashEntry())
                StartDashDialog.newInstance(id)
                    .show(supportFragmentManager, "start_dash_dialog")
            }
        }

        /**
         * Checks [RESULT_START_DASH_CONFIRM_START] and [ARG_ENTRY_ID]. It loads [ARG_ENTRY_ID] as the active entry. If
         * [RESULT_START_DASH_CONFIRM_START] is true, it passes [ARG_ENTRY_ID] to [MainActivityViewModel.loadActiveEntry].
         */
        fun setStartDashDialogResultListener() {
            supportFragmentManager.setFragmentResultListener(
                REQ_KEY_START_DASH_DIALOG,
                this
            ) { _, bundle ->
                val result = bundle.getBoolean(RESULT_START_DASH_CONFIRM_START)
                val entryId = bundle.getLong(ARG_ENTRY_ID, -1L)

                if (result) {
                    viewModel.loadActiveEntry(entryId)
                    sharedPrefs.edit().putLong(ACTIVE_ENTRY_ID, entryId).apply()
                }
            }

            /**
             * Checks [ARG_MODIFICATION_STATE] result from an [EntryDialog]. If it is
             * [ModificationState.DELETED], calls [clearActiveEntry] before
             * [MainActivityViewModel.deleteEntry]
             */
            supportFragmentManager.setFragmentResultListener(
                REQUEST_KEY_DATA_MODEL_DIALOG,
                this
            ) { _, bundle ->
                val modifyState = bundle.getString(ARG_MODIFICATION_STATE)
                    ?.let { ModificationState.valueOf(it) }

                val id = bundle.getLong(EditDataModelDialog.ARG_MODIFIED_ID, -1L)

                if (modifyState == ModificationState.DELETED && id != -1L) {
                    clearActiveEntry(id)
                    viewModel.deleteEntry(id)
                }
            }
        }

        /**
         * Sets [FloatingActionButton]'s [OnClickListener] to start/stop dashes. Initializes
         * [ActiveDashBar].
         */
        fun initMainActivityBinding() {
            fun isRecyclerViewAtTop(): Boolean {
                val currFrag: Fragment? =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)?.childFragmentManager?.fragments?.last()

                return currFrag is ScrollableFragment && currFrag.isAtTop
            }

            binding = ActivityMainBinding.inflate(layoutInflater).apply {
                fab.setOnClickListener {
                    when (fab.currentState) {
                        DTFloatingActionButton.Companion.FabState.DASH_INACTIVE -> {
                            showStartDashDialog()
                        }
                        DTFloatingActionButton.Companion.FabState.DASH_ACTIVE -> {
                            stopDash()
                        }
                        DTFloatingActionButton.Companion.FabState.EXPENSE_FRAG -> {
                            CoroutineScope(Dispatchers.Default).launch {
                                val id = viewModel.upsertAsync(Expense(isNew = true))
                                ExpenseDialog.newInstance(id)
                                    .show(supportFragmentManager, "new_expense_dialog")
                            }
                        }
                    }
                }

                adb.initialize(this@MainActivity)

                appBarLayout.apply {
                    expandedState = EXPANDED

                    addOnOffsetChangedListener { appBar: AppBarLayout, offset: Int ->
                        val appBarIsHidden = appBar.height + offset == 0
                        val forceShowBottomAppBar =
                            offset == 0 && bottomAppBar.isScrolledDown && isRecyclerViewAtTop()

                        when {
                            !isShowingOrHidingToolbars && forceShowBottomAppBar -> {
                                isShowingOrHidingToolbars = true
                                with(binding) {
                                    bottomAppBar.performShow(true)
                                    CoroutineScope(Dispatchers.Default).launch {
                                        runOnUiThread {
                                            fab.show()
                                        }
                                    }
                                }
                            }
                            isShowingOrHidingToolbars -> {
                                if (appBarIsHidden) {
                                    isShowingOrHidingToolbars = false
                                }
                            }
                        }
                    }


                    summaryBar.root.expandedState = EXPANDED
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

        activeDash.initLocSvcObserver()

        setStartDashDialogResultListener()

        if (savedInstanceState?.getBoolean(ARG_EXPECTED_EXIT) == true) {
            isAuthenticated = true
            expectedExit = false
        }

        if (intent.getBooleanExtra(EXTRA_SETTINGS_RESTART_APP, false)) {
            isAuthenticated = true
            expectedExit = true
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra(INTENT_EXTRA_PRE_AUTH, true)
            }
            settingsActivityLauncher.launch(intent)
        }

        isTesting = IS_TESTING
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
            showingWelcomeScreen = true
            startActivity((Intent(this, WelcomeActivity::class.java)))
        }

        super.onResume()

        if (expectedExit) {
            isAuthenticated = true
            expectedExit = false
        }

        val shouldShowWelcome = sharedPrefs.getBoolean(PREF_SHOW_ONBOARD_INTRO, true)
        if (!showingWelcomeScreen && (isTesting || shouldShowWelcome)) {
            isTesting = false
            onFirstRun()
        } else {
            cleanupFiles()

            authenticate()
        }

        invalidateOptionsMenu()
    }

    override fun onPause() {
        activeDash.unbindLocationService()

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putBoolean(ARG_EXPECTED_EXIT, expectedExit)

        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val showBPAs = sharedPrefs.getBoolean(PREF_SHOW_BASE_PAY_ADJUSTS, true)

        menu?.let {
            it.findItem(R.id.action_new_weekly)?.isVisible = showBPAs
            if (it is MenuBuilder) {
                try {
                    val f = it::class.java.getDeclaredMethod(
                        "setOptionalIconsVisible",
                        Boolean::class.java
                    )
                    f.isAccessible = true
                    f.invoke(it, true)
                } catch (e: Exception) {
                    errorLog("Kotlin Exception")
                } catch (se: java.lang.Exception) {
                    errorLog("Java Exception")
                }
            }
        }

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
                    ExpenseDialog.newInstance(id)
                        .show(supportFragmentManager, "new_expense_dialog")
                }
                true
            }
            R.id.action_settings -> {
                expectedExit = true
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    putExtra(INTENT_EXTRA_PRE_AUTH, true)
                }
                settingsActivityLauncher.launch(intent)
                true
            }
            R.id.action_contact -> {
                expectedExit = true
                val intent =
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:dashtracker@benica.dev"))
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Stops current dash by calling [ActiveDash.stopDash] on [entryId]
     */
    private fun endDash(entryId: Long? = null) {
        activeDash.stopDash(entryId)
    }

    /**
     * Clear active entry - if the active entry id is [id], sets [activeEntryDeleted] to true, so
     * that the [EndDashDialog] isn't triggered, and calls [MainActivityViewModel.loadActiveEntry]
     * with null as the argument.
     *
     * @param id
     */
    internal fun clearActiveEntry(id: Long) {
        if (activeDash.activeEntry?.entry?.entryId == id) {
            activeEntryDeleted = true
            viewModel.loadActiveEntry(null)
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

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent?.component)
            .putExtra(EXTRA_SETTINGS_RESTART_APP, true)
        startActivity(mainIntent)
        exitProcess(0)
    }

    // ActiveDashBarCallback
    override fun stopDash() {
        viewModel.loadActiveEntry(id = null)
    }

    override fun revealAppBarLayout(
        shouldShow: Boolean,
        doAnyways: Boolean,
        lockAppBar: Boolean,
        onComplete: (() -> Unit)?
    ) {
        binding.appBarLayout.revealIfTrue(
            shouldShow = shouldShow,
            doAnyways = doAnyways
        ) {
            (binding.summaryBar.root.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                if (lockAppBar) {
                    SCROLL_FLAG_NO_SCROLL
                } else {
                    SCROLL_FLAG_ENTER_ALWAYS or SCROLL_FLAG_SCROLL or SCROLL_FLAG_SNAP
                }
            onComplete?.invoke()
        }
    }

    // IncomeFragmentCallback
    override fun setDeductionType(dType: DeductionType) {
        deductionTypeViewModel.setDeductionType(dType)
    }

    // ListItemFragmentCallback overrides
    override fun hideToolbarsAndFab(hideToolbar: Boolean, hideFab: Boolean) {
        isShowingOrHidingToolbars = true
        with(binding) {
            appBarLayout.setExpanded(false, true)
            if (hideToolbar) {
                bottomAppBar.performHide(true)
            }
            if (hideFab) {
                fab.hide()
            }
        }
    }

    override fun showToolbarsAndFab(showToolbar: Boolean, showFab: Boolean) {
        isShowingOrHidingToolbars = true
        with(binding) {
            appBarLayout.setExpanded(true, true)
            if (showToolbar) {
                bottomAppBar.performShow(true)
                updateToolbarAndBottomPadding()
            }
            if (showFab) {
                fab.show()
            }
        }
    }

    private fun updateToolbarAndBottomPadding(slideAppBarDown: Boolean = true) {
        if (slideAppBarDown) {
            val lps =
                binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            val appBarBehavior = lps.behavior as AppBarLayout.Behavior?

            appBarBehavior?.topAndBottomOffset = 0
        }

        binding.apply {
            val newPadding = if (currDestination == R.id.navigation_insights) {
                bottomAppBar.height + resources.getDimension(R.dimen.margin_half).toInt()
            } else {
                0
            }

            navHostFragmentActivityMain.updatePadding(
                bottom = newPadding
            )
        }
    }

    // AuthenticatedActivity overrides
    override val onAuthentication: () -> Unit
        get() {
            fun onEndDashIntent(tripId: Long): () -> Unit = fun() {
                activeDash.bindLocationService()
                endDash(tripId)
                onUnlock()
            }

            val endDashExtra = intent?.getBooleanExtra(EXTRA_END_DASH, false)
            intent?.removeExtra(EXTRA_END_DASH)
            val tripId = intent?.getLongExtra(EXTRA_TRIP_ID, -1L)
            intent?.removeExtra(EXTRA_TRIP_ID)

            return if (endDashExtra == true && tripId != null) {
                onEndDashIntent(tripId)
            } else {
                ::onUnlock
            }
        }

    override val onAuthFailed: (() -> Unit)? = null

    override val onAuthError: () -> Unit = ::reAuthenticate

    // TODO: This might should be moved to AuthenticatedActivity
    /**
     * If there's an auth error, this is used to call authenticate again with the same parameters
     */
    private fun reAuthenticate() {
        authenticate(
            onAuthentication,
            onAuthFailed,
            onAuthError,
        )
    }

    override fun lockScreen() {
        binding.container.visibility = INVISIBLE
        supportActionBar?.hide()
    }

    private fun onUnlock() {
        isAuthenticated = true
        expectedExit = false
        binding.container.visibility = VISIBLE
        supportActionBar?.show()

        val activeEntryId = sharedPrefs.getLong(ACTIVE_ENTRY_ID, -1L)

        viewModel.loadActiveEntry(activeEntryId)
    }

    override val currentCpm: Float?
        get() = activeDash.activeCpm

    /**
     * Keeps track of the current dash and current pause state.
     *
     */
    inner class ActiveDash {
        // Active Entry
        internal var activeEntry: FullEntry? = null
            set(value) {
                onNewActiveEntry(field, value)
                field = value
                binding.adb.updateEntry(field, activeCpm)
            }

        internal var locationServiceState: ServiceState? = null

        /**
         * If [activeEntryId] is not null, calls [startLocationService] to start/restart the
         * location service. If [activeEntryId] is null, the location service is started with a new
         * [DashEntry]. If the location service is already started with a different [DashEntry],
         * that [DashEntry] is set as [activeEntry], otherwise the new [DashEntry] is.
         */
        fun resumeOrStartNewTrip() {
            bindLocationService()

            val id = activeEntryId
            if (id == null) {
                // TODO: Is this ever possible? sure active entry id is nullable, but is there ever
                //  a situation where resumeOrStartNewTrip would be called when it is null?
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
         * Opens [EndDashDialog] and calls [stopTracking]
         *
         * @param entryId the id of the dash to stop
         */
        fun stopDash(entryId: Long?) {
            val id = entryId ?: activeEntryId ?: AUTO_ID
            sharedPrefs.edit().putLong(ACTIVE_ENTRY_ID, -1L).apply()

            stopTracking()
            if (!activeEntryDeleted) {
                EndDashDialog.newInstance(id)
                    .show(supportFragmentManager, "end_dash_dialog")
            } else {
                activeEntryDeleted = false
            }
        }

        /**
         * Updates [locationServiceState] from [LocationService.serviceState]
         */
        fun initLocSvcObserver() {
            CoroutineScope(Dispatchers.Default).launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    locationService?.serviceState?.collectLatest { state ->
                        locationServiceState = state
                    }
                }
            }
        }

        /**
         * Calls [unbindService] on [locationServiceConnection], sets [locationServiceBound] to
         * false, [locationServiceConnection] to null, and [locationService] to null.
         */
        internal fun unbindLocationService() {
            if (locationServiceBound && locationServiceConnection != null) {
                unbindService(locationServiceConnection!!)
                locationServiceBound = false
                locationServiceConnection = null
                locationService = null
            }
        }

        private val activeEntryId: Long?
            get() = activeEntry?.entry?.entryId
        internal var activeCpm: Float? = 0f
        internal var serviceState: ADBState = ADBState.INACTIVE
            set(value) {
                field = value
                revealAppBarLayout(
                    shouldShow = currDestination == R.id.navigation_income || field != ADBState.INACTIVE,
                    lockAppBar = field == ADBState.TRACKING_COLLAPSED || field == ADBState.TRACKING_DISABLED
                ) {
                    updateToolbarAndBottomPadding(slideAppBarDown = false)
                    updateUi()
                }
            }

        private fun onNewActiveEntry(before: FullEntry?, after: FullEntry?) {
            val beforeId = before?.entry?.entryId
            val afterId = after?.entry?.entryId

            serviceState =
                updateActiveDashState(
                    afterId = afterId,
                    beforeId = beforeId,
                    showMini = currDestination != R.id.navigation_income
                )
        }


        /**
         * Animate fab icon and adb visibility depending on [serviceState].
         */
        internal fun updateUi() {
            fun updateTopAppBarVisibility() {
                when (serviceState) {
                    ADBState.INACTIVE -> {
                        binding.summaryBar.root.revealIfTrue(
                            shouldShow = true,
                            doAnyways = true
                        ) {
                            binding.appBarLayout.revealIfTrue(
                                shouldShow = currDestination == R.id.navigation_income,
                                doAnyways = true
                            ) {
                                updateToolbarAndBottomPadding(slideAppBarDown = false)
                            }
                        }
                    }
                    ADBState.TRACKING_DISABLED,
                    ADBState.TRACKING_COLLAPSED,
                    ADBState.TRACKING_FULL -> {
                        binding.summaryBar.root.revealIfTrue(
                            shouldShow = currDestination == R.id.navigation_income,
                            doAnyways = true
                        ) {
                            binding.appBarLayout.revealIfTrue(
                                shouldShow = true,
                                doAnyways = true
                            ) {
                                if (currDestination == R.id.navigation_income) {
                                    binding.adb.transitionBackgroundTo(R.attr.colorAppBarBg)
                                } else {
                                    binding.adb.transitionBackgroundTo(R.attr.colorActiveDashBarBg)
                                }

                                updateToolbarAndBottomPadding(slideAppBarDown = false)
                            }
                        }
                    }
                }
            }

            binding.adb.onServiceStateUpdated(serviceState)
            updateTopAppBarVisibility()

            binding.fab.updateIcon(
                currFragId = currDestination,
                isTracking = serviceState != ADBState.INACTIVE
            )
        }

        /**
         * Stops or starts tracking based on incoming activeEntry
         *
         * @param afterId id of the new activeEntry
         * @param beforeId id of the previous activeEntry
         * @return the current [ADBState]
         */
        internal fun updateActiveDashState(
            afterId: Long?,
            beforeId: Long?,
            showMini: Boolean
        ): ADBState {
            val mTrackingEnabled = trackingEnabled

            val res = if (afterId == null) { // stopping or stopped
                if (beforeId != null) {
                    stopDash(beforeId)
                }

                ADBState.INACTIVE
            } else { // starting or continuing
                if (beforeId == null && !locationServiceBound) {
                    startTracking()
                }

                if (mTrackingEnabled) {
                    if (!trackingEnabledPrevious) {
                        // tracking has been enabled
                        resumeOrStartNewTrip()
                    }

                    if (showMini) {
                        ADBState.TRACKING_COLLAPSED
                    } else {
                        ADBState.TRACKING_FULL
                    }
                } else {
                    if (locationServiceBound || trackingEnabledPrevious) {
                        // either location service hasn't been stopped or tracking was disabled
                        stopTracking()
                    }

                    ADBState.TRACKING_DISABLED
                }
            }

            trackingEnabledPrevious = mTrackingEnabled

            return res
        }

        // Location Service
        private var locationService: LocationService? = null
            set(value) {
                field = value
                field?.let {
                    updateLocationServiceNotificationData(activeEntryId, it)
                }
            }

        private var locationServiceConnection: ServiceConnection? = null

        internal var locationServiceBound: Boolean = false

        /**
         * lock to prevent location service from being started multiple times
         */
        private var startingService = false

        private var stopOnBind = false
        private var startOnBind = false
        private var startOnBindId: Long? = null

        /**
         * Launches [OnboardingMileageActivity] to check for permissions, and, if location is enabled, calls [resumeOrStartNewTrip]
         */
        private fun startTracking() {
            startOnBind = false
            stopOnBind = false
            expectedExit = true

            onboardMileageTrackingLauncher.launch(
                Intent(this@MainActivity, OnboardingMileageActivity::class.java)
            )
        }

        /**
         * If [locationService] is not null, calls [LocationService.stop]  If [locationService]
         * is null, meaning the service is not currently bound, the service will be stopped once
         * it is bound.
         */
        private fun stopTracking() {
            locationService.let {
                if (it != null) {
                    it.stop()
                    unbindLocationService()
                } else {
                    stopOnBind = true
                    bindLocationService()
                }
            }
        }

        internal fun bindLocationService() {
            fun onBind() {
                activeEntryId?.let { id ->
                    startLocationService(id)
                }
            }

            // stopOnBind is set true when a new active entry comes in, and trackingEnabled is
            // false.
            if (!locationServiceBound && !startingService && (trackingEnabled || stopOnBind)) {
                startingService = true
                val locationServiceIntent =
                    Intent(applicationContext, LocationService::class.java)

                if (locationServiceConnection == null) {
                    val conn = getLocationServiceConnection(::onBind)
                    locationServiceConnection = conn
                }

                bindService(
                    locationServiceIntent,
                    locationServiceConnection!!,
                    BIND_AUTO_CREATE
                )
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

            return locationService?.let {
                updateLocationServiceNotificationData(tripId, it)
                it.start(tripId, saveLocation)
            }
        }

        /**
         * Updates [locationService]'s ongoing notification data
         *
         * @param tripId the id of the active trip
         */
        private fun updateLocationServiceNotificationData(
            tripId: Long? = activeEntryId,
            locationService: LocationService
        ) {
            fun getOpenActivityAction(context: Context): NotificationCompat.Action {
                fun getLaunchActivityPendingIntent(): PendingIntent? {
                    val launchActivityIntent = Intent(context, MainActivity::class.java)
                        .putExtra(EXTRA_END_DASH, false)
                        .putExtra(EXTRA_TRIP_ID, tripId ?: -1L)

                    return PendingIntent.getActivity(
                        context,
                        0,
                        launchActivityIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

                return NotificationCompat.Action(
                    R.drawable.ic_launch,
                    getString(R.string.notification_button_open_app),
                    getLaunchActivityPendingIntent()
                )
            }

            fun getStopServiceAction(ctx: Context): NotificationCompat.Action {
                fun getEndDashPendingIntent(id: Long?): PendingIntent? {
                    val intent = Intent(ctx, MainActivity::class.java)
                        .putExtra(EXTRA_END_DASH, true)
                        .putExtra(EXTRA_TRIP_ID, id ?: -1L)

                    return PendingIntent.getActivity(
                        ctx,
                        1,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

                return NotificationCompat.Action(
                    R.drawable.ic_cancel,
                    getString(R.string.notification_button_stop),
                    getEndDashPendingIntent(tripId)
                )
            }

            val locServiceOngoingNotificationData =
                NotificationUtils.NotificationData(
                    contentTitle = R.string.app_name,
                    bigContentTitle = R.string.app_name,
                    icon = R.drawable.notification_icon,
                    actions = listOf(
                        ::getOpenActivityAction,
                        ::getStopServiceAction
                    )
                )

            locationService.initialize(
                notificationData = locServiceOngoingNotificationData,
                notificationChannel = notificationChannel,
                notificationText = { getString(R.string.notification_text_tracking_on) },
                updateNotificationText = { getString(R.string.notification_text_tracking_on) },
            )
        }

        /**
         * Returns a [ServiceConnection] that, in [ServiceConnection.onServiceConnected] saves the
         * service binding to [locationService], initiates observers on it, and loads the
         * [activeEntry] if the [LocationService] is already started. It will stop the service if
         * [stopOnBind] is set to true.
         *
         * @param onBind any actions to perform once the [LocationService] is bound
         * @return a [ServiceConnection]
         */
        private fun getLocationServiceConnection(onBind: (() -> Unit)? = null): ServiceConnection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val binder = service as LocationService.LocalBinder
                    locationService = binder.service
                    locationServiceBound = true

                    initLocSvcObserver()

                    if (stopOnBind) {
                        binder.service.stop()
                        stopOnBind = false
                        startOnBind = false
                        startOnBindId = null
                        unbindLocationService()
                    } else if (startOnBind && locationServiceState != TRACKING_ACTIVE) {
                        (activeEntryId ?: startOnBindId)?.let { startLocationService(it) }
                        startOnBind = false
                        startOnBindId = null
                    } else {
                        locationService?.tripId?.value?.let {
                            viewModel.loadActiveEntry(it)
                        }
                    }

                    onBind?.invoke()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    locationServiceBound = false
                    locationServiceConnection = null
                    locationService = null
                }
            }
    }

    companion object {
        private val prefix: String =
            "${BuildConfig.APPLICATION_ID}.${this::class.simpleName}"

        private const val LOC_SVC_CHANNEL_ID = "location_practice_0"
        private const val LOC_SVC_CHANNEL_NAME = "Mileage Tracking"
        private const val LOC_SVC_CHANNEL_DESC = "DashTracker mileage tracker is active"

        private const val EXTRA_SETTINGS_RESTART_APP =
            "${BuildConfig.APPLICATION_ID}.restart_settings"
        private val EXTRA_END_DASH = "$prefix.end_dash"
        private val EXTRA_TRIP_ID = "$prefix.trip_id"

        private val ACTIVE_ENTRY_ID = "$prefix.active_entry_id"

        private val notificationChannel =
            NotificationChannel(
                LOC_SVC_CHANNEL_ID,
                LOC_SVC_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = LOC_SVC_CHANNEL_DESC
            }

        @ColorInt
        internal fun getColorFab(context: Context) = context.getAttrColor(R.attr.colorFab)

        @ColorInt
        internal fun getColorFabDisabled(context: Context) =
            context.getAttrColor(R.attr.colorFabDisabled)

        private val convertPacksImport = listOf(
            DashEntry.getConvertPackImport(),
            Weekly.getConvertPackImport(),
            Expense.getConvertPackImport(),
            ExpensePurpose.getConvertPackImport(),
            LocationData.getConvertPackImport()
        )

        // This doesn't work when it's a fun but it does work when it's a val
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

    private var isTesting: Boolean = false
}

interface ScrollableFragment {
    val isAtTop: Boolean
}

/**
 * Marker interface for a fragment that adjusts based on [DeductionType]. Does not currently
 * serve a purpose
 */
interface DeductionCallback

