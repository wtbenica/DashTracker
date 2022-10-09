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
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.ui.*
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed class PermissionScreen(val route: String, @StringRes val resourceId: Int, val page: Int) {
    object LocationScreen : PermissionScreen(
        route = "location",
        resourceId = R.string.route_location_permission,
        page = 0
    )

    object BgLocationScreen : PermissionScreen(
        route = "bg_location",
        resourceId = R.string.route_bg_location_permission,
        page = 1
    )

    object NotificationScreen : PermissionScreen(
        route = "notification",
        resourceId = R.string.route_notification_permission,
        page = 2
    )

    object BatteryOptimizationScreen : PermissionScreen(
        route = "battery",
        resourceId = R.string.route_battery_permission,
        page = 3
    )

    companion object {
        fun getScreenByRoute(route: String?): Int {
            if (route != null) {
                PermissionScreen::class.sealedSubclasses.forEach {
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
class GetPermissionsActivity : AppCompatActivity() {
    private val sharedPrefs
        get() = getSharedPreferences(MainActivity.DT_SHARED_PREFS, 0)

    private val multiplePermissionsLauncher: ActivityResultLauncher<Array<String>> =
        registerMultiplePermissionsLauncher(onGranted = ::navigateToBgLocationScreen)

    private val singlePermissionLauncher: ActivityResultLauncher<String> =
        registerSinglePermissionLauncher()

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        Log.d(TAG, "onCreate")


        val missingPermissions = listOf<Boolean>(
            !hasPermissions(this, *LOCATION_PERMISSIONS),
            !hasPermissions(this, ACCESS_BACKGROUND_LOCATION),
            SDK_INT >= TIRAMISU && !hasPermissions(this, POST_NOTIFICATIONS),
            !hasBatteryPermission()
        )

        val numPages = missingPermissions.count { it }

        val startDest: String? = whenPermissions(
            hasAllPermissions = null,
            missingBatteryPerm = PermissionScreen.BatteryOptimizationScreen.route,
            missingNotificationPerm = PermissionScreen.NotificationScreen.route,
            missingBgLocation = PermissionScreen.BgLocationScreen.route,
            hasNoPermissions = PermissionScreen.LocationScreen.route
        )

        setContent {
            DashTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    navController = rememberAnimatedNavController()

                    if (startDest == null) {
                        finish()
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val navBackStackEntry by navController!!.currentBackStackEntryAsState()

                            val route = navBackStackEntry?.destination?.route

                            AnimatedNavHost(
                                navController = navController!!,
                                startDestination = startDest,
                                modifier = Modifier
                                    .weight(1f),
                                enterTransition = { slideInHorizontally { it / 2 } + fadeIn() },
                                exitTransition = { slideOutHorizontally { -it / 2 } + fadeOut() }
                            ) {
                                composable(PermissionScreen.LocationScreen.route) {
                                    GetLocationPermissionsScreen()
                                }
                                composable(PermissionScreen.BgLocationScreen.route) {
                                    GetBgLocationPermissionScreen()
                                }
                                composable(PermissionScreen.NotificationScreen.route) {
                                    GetNotificationPermissionScreen()
                                }
                                composable(PermissionScreen.BatteryOptimizationScreen.route) {
                                    GetBatteryPermission()
                                }
                            }

                            val absoluteScreenNumber = PermissionScreen.getScreenByRoute(route)

                            val currentPage = missingPermissions.subList(0, absoluteScreenNumber)
                                        .count { it } + 1

                            Log.d(TAG, "Screen: $absoluteScreenNumber | Page: $currentPage")

                            PageIndicator(numPages = numPages, selectedPage = currentPage)

                            Row {
                                AnimatedContent(
                                    targetState = route,
                                    transitionSpec = {
                                        slideInHorizontally { it / 2 } + fadeIn() with
                                                slideOutHorizontally { -it / 2 } + fadeOut()
                                    }
                                ) { target ->
                                    when (target) {
                                        PermissionScreen.LocationScreen.route ->
                                            GetLocationPermissionsNav(this@GetPermissionsActivity)
                                        PermissionScreen.BgLocationScreen.route ->
                                            GetBgLocationPermissionNav(this@GetPermissionsActivity)
                                        PermissionScreen.NotificationScreen.route ->
                                            if (SDK_INT >= TIRAMISU) {
                                                GetNotificationsPermissionNav(this@GetPermissionsActivity)
                                            }
                                        PermissionScreen.BatteryOptimizationScreen.route ->
                                            GetBatteryPermissionNav(this@GetPermissionsActivity)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        whenPermissions(
            hasAllPermissions = {
                finish()
            },
            missingBatteryPerm = {
                navController?.navigate(PermissionScreen.BatteryOptimizationScreen.route) {
                    launchSingleTop = true
                }
            },
            missingNotificationPerm = {
                navController?.navigate(PermissionScreen.NotificationScreen.route) {
                    launchSingleTop = true
                }
            },
            missingBgLocation = {
                navController?.navigate(PermissionScreen.BgLocationScreen.route) {
                    launchSingleTop = true
                }
            },
            hasNoPermissions = {
                navController?.navigate(PermissionScreen.LocationScreen.route) {
                    launchSingleTop = true
                }
            }
        )?.invoke()
    }

    fun getLocationPermissions() {
        when {
            sharedPrefs.getBoolean(MainActivity.PREFS_OPT_OUT_LOCATION, false) -> {}
            hasPermissions(this, *REQUIRED_PERMISSIONS) -> {}
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
//                showRationaleLocation {
                multiplePermissionsLauncher.launch(LOCATION_PERMISSIONS)
//                }
            }
            else -> {
                multiplePermissionsLauncher.launch(LOCATION_PERMISSIONS)
            }
        }
    }

    fun getBgPermission() {
        when {
            sharedPrefs.getBoolean(MainActivity.PREFS_OPT_OUT_LOCATION, false) -> {}
            hasPermissions(this, *REQUIRED_PERMISSIONS) -> {}
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
//                showRationaleLocation {
                singlePermissionLauncher.launch(ACCESS_BACKGROUND_LOCATION)
//                }
            }
            else -> {
                singlePermissionLauncher.launch(ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    @RequiresApi(TIRAMISU)
    fun getNotificationPermission() {
        when {
            sharedPrefs.getBoolean(MainActivity.PREFS_OPT_OUT_NOTIFICATION, false) -> {}
            hasPermissions(this, *REQUIRED_PERMISSIONS) -> {}
            shouldShowRequestPermissionRationale(POST_NOTIFICATIONS) -> {
//                showRationaleLocation {
                singlePermissionLauncher.launch(POST_NOTIFICATIONS)
//                }
            }
            else -> {
                singlePermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }

    fun getBatteryPermission() {
        when {
            sharedPrefs.getBoolean(MainActivity.PREFS_OPT_OUT_BATTERY_OPTIM, false) -> {}
            hasBatteryPermission() -> {}
            else -> {
                val intent = Intent().apply {
                    setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                }
                startActivity(intent)
            }
        }
    }

    private fun navigateToBgLocationScreen() {
        navController?.navigate(PermissionScreen.BgLocationScreen.route) {
            launchSingleTop = true
        }
    }
}

@Composable
fun PageIndicator(modifier: Modifier = Modifier, numPages: Int, selectedPage: Int = 1) =
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
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
                GetLocationPermissionsScreen(modifier = Modifier.weight(1f))
                PageIndicator(numPages = 4, selectedPage = 0)
                GetLocationPermissionsNav()
            }
        }
    }
}