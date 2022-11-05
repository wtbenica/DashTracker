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

package com.wtb.dashTracker.ui.activity_welcome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.activity_welcome.ui.InitialScreenCallback
import com.wtb.dashTracker.ui.activity_welcome.ui.InitialSettings
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.WelcomeScreen
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.WelcomeScreenCallback
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.util.PermissionsHelper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_ONBOARD_INTRO
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
class WelcomeActivity : ComponentActivity(), WelcomeScreenCallback, InitialScreenCallback {

    private val permissionsHelper = PermissionsHelper(this)
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        setContent {
            DashTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp)
                    ) {
                        navController = rememberAnimatedNavController()

                        Column(modifier = Modifier.fillMaxSize()) {
                            AnimatedNavHost(
                                navController = navController!!,
                                startDestination = WelcomeActivityScreen.WELCOME.name,
                                modifier = Modifier.weight(1f),
                                enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                                exitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() },
                                popEnterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                                popExitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() }
                            ) {
                                composable(WelcomeActivityScreen.WELCOME.name) {
                                    WelcomeScreen(
                                        modifier = Modifier.padding(bottom = 16.dp),
                                        callback = this@WelcomeActivity
                                    )
                                }
                                composable(WelcomeActivityScreen.SETTINGS.name) {
                                    InitialSettings(this@WelcomeActivity)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun nextScreen() {
        navController?.navigate(WelcomeActivityScreen.SETTINGS.name)
    }

    override fun nextScreen2() {
        Log.d(TAG, "WelcomeActivity | nextScreen")
        permissionsHelper.setBooleanPref(PREF_SHOW_ONBOARD_INTRO, false)
        startActivity(Intent(this, OnboardingMileageActivity::class.java))
        finish()
    }
}

enum class WelcomeActivityScreen {
    WELCOME, SETTINGS
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val callback = object : WelcomeScreenCallback {
        override fun nextScreen() {

        }
    }

    DashTrackerTheme {
        Surface {
            Column {
                WelcomeScreen(modifier = Modifier.weight(1f), callback)
            }
        }
    }
}