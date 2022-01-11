package com.wtb.dashTracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
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
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.wtb.dashTracker.databinding.ActivityMainBinding
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog
import com.wtb.dashTracker.ui.dialog_entry.EntryDialog
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.entry_list.EntryListFragment.EntryListFragmentCallback
import com.wtb.dashTracker.ui.weekly_list.WeeklyListFragment.WeeklyListFragmentCallback
import com.wtb.dashTracker.util.CSVUtils.Companion.FILE_ENTRIES
import com.wtb.dashTracker.util.CSVUtils.Companion.FILE_WEEKLIES
import com.wtb.dashTracker.util.CSVUtils.Companion.FILE_ZIP
import com.wtb.dashTracker.views.FabMenuButtonInfo
import com.wtb.dashTracker.views.getStringOrElse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), WeeklyListFragmentCallback, EntryListFragmentCallback {

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun initMobileAds() {
        MobileAds.initialize(this@MainActivity)

        mAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        if (!isAuthenticated) {
            val executor = ContextCompat.getMainExecutor(this)

            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticated = true
                        this@MainActivity.binding.root.visibility = VISIBLE
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Login for DashTracker")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    override fun onPause() {
        super.onPause()
        isAuthenticated = false
        binding.root.visibility = INVISIBLE
    }

    private fun initBiometrics() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                Log.d(TAG, "App can authenticate using biometrics.")
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Log.e(TAG, "No biometric features available on this device.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Log.e(TAG, "Biometric features are currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                        )
                    }
                    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }.launch(
                        enrollIntent
                    )
                }
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.e(TAG, "Biometric features are currently unavailable. (update required)")
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.e(TAG, "Biometric features are currently unavailable. (unsupported)")
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.e(TAG, "Biometric features are currently unavailable. (unknown)")
            }
        }
    }

    private fun initObservers() {
        viewModel.hourly.observe(this) {
            binding.actMainHourly.text =
                getStringOrElse(R.string.currency_unit, it)
        }

        viewModel.thisWeek.observe(this) {
            binding.actMainThisWeek.text =
                getStringOrElse(R.string.currency_unit, it)
        }

        viewModel.lastWeek.observe(this) {
            binding.actMainLastWeek.text =
                getStringOrElse(R.string.currency_unit, it)
        }
    }

    private fun initBottomNavBar() {
        val navView: BottomNavigationView = binding.navView
        navView.background = null

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_weekly,
                R.id.navigation_yearly,
                R.id.navigation_insights,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        binding.fab.interceptTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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
                exportDatabaseToCSV()
                true
            }
            R.id.action_import_from_csv -> {
                showImportCsvConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun exportDatabaseToCSV(encrypted: Boolean = false) = viewModel.export(encrypted)

    private fun showImportCsvConfirmationDialog() {
        ConfirmationDialog(
            text = R.string.confirm_import,
            requestKey = "confirmImportEntry",
            posButton = R.string.ok,
            negButton = R.string.cancel,
            message = "Import from CSV",
            posAction = {
                getContentZip.launch("application/zip")
            },
            negAction = { }
        ).show(supportFragmentManager, null)
    }

    private val getContentZip: ActivityResultLauncher<String> =
        getContent(FILE_ZIP) { extractZip(it) }

    private fun extractZip(it: Uri) {
        ZipInputStream(contentResolver.openInputStream(it)).use { zipIn ->
            var nextEntry: ZipEntry? = zipIn.nextEntry
            while (nextEntry != null) {
                val destFile = File(filesDir, nextEntry.name)
                FileOutputStream(destFile).use { t ->
                    zipIn.copyTo(t, 1024)
                }
                val inputStream = FileInputStream(destFile)

                nextEntry.name?.let {
                    when {
                        it.startsWith(FILE_ENTRIES, false) -> {
                            importCsv(entriesPath = inputStream)
                        }
                        it.startsWith(FILE_WEEKLIES, false) -> {
                            importCsv(weekliesPath = inputStream)
                        }
                    }
                }

                nextEntry = zipIn.nextEntry
            }
            zipIn.closeEntry()
        }
    }

    private fun importCsv(entriesPath: InputStream? = null, weekliesPath: InputStream? = null) =
        viewModel.import(
            entriesPath?.let { entriesPath },
            weekliesPath?.let { weekliesPath }
        )

    private fun getContent(prefix: String, action: (Uri) -> Unit): ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                contentResolver.query(it, null, null, null, null)
                    ?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        val fileName = cursor.getString(nameIndex)
                        if (fileName.startsWith(prefix)) {
                            action(it)
                        }
                    }
            }
        }

    companion object {
        const val APP = "GT_"
        private const val TAG = APP + "MainActivity"
        private val weekEndsOn = DayOfWeek.SUNDAY
        var isAuthenticated = false

        private fun getMenuItems(fm: FragmentManager): List<FabMenuButtonInfo> = listOf(
            FabMenuButtonInfo(
                "Add Entry",
                R.drawable.calendar
            ) { EntryDialog().show(fm, "new_entry_dialog") },
            FabMenuButtonInfo(
                "Add Adjustment",
                R.drawable.alert
            ) { WeeklyDialog().show(fm, "new_adjust_dialog") },
//            FabMenuButtonInfo(
//                "Add Payout",
//                R.drawable.chart
//            ) { PayoutDialog().show(fm, "new_payout_dialog") }
        )

        @ColorInt
        fun getColorFab(context: Context) = getAttrColor(context, R.attr.colorFab)

        @ColorInt
        fun getColorFabDisabled(context: Context) = getAttrColor(context, R.attr.colorFabDisabled)

        @ColorInt
        fun getColorAccent(context: Context) = getAttrColor(context, R.attr.colorAccent)

        @ColorInt
        fun getColorPrimary(context: Context) = getAttrColor(context, R.attr.colorPrimary)

        @ColorInt
        fun getAttrColor(context: Context, @AttrRes id: Int): Int {
            val tv = TypedValue()
            val arr = context.obtainStyledAttributes(tv.data, intArrayOf(id))
            @ColorInt val color = arr.getColor(0, 0)
            arr.recycle()
            return color
        }

        fun getThisWeeksDateRange(): Pair<LocalDate, LocalDate> {
            val endDate: LocalDate = getNextEndOfWeek()
            val startDate = endDate.minusDays(6L)
            return Pair(startDate, endDate)
        }

        private fun getNextEndOfWeek(): LocalDate {
            val todayIs: DayOfWeek = LocalDate.now().dayOfWeek
            val daysLeft: Long = (weekEndsOn.value - todayIs.value + 7) % 7L
            return LocalDate.now().plusDays(daysLeft)
        }
    }
}

