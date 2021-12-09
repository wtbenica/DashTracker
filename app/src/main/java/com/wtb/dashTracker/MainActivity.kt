package com.wtb.dashTracker

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.wtb.dashTracker.databinding.ActivityMainBinding
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.dialog_base_pay_adjustment.BasePayAdjustDialog
import com.wtb.dashTracker.ui.dialog_edit_details.DetailDialog
import com.wtb.dashTracker.ui.entry_list.EntryListFragment
import com.wtb.dashTracker.views.FabMenuButton
import com.wtb.dashTracker.views.FabMenuButtonInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.DayOfWeek
import java.time.LocalDate

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), EntryListFragment.EntryListFragmentCallback {

    private lateinit var binding: ActivityMainBinding
    private var fabMenuIsVisible = false
    private var fabMenuItems = mutableListOf<FabMenuButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.initialize(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBottomNavBar()
        initObservers()
        binding.fab.initialize(getMenuItems(supportFragmentManager), binding.root)
    }

    private fun initObservers() {
        val viewModel: MainActivityViewModel by viewModels()

        viewModel.hourly.observe(this) {
            binding.actMainHourly.text = getString(R.string.currency_unit, it)
        }

        viewModel.thisWeek.observe(this) {
            binding.actMainThisWeek.text = getString(R.string.currency_unit, it)
        }

        viewModel.lastWeek.observe(this) {
            binding.actMainLastWeek.text = getString(R.string.currency_unit, it)
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
                R.id.navigation_best_days,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        binding.fab.interceptTouchEvent(ev)
        return super.dispatchTouchEvent(ev)

    }

    companion object {
        const val APP = "GT_"
        private const val TAG = APP + "MainActivity"
        private val weekEndsOn = DayOfWeek.SUNDAY

        private fun getMenuItems(fm: FragmentManager): List<FabMenuButtonInfo> = listOf(
            FabMenuButtonInfo(
                "Add Entry",
                R.drawable.calendar
            ) { DetailDialog().show(fm, "new_entry_dialog") },
            FabMenuButtonInfo(
                "Add Adjustment",
                R.drawable.alert
            ) { BasePayAdjustDialog().show(fm, "new_adjust_dialog") },
            FabMenuButtonInfo(
                "Add Payout",
                R.drawable.chart
            ) { }
        )

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

        fun getNextEndOfWeek(): LocalDate {
            val todayIs: DayOfWeek = LocalDate.now().dayOfWeek
            val daysLeft: Long = (weekEndsOn.value - todayIs.value + 7) % 7L
            return LocalDate.now().plusDays(daysLeft)
        }
    }
}

