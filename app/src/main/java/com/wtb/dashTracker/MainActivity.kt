package com.wtb.dashTracker

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
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

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), DailyFragment.DailyFragmentCallback {

    private lateinit var binding: ActivityMainBinding
    private var fabMenuIsVisible = false
    private var fabMenuItems = mutableListOf<FabMenuButton>()
    private val fabOpenAnimation: Animation
        get() = AnimationUtils.loadAnimation(this, R.anim.open_fab_menu)
    private val fabCloseAnimation: Animation
        get() = AnimationUtils.loadAnimation(this, R.anim.close_fab_menu)

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
                    showFabMenu()
            }
        }

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

        initFabMenu()
    }

    private fun hideFabMenu() {
        fadeOutFab()
        saturateFab()
        runFabIconCollapseAnimation()

        fabMenuIsVisible = false
    }

    private fun showFabMenu() {
        fadeInFabMenu()
        desaturateFab()
        runFabIconExpandAnimation()

        fabMenuIsVisible = true
    }

    private fun runFabIconCollapseAnimation() {
        binding.fab.setImageResource(R.drawable.anim_fab_collapse)
        when (val d = binding.fab.drawable) {
            is AnimatedVectorDrawableCompat -> d.start()
            is AnimatedVectorDrawable -> d.start()
        }
    }

    private fun runFabIconExpandAnimation() {
        binding.fab.setImageResource(R.drawable.anim_fab_expand)
        when (val d = binding.fab.drawable) {
            is AnimatedVectorDrawableCompat -> d.start()
            is AnimatedVectorDrawable -> d.start()
        }
    }

    private fun saturateFab() {
        @ColorInt val accent = getColorAccent(this)
        @ColorInt val gray = getColor(R.color.shadow)

        animateFabColor(gray, accent)
    }

    private fun animateFabColor(fromColor: Int, toColor: Int) {
        ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
            duration = ANIM_LENGTH
            addUpdateListener {
                val i = animatedValue as Int
                binding.fab.backgroundTintList = ColorStateList.valueOf(i)
            }
            start()
        }
    }

    private fun desaturateFab() {
        @ColorInt val accent = getColorAccent(this)
        @ColorInt val gray = getColor(R.color.shadow)

        animateFabColor(accent, gray)
    }

    private fun fadeInFabMenu() {
        fabMenuItems.forEachIndexed { index, item ->
            item.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(ANIM_DELAY * index)
                .setDuration(ANIM_LENGTH)
                .withStartAction { item.visibility = VISIBLE }
        }
    }

    private fun fadeOutFab() {
        val dimension = resources.getDimension(R.dimen.fab_menu_offset)
        fabMenuItems.forEachIndexed { index, item ->
            item.animate()
                .alpha(0f)
                .translationY(dimension)
                .setStartDelay(ANIM_DELAY * (fabMenuItems.size - index - 1))
                .setDuration(ANIM_LENGTH)
                .withEndAction { item.visibility = GONE }
        }
    }

    private fun initFabMenu() {
        @IdRes var itemAnchor: Int = binding.fab.id
        for (item in getMenuItems(supportFragmentManager)) {
            val newMenuItem: FabMenuButton =
                FabMenuButton.newInstance(this, item, binding.root).apply {
//                    elevation = resources.getDimension(R.dimen.fab_menu_spacing)
                    id = View.generateViewId()
                    translationY = resources.getDimension(R.dimen.fab_menu_offset)
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
        for (item in fabMenuItems) {
            item.visibility = GONE
        }
    }

    companion object {
        const val APP = "GT_"
        const val TAG = APP + "MainActivity"
        const val ANIM_LENGTH = 100L
        const val ANIM_DELAY = 50L

        private fun getMenuItems(fm: FragmentManager): List<FabMenuButtonInfo> = listOf(
            FabMenuButtonInfo(
                "Add Entry",
                R.drawable.calendar
            ) { DetailFragment().show(fm, "new_entry_dialog") },
            FabMenuButtonInfo(
                "Add Adjustment",
                R.drawable.alert,
                { }
            ),
            FabMenuButtonInfo(
                "Add Payout",
                R.drawable.chart,
                { }
            )
        )

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