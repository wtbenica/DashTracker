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
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetBatteryPermission
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetLocationPermissionsScreen
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetNotificationPermissionScreen
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.OnboardingIntroScreen
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.util.*
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_IN_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREFS_ASK_AGAIN_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREFS_ASK_AGAIN_NOTIFICATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREFS_OPT_OUT_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.SHOW_NOTIFICATION
import kotlinx.coroutines.*

sealed class OnboardMileageTrackingScreen(val route: String, val page: Int) {
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

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
class OnboardingMileageActivity : AppCompatActivity() {
    fun setOptOutPref(prefKey: String, optedOut: Boolean) {
        Log.d(TAG, "whenPermissions | setOptOutPref: $prefKey $optedOut")
        permissionsHelper.setBooleanPref(
            prefKey,
            optedOut,
            ::onPermissionsUpdated
        )
    }

    internal fun setOptOutLocation(optOut: Boolean) = setOptOutPref(OPT_IN_LOCATION, !optOut)

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

        Log.d(TAG, "GetPermissionsActivity | onCreate")
        val missingPermissions = listOf(
            showIntroScreen,
            !hasPermissions(this, *LOCATION_PERMISSIONS),
            !hasPermissions(this, ACCESS_BACKGROUND_LOCATION),
            SDK_INT >= TIRAMISU && !hasPermissions(this, POST_NOTIFICATIONS),
            !hasBatteryPermission()
        )

        val numPages = missingPermissions.count { it }

        val startDest: String? = permissionsHelper.whenPermissions(
            optOutLocation = null,
            hasAllPermissions = null,
            missingBatteryPermission = OnboardMileageTrackingScreen.BatteryOptimizationScreen.route,
            missingNotificationPermission = OnboardMileageTrackingScreen.NotificationScreen.route,
            missingBgLocationPermission = OnboardMileageTrackingScreen.BgLocationScreen.route,
            missingAllPermissions = if (showIntroScreen) {
                OnboardMileageTrackingScreen.IntroScreen.route
            } else {
                OnboardMileageTrackingScreen.LocationScreen.route
            }
        )

        setContent {
            DashTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    navController = rememberAnimatedNavController()

                    if (startDest == null) {
                        finish()
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val navBackStackEntry by navController!!.currentBackStackEntryAsState()

                            val route = navBackStackEntry?.destination?.route

                            Log.d(TAG, "GetPermissionActivity | route: $route")

                            AnimatedNavHost(
                                navController = navController!!,
                                startDestination = startDest,
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
                                composable(OnboardMileageTrackingScreen.NotificationScreen.route) {
                                    if (SDK_INT >= TIRAMISU) {
                                        GetNotificationPermissionScreen(activity = this@OnboardingMileageActivity)
                                    }
                                }
                                composable(OnboardMileageTrackingScreen.BatteryOptimizationScreen.route) {
                                    GetBatteryPermission(activity = this@OnboardingMileageActivity)
                                }
                                composable(OnboardMileageTrackingScreen.IntroScreen.route) {
                                    OnboardingIntroScreen(activity = this@OnboardingMileageActivity)
                                }
                            }

                            val absoluteScreenNumber =
                                OnboardMileageTrackingScreen.getScreenByRoute(route)

                            val currentPage =
                                missingPermissions.subList(0, absoluteScreenNumber).count { it } + 1

                            Log.d(
                                TAG, "GetPermissionActivity | numPage: $numPages | screenNum: " +
                                        "$absoluteScreenNumber | currentPage: $currentPage"
                            )

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

    override fun onDestroy() {
        permissionsHelper.sharedPrefs.edit()
            .putBoolean(PREFS_ASK_AGAIN_NOTIFICATION, false)
            .putBoolean(PREFS_ASK_AGAIN_BATTERY_OPTIMIZER, false).commit()
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
            missingBatteryPermission = {
                navController?.navigate(OnboardMileageTrackingScreen.BatteryOptimizationScreen.route) {
                    launchSingleTop = true
                }
            },
            missingNotificationPermission = {
                navController?.navigate(OnboardMileageTrackingScreen.NotificationScreen.route) {
                    launchSingleTop = true
                }
            },
            missingBgLocationPermission = {
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
            !sharedPrefs.getBoolean(OPT_IN_LOCATION, true) -> {}
            hasPermissions(this, *REQUIRED_PERMISSIONS) -> {}
            else -> {
                multiplePermissionsLauncher.launch(LOCATION_PERMISSIONS)
            }
        }
    }

    fun getBgPermission() {
        when {
            !sharedPrefs.getBoolean(OPT_IN_LOCATION, true) -> {}
            hasPermissions(this, *REQUIRED_PERMISSIONS) -> {}
            else -> {
                singlePermissionLauncher.launch(ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    @RequiresApi(TIRAMISU)
    fun getNotificationPermission() {
        when {
            !sharedPrefs.getBoolean(SHOW_NOTIFICATION, true) -> {}
            hasPermissions(this, POST_NOTIFICATIONS) -> {}
            else -> {
                singlePermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }

    fun getBatteryPermission() {
        when {
            sharedPrefs.getBoolean(PREFS_OPT_OUT_BATTERY_OPTIMIZER, false) -> {}
            hasBatteryPermission() -> {}
            else -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }
    }
}

@Composable
internal fun PageIndicator(modifier: Modifier = Modifier, numPages: Int, selectedPage: Int = 1) {
    if (numPages > 1) {
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