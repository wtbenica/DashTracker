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
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_welcome.ui.InitialScreenCallback
import com.wtb.dashTracker.ui.activity_welcome.ui.InitialSettings
import com.wtb.dashTracker.ui.activity_welcome.ui.WelcomeScreen
import com.wtb.dashTracker.ui.activity_welcome.ui.WelcomeScreenCallback
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.ActivityScreen
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.util.PermissionsHelper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_ONBOARD_INTRO
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SKIP_WELCOME_SCREEN
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
class WelcomeActivity : ComponentActivity(), WelcomeScreenCallback, InitialScreenCallback {

    private val permissionsHelper = PermissionsHelper(this)
    private var navController: NavHostController? = null

    override val isDarkMode: Boolean
        get() = permissionsHelper.uiModeIsDarkMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        setContent {
            DashTrackerTheme(darkTheme = permissionsHelper.uiModeIsDarkMode) {
                ActivityScreen {
                    navController = rememberAnimatedNavController()

                    val startScreen = if (
                        permissionsHelper.sharedPrefs.getBoolean(
                            PREF_SKIP_WELCOME_SCREEN,
                            false
                        )
                    ) {
                        permissionsHelper.setBooleanPref(PREF_SKIP_WELCOME_SCREEN, false)
                        WelcomeActivityScreen.SETTINGS
                    } else {
                        WelcomeActivityScreen.WELCOME
                    }

                    AnimatedNavHost(
                        navController = navController!!,
                        startDestination = startScreen.name,
                        modifier = Modifier.weight(1f),
                        enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                        exitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() },
                        popEnterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                        popExitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() }
                    ) {
                        composable(WelcomeActivityScreen.WELCOME.name) {
                            WelcomeScreen(callback = this@WelcomeActivity)
                        }
                        composable(WelcomeActivityScreen.SETTINGS.name) {
                            InitialSettings(this@WelcomeActivity)
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
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun DefaultPreview() {
    val callback = object : WelcomeScreenCallback {
        override fun nextScreen() {}

        override val isDarkMode: Boolean
            get() = true
    }

    DashTrackerTheme {
        Surface {
            Column {
                WelcomeScreen(modifier = Modifier.weight(1f), callback)
            }
        }
    }
}