package com.wtb.dashTracker

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wtb.dashTracker.databinding.ActivityMainBinding
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.daily.DailyFragment
import com.wtb.dashTracker.ui.edit_details.DetailFragment
import com.wtb.dashTracker.views.FabMenuButton
import com.wtb.dashTracker.views.FabMenuButtonInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.DayOfWeek
import java.time.LocalDate

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), DailyFragment.DailyFragmentCallback {

    private lateinit var binding: ActivityMainBinding
    private var fabMenuIsVisible = false
    private var fabMenuItems = mutableListOf<FabMenuButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.initialize(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        navView.background = null

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val fab: FloatingActionButton = binding.fab
        fab.apply {
            setOnClickListener {
//                DetailFragment().show(supportFragmentManager, "new_entry_dialog")
                if (fabMenuIsVisible)
                    hideFabMenu()
                else
                    makeFabMenu()
                fabMenuIsVisible = !fabMenuIsVisible
            }
        }

        val viewModel: MainActivityViewModel by viewModels()

        viewModel.hourly.observe(this) {
            binding.actMainHourly.text = getString(R.string.currency_unit, it)
        }

        viewModel.thisWeek.observe(this) {
            binding.actMainThisWeek.text = getString(R.string.currency_unit, it)
        }
    }

    private fun hideFabMenu() {
        for (item in fabMenuItems) {
            item.visibility = GONE
        }
    }

    private fun makeFabMenu() {

        if (fabMenuItems.isEmpty()) {
            val menuItems: List<FabMenuButtonInfo> = listOf(
                FabMenuButtonInfo(
                    "Add Entry",
                    R.drawable.calendar,
                    { DetailFragment().show(supportFragmentManager, "new_entry_dialog") }
                )
            )

            @IdRes var itemAnchor: Int = binding.fab.id
            for (item in menuItems) {
                val newMenuItem: FabMenuButton =
                    FabMenuButton.newInstance(this, item, binding.root).apply {
                        elevation = R.dimen.fab_menu_spacing.toFloat()
                        id = View.generateViewId()
                    }
                fabMenuItems.add(newMenuItem)
                binding.root.addView(
                    newMenuItem,
                    CoordinatorLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        anchorId = itemAnchor
                        anchorGravity = Gravity.TOP or Gravity.END
                        gravity = Gravity.TOP
                    })

                itemAnchor = newMenuItem.id
            }
        } else {
            for (fabMenuItem in fabMenuItems) {
                fabMenuItem.visibility = VISIBLE
            }
        }
    }

    companion object {
        const val APP = "GT_"
        const val TAG = APP + "MainActivity"

        fun getThisWeeksDateRange(): Pair<LocalDate, LocalDate> {
            val todayIs = LocalDate.now().dayOfWeek
            val weekEndsOn = DayOfWeek.SUNDAY
            val daysLeft = (weekEndsOn.value - todayIs.value + 7) % 7L
            val endDate = LocalDate.now().plusDays(daysLeft)
            val startDate = endDate.minusDays(6L)
            return Pair(startDate, endDate)
        }
    }
}