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
import android.util.Log
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
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardMileageTrackingScreen.NotificationScreen
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetBatteryPermissionScreen
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetLocationPermissionsScreen
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetNotificationPermissionScreen
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.OnboardingIntroScreen
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.util.*
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_NOTIFICATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.BG_BATTERY_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.LOCATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.NOTIFICATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_LOCATION
import kotlinx.coroutines.*

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
class OnboardingMileageActivity : AppCompatActivity() {
    fun setBooleanPref(prefKey: String, value: Boolean) {
        Log.d(TAG, "whenPermissions | setOptOutPref: $prefKey $value")
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
            permissionsHelper.setBooleanPref(NOTIFICATION_ENABLED, enabled)
            permissionsHelper.setBooleanPref(BG_BATTERY_ENABLED, enabled)
        }
        permissionsHelper.setBooleanPref(LOCATION_ENABLED, enabled, ::onPermissionsUpdated)
    }

    private val permissionsHelper = PermissionsHelper(this)

    private val sharedPrefs
        get() = permissionsHelper.sharedPrefs

    private val multiplePermissionsLauncher: ActivityResultLauncher<Array<String>> =
        registerMultiplePermissionsLauncher()

    private val singlePermissionLauncher: ActivityResultLauncher<String> =
        registerSinglePermissionLauncher()

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val showIntroScreen = sharedPrefs.getBoolean(ASK_AGAIN_LOCATION, true)

        Log.d(TAG, "onCreate |")
        val missingPermissions = listOf(
            showIntroScreen,
            !hasPermissions(this, *LOCATION_PERMISSIONS),
            !hasPermissions(this, ACCESS_BACKGROUND_LOCATION),
            (SDK_INT >= TIRAMISU)
                    && !hasPermissions(this, POST_NOTIFICATIONS)
                    && sharedPrefs.getBoolean(NOTIFICATION_ENABLED, true),
            !hasBatteryPermission()
                    && sharedPrefs.getBoolean(BG_BATTERY_ENABLED, true)
        )

        val numPages = missingPermissions.count { it }

        @Suppress("DEPRECATION")
        val launchScreen: OnboardMileageTrackingScreen? = if (SDK_INT >= TIRAMISU) {
            intent.getSerializableExtra(
                EXTRA_PERMISSIONS_ROUTE,
                OnboardMileageTrackingScreen::class.java
            )
        } else {
            intent.getSerializableExtra(EXTRA_PERMISSIONS_ROUTE) as OnboardMileageTrackingScreen?
        }

        Log.d(TAG, "onCreate | singlePermIntent: ${launchScreen?.route}")

        val screen: OnboardMileageTrackingScreen? = permissionsHelper.whenPermissions(
            optOutLocation = null,
            hasAllPermissions = null,
            hasNotification = OnboardMileageTrackingScreen.BatteryOptimizationScreen,
            hasBgLocation = NotificationScreen,
            hasLocation = OnboardMileageTrackingScreen.BgLocationScreen,
            noPermissions = if (showIntroScreen) {
                OnboardMileageTrackingScreen.IntroScreen
            } else {
                OnboardMileageTrackingScreen.LocationScreen
            }
        )

        Log.d(TAG, "onCreate | startDest: ${screen?.route}")

        setContent {
            DashTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    if (launchScreen != null) {
                        Log.d(TAG, "onCreate | launchScreen")
                        if (screen?.page?.let { it > launchScreen.page } == true) {
                            Log.d(TAG, "onCreate | has applicable permission")
                            finish()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            when (launchScreen) {
                                is OnboardMileageTrackingScreen.IntroScreen -> {
                                    OnboardingIntroScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity
                                    )
                                }
                                is OnboardMileageTrackingScreen.LocationScreen -> {
                                    GetLocationPermissionsScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity
                                    )
                                }
                                is OnboardMileageTrackingScreen.BgLocationScreen -> {
                                    GetBgLocationPermissionScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity
                                    )
                                }
                                is NotificationScreen -> {
                                    if (SDK_INT >= TIRAMISU) {
                                        GetNotificationPermissionScreen(
                                            modifier = Modifier.weight(1f),
                                            activity = this@OnboardingMileageActivity,
                                            finishWhenDone = true
                                        )
                                    }
                                }
                                is OnboardMileageTrackingScreen.BatteryOptimizationScreen -> {
                                    GetBatteryPermissionScreen(
                                        modifier = Modifier.weight(1f),
                                        activity = this@OnboardingMileageActivity,
                                        finishWhenDone = true
                                    )
                                }
                            }
                        }
                    } else if (screen == null) {
                        Log.d(TAG, "onCreate | startDest is null")
                        finish()
                    } else {
                        navController = rememberAnimatedNavController()

                        Log.d(TAG, "onCreate | startDest is normal")

                        Column(modifier = Modifier.fillMaxSize()) {
                            val navBackStackEntry by navController!!.currentBackStackEntryAsState()

                            val route = navBackStackEntry?.destination?.route

                            Log.d(TAG, "GetPermissionActivity | route: $route")

                            AnimatedNavHost(
                                navController = navController!!,
                                startDestination = screen.route,
                                modifier = Modifier.weight(1f),
                                enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                                exitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() },
                                popEnterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                                popExitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() }
                            ) {
                                composable(OnboardMileageTrackingScreen.LocationScreen.route) {
                                    GetLocationPermissionsScreen(activity = this@OnboardingMileageActivity)
                                }
                                composable(OnboardMileageTrackingScreen.BgLocationScreen.route) {
                                    GetBgLocationPermissionScreen(activity = this@OnboardingMileageActivity)
                                }
                                composable(NotificationScreen.route) {
                                    if (SDK_INT >= TIRAMISU) {
                                        GetNotificationPermissionScreen(activity = this@OnboardingMileageActivity)
                                    }
                                }
                                composable(OnboardMileageTrackingScreen.BatteryOptimizationScreen.route) {
                                    GetBatteryPermissionScreen(activity = this@OnboardingMileageActivity)
                                }
                                composable(OnboardMileageTrackingScreen.IntroScreen.route) {
                                    OnboardingIntroScreen(activity = this@OnboardingMileageActivity)
                                }
                            }

                            val absoluteScreenNumber =
                                OnboardMileageTrackingScreen.getScreenByRoute(route)

                            val currentPage =
                                missingPermissions.subList(0, absoluteScreenNumber).count { it } + 1

                            PageIndicator(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                                numPages = numPages,
                                selectedPage = currentPage
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        onPermissionsUpdated()
    }

    @SuppressLint("ApplySharedPref")
    override fun onDestroy() {
        permissionsHelper.sharedPrefs.edit()
            .putBoolean(ASK_AGAIN_NOTIFICATION, false)
            .putBoolean(ASK_AGAIN_BATTERY_OPTIMIZER, false).commit()
        super.onDestroy()
    }

    private fun onPermissionsUpdated() {
        permissionsHelper.whenPermissions(
            optOutLocation = {
                finish()
            },
            hasAllPermissions = {
                finish()
            },
            hasNotification = {
                navController?.navigate(OnboardMileageTrackingScreen.BatteryOptimizationScreen.route) {
                    launchSingleTop = true
                }
            },
            hasBgLocation = {
                navController?.navigate(NotificationScreen.route) {
                    launchSingleTop = true
                }
            },
            hasLocation = {
                navController?.navigate(OnboardMileageTrackingScreen.BgLocationScreen.route) {
                    launchSingleTop = true
                }
            }
        ) {
            navController?.navigate(OnboardMileageTrackingScreen.LocationScreen.route) {
                launchSingleTop = true
            }
        }?.invoke()
    }

    fun getLocationPermissions() {
        when {
            !sharedPrefs.getBoolean(LOCATION_ENABLED, true) -> {}
            hasPermissions(this, *REQUIRED_PERMISSIONS) -> {}
            else -> {
                multiplePermissionsLauncher.launch(LOCATION_PERMISSIONS)
            }
        }
    }

    fun getBgPermission() {
        when {
            !sharedPrefs.getBoolean(LOCATION_ENABLED, true) -> {}
            hasPermissions(this, *REQUIRED_PERMISSIONS) -> {}
            else -> {
                singlePermissionLauncher.launch(ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    @RequiresApi(TIRAMISU)
    fun getNotificationPermission() {
        when {
            !sharedPrefs.getBoolean(NOTIFICATION_ENABLED, true) -> {}
            hasPermissions(this, POST_NOTIFICATIONS) -> {}
            else -> {
                singlePermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }

    fun getBatteryPermission() {
        when {
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

    companion object {
        internal const val EXTRA_PERMISSIONS_ROUTE = "extra_permissions_route"
        internal const val EXTRA_SKIP_INTRO = "extra_skip_intro"
    }
}

sealed class OnboardMileageTrackingScreen(val route: String, val page: Int) : java.io.Serializable {
    object IntroScreen : OnboardMileageTrackingScreen(
        route = "intro",
        page = 0
    )

    object LocationScreen : OnboardMileageTrackingScreen(
        route = "location",
        page = 1
    )

    object BgLocationScreen : OnboardMileageTrackingScreen(
        route = "bg_location",
        page = 2
    )

    object NotificationScreen : OnboardMileageTrackingScreen(
        route = "notification",
        page = 3
    )

    object BatteryOptimizationScreen : OnboardMileageTrackingScreen(
        route = "battery",
        page = 4
    )

    companion object {
        fun getScreenByRoute(route: String?): Int {
            if (route != null) {
                OnboardMileageTrackingScreen::class.sealedSubclasses.forEach {
                    if (it.objectInstance?.route == route) {
                        return it.objectInstance?.page ?: 0
                    }
                }
            }

            return 0
        }
    }
}

@Composable
internal fun PageIndicator(modifier: Modifier = Modifier, numPages: Int, selectedPage: Int = 1) {
//    if (numPages > 1) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Log.d(TAG, "Selected Page: $selectedPage")
        for (i in 0 until numPages) {
            Icon(
                if (i == selectedPage - 1) Icons.Filled.Circle else Icons.TwoTone.Circle,
                contentDescription = "circle",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
//    }
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