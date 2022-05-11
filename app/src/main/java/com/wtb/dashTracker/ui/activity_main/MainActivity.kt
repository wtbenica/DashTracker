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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.*
import com.wtb.dashTracker.databinding.ActivityMainBinding
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog
import com.wtb.dashTracker.ui.dialog_confirm.LambdaWrapper
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.EntryDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_expense.ExpenseDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.fragment_expenses.ExpenseListFragment.ExpenseListFragmentCallback
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import com.wtb.dashTracker.util.CSVUtils
import com.wtb.dashTracker.util.CSVUtils.Companion.FILE_ZIP
import com.wtb.dashTracker.views.FabMenuButtonInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), ExpenseListFragmentCallback,
    IncomeFragment.IncomeFragmentCallback {

    private val viewModel: MainActivityViewModel by viewModels()
    private val deductionTypeViewModel: DeductionTypeViewModel by viewModels()

    override val deductionType: StateFlow<DeductionType>
        get() = deductionTypeViewModel.deductionType

    override fun setDeductionType(dType: DeductionType) {
        deductionTypeViewModel.setDeductionType(dType)
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mAdView: AdView

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
                it.addOnDestinationChangedListener { controller, destination, arguments ->
                    when (destination.id) {
                        R.id.navigation_income -> binding.summaryBar.apply {
                            visibility = VISIBLE
                            layoutParams.height = WRAP_CONTENT
                        }
                        R.id.navigation_expenses -> binding.summaryBar.apply {
                            visibility = VISIBLE
                            layoutParams.height = WRAP_CONTENT
                        }
                        R.id.navigation_insights -> binding.summaryBar.collapse()
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
        }

        installSplashScreen()
        Repository.initialize(this)
        supportActionBar?.title = "DashTracker"

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.fab.initialize(getMenuItems(supportFragmentManager), binding.container)
        setContentView(binding.root)

        initBiometrics()
        initMobileAds()
        initBottomNavBar()
        initObservers()
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
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock to access DashTracker")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        }

        super.onResume()
        cleanupFiles()
        if (!isAuthenticated) {
            authenticate()
        }
    }

    override fun onPause() {
        super.onPause()
        isAuthenticated = false
        binding.container.visibility = INVISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun exportDatabaseToCSV() = viewModel.export(this)

        fun importCSVtoDatabase() = viewModel.import(this)

        return when (item.itemId) {
            R.id.action_licenses -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            R.id.action_export_to_csv -> {
                exportDatabaseToCSV()
                true
            }
            R.id.action_import_from_csv -> {
                importCSVtoDatabase()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        binding.fab.interceptTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    internal val contentZipLauncher: ActivityResultLauncher<String> =
        getContentLauncher(
            FILE_ZIP,
            ::extractZip,
            ConfirmationDialog.newInstance(
                text = R.string.unzip_error,
                requestKey = "confirmUnzipError",
                message = "Error",
                posButton = R.string.ok,
                posAction = LambdaWrapper {},
                negButton = null
            )
        )

    private fun extractZip(uri: Uri) {
        ZipInputStream(contentResolver.openInputStream(uri)).use { zipIn ->
            var entryModels: List<DashEntry> = emptyList()
            var weeklyModels: List<Weekly> = emptyList()
            var expenseModels: List<Expense> = emptyList()
            var purposeModels: List<ExpensePurpose> = emptyList()

            var nextEntry: ZipEntry? = zipIn.nextEntry
            while (nextEntry != null) {
                val destFile = File(filesDir, nextEntry.name)
                if (!destFile.canonicalPath.startsWith(filesDir.canonicalPath)) {
                    throw SecurityException()
                }
                FileOutputStream(destFile).use { t ->
                    zipIn.copyTo(t, 1024)
                }
                val inputStream = FileInputStream(destFile)

                nextEntry.name?.also { entryName ->
                    when {
                        entryName.startsWith(CSVUtils.FILE_ENTRIES, false) -> {
                            getModelsFromCsv(inputStream) {
                                DashEntry.fromCSV(it)
                            }?.let { entryModels = it }
                        }
                        entryName.startsWith(CSVUtils.FILE_WEEKLIES, false) -> {
                            getModelsFromCsv(inputStream) {
                                Weekly.fromCSV(it)
                            }?.let { weeklyModels = it }
                        }
                        entryName.startsWith(CSVUtils.FILE_EXPENSES, false) -> {
                            getModelsFromCsv(inputStream) {
                                Expense.fromCSV(it)
                            }?.let { expenseModels = it }
                        }
                        entryName.startsWith(CSVUtils.FILE_PURPOSES, false) -> {
                            getModelsFromCsv(inputStream) {
                                ExpensePurpose.fromCSV(it)
                            }?.let { purposeModels = it }
                        }
                    }
                }

                nextEntry = zipIn.nextEntry
            }

            zipIn.closeEntry()

            viewModel.importStream(
                entries = entryModels.ifEmpty { null },
                weeklies = weeklyModels.ifEmpty { null },
                expenses = expenseModels.ifEmpty { null },
                purposes = purposeModels.ifEmpty { null }
            )
        }
    }

    private fun <T : DataModel> getModelsFromCsv(
        path: InputStream?,
        function: (Map<String, String>) -> T
    ): List<T>? =
        path?.let { inStream ->
            csvReader().readAllWithHeader(inStream).map { function(it) }
        }

    @Suppress("SameParameterValue")
    private fun getContentLauncher(
        prefix: String,
        action: (Uri) -> Unit,
        errorDialog: ConfirmationDialog
    ): ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                contentResolver.query(it, null, null, null, null)
                    ?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        val fileName = cursor.getString(nameIndex)
                        if (fileName.startsWith(prefix)) {
                            try {
                                action(it)
                            } catch (e: SecurityException) {
                                errorDialog.show(supportFragmentManager, null)
                            }
                        }
                    }
            }
        }

    private fun getMenuItems(fm: FragmentManager): List<FabMenuButtonInfo> = listOf(
        FabMenuButtonInfo(
            "Add Entry",
            R.drawable.ic_new_entry
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                val id = viewModel.upsertAsync(DashEntry())
                EntryDialog.newInstance(id.toInt()).show(fm, "new_entry_dialog")
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
                ExpenseDialog.newInstance(id.toInt()).show(fm, "new_expense_dialog")
            }
        },
        //        FabMenuButtonInfo(
        //            "Add Payout",
        //            R.drawable.chart
        //        ) { PayoutDialog().show(fm, "new_payout_dialog") }
    )

    companion object {
        const val APP = "GT_"
        var isAuthenticated = false

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

    }
}

interface DeductionCallback


