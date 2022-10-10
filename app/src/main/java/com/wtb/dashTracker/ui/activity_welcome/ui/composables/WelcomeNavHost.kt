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

package com.wtb.dashTracker.ui.activity_welcome.ui.composables

import androidx.compose.animation.*
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun WelcomeNavHost(activity: WelcomeActivity? = null) {
    val navController = rememberAnimatedNavController()

    DashTrackerTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                AnimatedNavHost(
                    navController = navController,
                    startDestination = Screen.WelcomeScreen.route,
                    modifier = Modifier
                        .weight(1f),
                    enterTransition = { slideInHorizontally { it / 2 } + fadeIn() },
                    exitTransition = { slideOutHorizontally { -it / 2 } + fadeOut() }
                ) {
                    composable(Screen.WelcomeScreen.route) { WelcomeScreen() }
                    composable(Screen.WhatsNewScreen.route) { WhatsNewScreen() }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val route = navBackStackEntry?.destination?.route

                Row {
                    AnimatedContent(
                        targetState = route,
                        transitionSpec = {
                            slideInHorizontally { it / 2 } + fadeIn() with
                                    slideOutHorizontally { -it / 2 } + fadeOut()
                        }
                    ) { target ->
                        when (target) {
                            Screen.WelcomeScreen.route -> WelcomeNav(navController)
                            Screen.WhatsNewScreen.route -> WhatsNewNav(activity)
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Preview(showBackground = true)
@Composable
fun WelcomeNavHostPreview() {
    WelcomeNavHost()
}