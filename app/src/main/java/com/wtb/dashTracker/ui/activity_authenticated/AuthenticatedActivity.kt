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

package com.wtb.dashTracker.ui.activity_authenticated

import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.wtb.dashTracker.ui.activity_main.authenticate
import com.wtb.dashTracker.util.PermissionsHelper.Companion.AUTHENTICATION_ENABLED

abstract class AuthenticatedActivity : AppCompatActivity() {
    protected val sharedPrefs
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    protected var expectedExit = false

    protected var isAuthenticated = false

    protected val authenticationEnabled: Boolean
        get() = sharedPrefs.getBoolean(AUTHENTICATION_ENABLED, true)

    abstract val onAuthentication: () -> Unit
    abstract val onAuthFailed: (() -> Unit)?
    abstract val onAuthError: (() -> Unit)?

    fun authenticate() {
        if (authenticationEnabled && !isAuthenticated) {
            authenticate(
                onSuccess = onAuthentication,
                onError = onAuthError,
                onFailed = onAuthFailed
            )
        } else {
            onAuthentication()
        }
    }

    override fun onPause() {
        super.onPause()

        if (!expectedExit) {
            isAuthenticated = false
            lockScreen()
        }
    }

    protected abstract fun lockScreen()
}