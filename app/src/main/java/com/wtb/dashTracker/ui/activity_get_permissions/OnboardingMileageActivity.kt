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

package com.wtb.dashTracker.ui.activity_get_permissions

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.twotone.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingScreen.*
import com.wtb.dashTracker.ui.activity_get_permissions.ui.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.util.*
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BG_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_NOTIFICATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.BG_BATTERY_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.LOCATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.NOTIFICATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_NOTIFICATION
import kotlinx.coroutines.*

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
class OnboardingMileageActivity : ComponentActivity() {

    private val permissionsHelper = PermissionsHelper(this)

    private val sharedPrefs
        get() = permissionsHelper.sharedPrefs

    private val singlePermissionLauncher: ActivityResultLauncher<String> =
        registerSinglePermissionLauncher()

    private val multiplePermissionsLauncher: ActivityResultLauncher<Array<String>> =
        registerMultiplePermissionsLauncher()

    private var navController: NavHostController? = null
    private var loadSingleScreen: OnboardingScreen? = null
    private var initialScreen: OnboardingScreen? = null
    private var showSummaryScreen = false
    private var showIntroScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        @Suppress("DEPRECATION")
        loadSingleScreen = if (SDK_INT >= TIRAMISU) {
            intent.getSerializableExtra(
                EXTRA_PERMISSIONS_ROUTE,
                OnboardingScreen::class.java
            )
        } else {
            intent.getSerializableExtra(EXTRA_PERMISSIONS_ROUTE) as OnboardingScreen?
        }

        showIntroScreen = sharedPrefs.getBoolean(ASK_AGAIN_LOCATION, true)

        val missingLocation = !hasPermissions(*LOCATION_PERMISSIONS)
        val missingBgLocation = !hasPermissions(ACCESS_BACKGROUND_LOCATION)
        val missingNotification = ((SDK_INT >= TIRAMISU)
                && !hasPermissions(POST_NOTIFICATIONS)
                && !sharedPrefs.getBoolean(OPT_OUT_NOTIFICATION, false))
        val missingBatteryOptimization = (!hasBatteryPermission()
                && !sharedPrefs.getBoolean(OPT_OUT_BATTERY_OPTIMIZER, false))
        val missingPermissions = mutableListOf(
            showIntroScreen,
            missingLocation,
            missingBgLocation,
            missingNotification,
            missingBatteryOptimization,
        )
        showSummaryScreen = missingPermissions.contains(true)
                && !sharedPrefs.getBoolean(OPT_OUT_LOCATION, false)
        missingPermissions.add(showSummaryScreen)
        val numPages = missingPermissions.count { it }

        initialScreen = getStartingScreen(showSummaryScreen, showIntroScreen)

        setContent {
            DashTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    if (loadSingleScreen != null) {
                        if (initialScreen != null && initialScreen!! > loadSingleScreen!!) {
                            finish()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            when (loadSingleScreen!!) {
                                INTRO_SCREEN -> {
                                    OnboardingIntroScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity
                                    )
                                }
                                LOCATION_SCREEN -> {
                                    GetLocationPermissionsScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity
                                    )
                                }
                                BG_LOCATION_SCREEN -> {
                                    GetBgLocationPermissionScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity
                                    )
                                }
                                NOTIFICATION_SCREEN -> {
                                    if (SDK_INT >= TIRAMISU) {
                                        GetNotificationPermissionScreen(
                                            modifier = Modifier.weight(1f),
                                            activity = this@OnboardingMileageActivity,
                                            finishWhenDone = true
                                        )
                                    }
                                }
                                OPTIMIZATION_OFF_SCREEN -> {
                                    GetBatteryPermissionScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity,
                                        finishWhenDone = true
                                    )
                                }
                                SUMMARY_SCREEN -> {
                                    SummaryScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity,
                                    )
                                }
                                OPTIMIZATION_ON_SCREEN -> {
                                    ReenableBatteryOptimizationScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity,
                                        finishWhenDone = true
                                    )
                                }
                            }
                        }
                    } else if (initialScreen == null) {
                        finish()
                    } else {
                        navController = rememberAnimatedNavController()

                        Column(modifier = Modifier.fillMaxSize()) {
                            val navBackStackEntry by navController!!.currentBackStackEntryAsState()

                            val route = navBackStackEntry?.destination?.route

                            AnimatedNavHost(
                                navController = navController!!,
                                startDestination = initialScreen!!.name,
                                modifier = Modifier.weight(1f),
                                enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                                exitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() },
                                popEnterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                                popExitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() }
                            ) {
                                composable(LOCATION_SCREEN.name) {
                                    GetLocationPermissionsScreen(activity = this@OnboardingMileageActivity)
                                }
                                composable(BG_LOCATION_SCREEN.name) {
                                    GetBgLocationPermissionScreen(activity = this@OnboardingMileageActivity)
                                }
                                composable(NOTIFICATION_SCREEN.name) {
                                    if (SDK_INT >= TIRAMISU) {
                                        GetNotificationPermissionScreen(activity = this@OnboardingMileageActivity)
                                    }
                                }
                                composable(OPTIMIZATION_OFF_SCREEN.name) {
                                    GetBatteryPermissionScreen(activity = this@OnboardingMileageActivity)
                                }
                                composable(INTRO_SCREEN.name) {
                                    OnboardingIntroScreen(activity = this@OnboardingMileageActivity)
                                }
                                composable(SUMMARY_SCREEN.name) {
                                    SummaryScreen(activity = this@OnboardingMileageActivity)
                                }
                            }

                            val absoluteScreenNumber = route?.let {
                                OnboardingScreen.valueOf(it).ordinal
                            } ?: 0

                            val currentPage =
                                missingPermissions.subList(0, absoluteScreenNumber).count { it }

                            PageIndicator(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 0.dp
                                ),
                                numPages = numPages,
                                selectedPage = currentPage
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     *  wraps a when expression that checks for missing permissions in this order:
     *  none > battery > notification > bg location > location & activity > all
     *
     * @param T return type
     * @param optOutLocation Has opted out of mileage tracking
     * @param hasAllPermissions Has [REQUIRED_PERMISSIONS] + [AppCompatActivity.hasBatteryPermission]
     * + [OPTIONAL_PERMISSIONS] or has opted out of optional permissions
     * @param hasNotification Has [REQUIRED_PERMISSIONS] + []
     * @param hasBgLocation SDK_INT >= TIRAMISU and has [ACCESS_BACKGROUND_LOCATION] and
     * [LOCATION_PERMISSIONS]
     * @param hasLocation Has [LOCATION_PERMISSIONS]
     * @param noPermissions also the default return value
     * @return the matching parameter
     */
    internal fun <T : Any> whenHasDecided(
        optOutLocation: T? = null,
        hasAllPermissions: T? = null,
        hasNotification: T? = null,
        hasBgLocation: T? = null,
        hasLocation: T? = null,
        noPermissions: T? = null
    ): T? {
        val optOut = !sharedPrefs.getBoolean(ASK_AGAIN_LOCATION, true)
                && sharedPrefs.getBoolean(OPT_OUT_LOCATION, false)

        val askAgainLocation =
            sharedPrefs.getBoolean(ASK_AGAIN_LOCATION, false)

        val hasDecidedLocation = this.hasPermissions(*LOCATION_PERMISSIONS)
                || sharedPrefs.getBoolean(OPT_OUT_LOCATION, false)
                || askAgainLocation

        val askAgainBgLocation =
            sharedPrefs.getBoolean(ASK_AGAIN_BG_LOCATION, false)

        val hasDecidedBgLocation =
            (this.hasPermissions(*REQUIRED_PERMISSIONS) && hasDecidedLocation)
                    || sharedPrefs.getBoolean(OPT_OUT_LOCATION, false)
                    || askAgainBgLocation

        val askAgainNotification =
            sharedPrefs.getBoolean(ASK_AGAIN_NOTIFICATION, false)

        val hasDecidedNotifs =
            (this.hasPermissions(*OPTIONAL_PERMISSIONS) && hasDecidedBgLocation)
                    || sharedPrefs.getBoolean(OPT_OUT_NOTIFICATION, false)
                    || askAgainNotification

        val askAgainBattery = sharedPrefs.getBoolean(ASK_AGAIN_BATTERY_OPTIMIZER, false)

        val hasDecidedBattery = (permissionsHelper.hasBatteryPermission && hasDecidedNotifs)
                || sharedPrefs.getBoolean(OPT_OUT_BATTERY_OPTIMIZER, false)
                || askAgainBattery

        val hasAll = hasDecidedBattery
                && hasDecidedNotifs
                && hasDecidedBgLocation
                && hasDecidedLocation

        return when {
            optOut -> {
                optOutLocation
            }
            hasAll -> {
                hasAllPermissions
            }
            hasDecidedNotifs -> {
                hasNotification
            }
            hasDecidedBgLocation -> {
                hasBgLocation
            }
            hasDecidedLocation -> {
                hasLocation
            }
            else -> {
                noPermissions
            }
        }
    }

    /**
     * @param showSummaryScreen whether to show [SUMMARY_SCREEN]
     * @param showIntroScreen whether to s [INTRO_SCREEN]
     * @return The next [OnboardingScreen] based on which permissions have been decided (granted,
     * postponed or denied). Null if all permissions are decided and [showSummaryScreen] is false.
     */
    private fun getStartingScreen(
        showSummaryScreen: Boolean,
        showIntroScreen: Boolean
    ) = whenHasDecided(
        optOutLocation = if (showSummaryScreen) SUMMARY_SCREEN else null,
        hasAllPermissions = if (showSummaryScreen) SUMMARY_SCREEN else null,
        hasNotification = OPTIMIZATION_OFF_SCREEN,
        hasBgLocation = NOTIFICATION_SCREEN,
        hasLocation = BG_LOCATION_SCREEN,
        noPermissions = if (showIntroScreen) INTRO_SCREEN else LOCATION_SCREEN
    )

    override fun onResume() {
        super.onResume()

        onPermissionsUpdated()
    }

    @SuppressLint("ApplySharedPref")
    override fun onDestroy() {
        permissionsHelper.sharedPrefs.edit()
            .putBoolean(ASK_AGAIN_LOCATION, false)
            .putBoolean(ASK_AGAIN_BG_LOCATION, false)
            .putBoolean(ASK_AGAIN_NOTIFICATION, false)
            .putBoolean(ASK_AGAIN_BATTERY_OPTIMIZER, false)
            .commit()

        super.onDestroy()
    }

    private fun onPermissionsUpdated() {
        if (loadSingleScreen == null) {
            val route = whenHasDecided(
                optOutLocation = SUMMARY_SCREEN,
                hasAllPermissions = SUMMARY_SCREEN,
                hasNotification = OPTIMIZATION_OFF_SCREEN,
                hasBgLocation = NOTIFICATION_SCREEN,
                hasLocation = BG_LOCATION_SCREEN,
                noPermissions = LOCATION_SCREEN
            )!!

            navController?.navigate(route.name) {
                launchSingleTop = true
            }
        } else {
            val loadSingleComplete = when (loadSingleScreen!!) {
                LOCATION_SCREEN -> hasPermissions(*LOCATION_PERMISSIONS)
                BG_LOCATION_SCREEN -> hasPermissions(ACCESS_BACKGROUND_LOCATION)
                NOTIFICATION_SCREEN -> SDK_INT < TIRAMISU || hasPermissions(POST_NOTIFICATIONS)
                OPTIMIZATION_OFF_SCREEN -> hasBatteryPermission()
                OPTIMIZATION_ON_SCREEN -> !hasBatteryPermission()
                INTRO_SCREEN -> null
                SUMMARY_SCREEN -> null
            }

            if (loadSingleComplete == true) {
                finish()
            }
        }
    }

    fun getLocationPermissions() {
        when {
            !sharedPrefs.getBoolean(LOCATION_ENABLED, true) -> {}
            this.hasPermissions(*REQUIRED_PERMISSIONS) -> {}
            else -> {
                multiplePermissionsLauncher.launch(LOCATION_PERMISSIONS)
            }
        }
    }

    fun getBgPermission() {
        when {
            !sharedPrefs.getBoolean(LOCATION_ENABLED, true) -> {}
            this.hasPermissions(*REQUIRED_PERMISSIONS) -> {}
            else -> {
                singlePermissionLauncher.launch(ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    @RequiresApi(TIRAMISU)
    fun getNotificationPermission() {
        when {
            !sharedPrefs.getBoolean(NOTIFICATION_ENABLED, true) -> {}
            this.hasPermissions(POST_NOTIFICATIONS) -> {}
            else -> {
                singlePermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }

    fun getBatteryPermission(ifHasPermission: Boolean = true) {
        when (ifHasPermission) {
            !sharedPrefs.getBoolean(BG_BATTERY_ENABLED, true) -> {}
            hasBatteryPermission() -> {}
            else -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }
    }

    fun setBooleanPref(prefKey: String, value: Boolean) {
        permissionsHelper.setBooleanPref(
            prefKey,
            value,
            ::onPermissionsUpdated
        )
    }

    internal fun setOptOutLocation(optOut: Boolean) = setBooleanPref(OPT_OUT_LOCATION, optOut)

    /**
     * Sets shared pref [LOCATION_ENABLED] to [enabled]. If [enabled] is false,
     * [NOTIFICATION_ENABLED] and [BG_BATTERY_ENABLED] are set to false also. Calls
     * [onPermissionsUpdated] when finished.
     */
    internal fun setLocationEnabled(enabled: Boolean) {
        if (!enabled) {
            permissionsHelper.setBooleanPref(NOTIFICATION_ENABLED, false)
            permissionsHelper.setBooleanPref(BG_BATTERY_ENABLED, false)
        }
        permissionsHelper.setBooleanPref(LOCATION_ENABLED, enabled, ::onPermissionsUpdated)
    }


    companion object {
        internal const val EXTRA_PERMISSIONS_ROUTE = "extra_permissions_route"
    }
}

enum class OnboardingScreen {
    // page order/numbering matters for these
    INTRO_SCREEN, LOCATION_SCREEN, BG_LOCATION_SCREEN, NOTIFICATION_SCREEN,
    OPTIMIZATION_OFF_SCREEN, SUMMARY_SCREEN,

    // page order/numbering doesn't matter - intended for loadSingleScreen ONLY
    OPTIMIZATION_ON_SCREEN
}

@Composable
internal fun PageIndicator(modifier: Modifier = Modifier, numPages: Int, selectedPage: Int = 0) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        for (i in 0 until numPages) {
            Icon(
                if (i == selectedPage) Icons.Filled.Circle else Icons.TwoTone.Circle,
                contentDescription = "circle",
                modifier = Modifier.size(8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Preview(showBackground = true)
@Composable
fun GetPermissionsActivityPreview() {
    DashTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                GetLocationPermissionsScreen()
                PageIndicator(numPages = 4, selectedPage = 0)
            }
        }
    }
}