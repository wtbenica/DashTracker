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

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.WelcomeNavHost
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.WelcomeNavHostPreview
import com.wtb.dashTracker.util.PermissionsHelper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREFS_SHOULD_SHOW_INTRO

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
class WelcomeActivity : ComponentActivity() {
    fun setOptOutPref(prefKey: String, optedOut: Boolean, onPrefSet: (() -> Unit)? = null) =
        permissionsHelper.setBooleanPref(
            prefKey,
            optedOut,
        ) {
            onPrefSet?.invoke()
            permissionsHelper.setBooleanPref(PREFS_SHOULD_SHOW_INTRO, false)
            finish()
        }

    private val permissionsHelper = PermissionsHelper(this)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        setContent {
            WelcomeNavHost(this)
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    WelcomeNavHostPreview()
}