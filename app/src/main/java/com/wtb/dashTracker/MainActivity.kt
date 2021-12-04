package com.wtb.dashTracker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wtb.dashTracker.databinding.ActivityMainBinding
import com.wtb.dashTracker.ui.daily.DailyFragment
import com.wtb.dashTracker.ui.edit_details.DetailFragment
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.DayOfWeek
import java.time.LocalDate

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), DailyFragment.DailyFragmentCallback {

    private lateinit var binding: ActivityMainBinding

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
                DetailFragment().show(supportFragmentManager, "new_entry_dialog")
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

    companion object {
        const val APP = "GT_"

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