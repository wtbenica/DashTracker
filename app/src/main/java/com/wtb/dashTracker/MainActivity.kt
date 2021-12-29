package com.wtb.dashTracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
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
import com.wtb.dashTracker.ui.dialog_entry.EntryDialog
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.entry_list.EntryListFragment.EntryListFragmentCallback
import com.wtb.dashTracker.ui.weekly_list.WeeklyListFragment.WeeklyListFragmentCallback
import com.wtb.dashTracker.views.FabMenuButtonInfo
import com.wtb.dashTracker.views.getStringOrElse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.DayOfWeek
import java.time.LocalDate

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), WeeklyListFragmentCallback, EntryListFragmentCallback {

    private lateinit var binding: ActivityMainBinding
    lateinit var mAdView : AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.initialize(this)
        supportActionBar?.title = "DashTracker"

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this) {}

        mAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        initBottomNavBar()
        initObservers()
        binding.fab.initialize(getMenuItems(supportFragmentManager), binding.container)
    }

    private fun initObservers() {
        val viewModel: MainActivityViewModel by viewModels()

        viewModel.hourly.observe(this) {
            binding.actMainHourly.text = getStringOrElse(R.string.currency_unit, it)
        }

        viewModel.thisWeek.observe(this) {
            binding.actMainThisWeek.text = getStringOrElse(R.string.currency_unit, it)
        }

        viewModel.lastWeek.observe(this) {
            binding.actMainLastWeek.text = getStringOrElse(R.string.currency_unit, it)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_licenses -> {
            startActivity(Intent(this, OssLicensesMenuActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        const val APP = "GT_"
        private const val TAG = APP + "MainActivity"
        private val weekEndsOn = DayOfWeek.SUNDAY

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

