package com.wtb.dashTracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.bottomnav.databinding.ActivityMainBinding
import com.example.bottomnav.ui.daily.DailyFragment
import com.example.bottomnav.ui.edit_details.DetailFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wtb.gigtracker.repository.Repository
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

        val navController = findNavController(com.example.bottomnav.R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                com.example.bottomnav.R.id.navigation_home,
                com.example.bottomnav.R.id.navigation_dashboard,
                com.example.bottomnav.R.id.navigation_notifications
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