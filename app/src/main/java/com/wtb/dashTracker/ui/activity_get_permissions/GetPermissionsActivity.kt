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
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetLocationPermissionsNav
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetLocationPermissionsScreen
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetNotificationPermissionScreen
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetNotificationsPermissionNav
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed class PermissionScreen(val route: String, @StringRes val resourceId: Int) {
    object LocationScreen : PermissionScreen(
        route = "location",
        resourceId = R.string.route_location_permission
    )

    object BgLocationScreen : PermissionScreen(
        route = "bg_location",
        resourceId = R.string.route_bg_location_permission
    )

    object NotificationScreen : PermissionScreen(
        route = "notification",
        resourceId = R.string.route_notification_permission
    )
}

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

    override fun onResume() {
        super.onResume()
        when {
            hasPermissions(this, *REQUIRED_PERMISSIONS) -> {
                finish()
            }
            hasPermissions(this, *LOCATION_PERMISSIONS, ACCESS_BACKGROUND_LOCATION) -> {
                if (SDK_INT >= TIRAMISU) {
                    navController?.navigate(PermissionScreen.NotificationScreen.route) {
                        launchSingleTop = true
                    }
                } else {
                    finish()
                }
            }
            hasPermissions(this, *LOCATION_PERMISSIONS) -> {
                navController?.navigate(PermissionScreen.BgLocationScreen.route) {
                    launchSingleTop = true
                }
            }
            else -> {
                navController?.navigate(PermissionScreen.LocationScreen.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        Log.d(TAG, "onResume")

        setContent {
            DashTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    navController = rememberNavController()

                    val startDest = when {
                        hasPermissions(this, *REQUIRED_PERMISSIONS) -> {
                            // Has all permissions, including notifications if SDK_INT > TIRAMISU
                            null
                        }
                        hasPermissions(this, *LOCATION_PERMISSIONS, ACCESS_BACKGROUND_LOCATION) -> {
                            // Has all permissions except notification, only if SDK_INT < TIRAMISU
                            PermissionScreen.NotificationScreen.route
                        }
                        hasPermissions(this, *LOCATION_PERMISSIONS) -> {
                            // Missing bg location && notifications
                            PermissionScreen.BgLocationScreen.route
                        }
                        else -> {
                            // has no permissions
                            PermissionScreen.LocationScreen.route
                        }
                    }

                    if (startDest == null) {
                        finish()
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            NavHost(
                                navController = navController!!,
                                startDestination = startDest,
                                modifier = Modifier
                                    .weight(1f)
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
                            }


                            val navBackStackEntry by navController!!.currentBackStackEntryAsState()
                            val route = navBackStackEntry?.destination?.route

                            Row {
                                when (route) {
                                    PermissionScreen.LocationScreen.route ->
                                        GetLocationPermissionsNav(this@GetPermissionsActivity)
                                    PermissionScreen.BgLocationScreen.route ->
                                        GetBgLocationPermissionNav(this@GetPermissionsActivity)
                                    PermissionScreen.NotificationScreen.route ->
                                        GetNotificationsPermissionNav(this@GetPermissionsActivity)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun getPermissions() {
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

    private fun navigateToBgLocationScreen() {
        navController?.navigate(PermissionScreen.BgLocationScreen.route) {
            launchSingleTop = true
        }
//        when {
//            sharedPrefs.getBoolean(MainActivity.PREFS_DONT_ASK_BG_LOCATION, false) -> {}
//            hasPermissions(this, ACCESS_BACKGROUND_LOCATION) -> {}
//            shouldShowRequestPermissionRationale(ACCESS_BACKGROUND_LOCATION) -> {
////                showRationaleBgLocation {
//                bgLocationPermLauncher.launch(ACCESS_BACKGROUND_LOCATION)
////                }
//            }
//            else -> {
//                bgLocationPermLauncher.launch(ACCESS_BACKGROUND_LOCATION)
//            }
//        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalTextApi
@Preview(showBackground = true)
@Composable
fun GetPermissionsActivityPreview() {
    DashTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            GetLocationPermissionsScreen()
        }
    }
}