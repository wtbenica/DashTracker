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

import android.content.SharedPreferences
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.wtb.dashTracker.util.PermissionsHelper.Companion.AUTHENTICATION_ENABLED

/**
 *
 *
 */
abstract class AuthenticatedActivity : AppCompatActivity() {
    protected val sharedPrefs: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    protected var expectedExit: Boolean = false

    protected var isAuthenticated: Boolean = false

    private val authenticationEnabled: Boolean
        get() = sharedPrefs.getBoolean(AUTHENTICATION_ENABLED, true)

    abstract val onAuthentication: () -> Unit
    protected open val onAuthError: (() -> Unit)? = null
    protected open val onAuthFailed: (() -> Unit)? = null

    private var disableBackButtonCallback: OnBackPressedCallback? = null

    override fun onPause() {
        super.onPause()

        if (authenticationEnabled && !expectedExit) {
            isAuthenticated = false
            lockScreen()
        }
    }

    protected abstract fun lockScreen()

    /**
     * Authenticates user using [BiometricPrompt]
     */
    fun authenticate(
        onSuccess: () -> Unit = onAuthentication,
        onError: (() -> Unit)? = onAuthError,
        onFailed: (() -> Unit)? = onAuthFailed,
        forceAuthentication: Boolean = false
    ) {
        if (forceAuthentication || (authenticationEnabled && !isAuthenticated)) {
            val executor = ContextCompat.getMainExecutor(this)

            disableBackButtonCallback?.remove()
            disableBackButtonCallback = onBackPressedDispatcher.addCallback(this, true) {
                authenticate(onSuccess, onError, onFailed)
            }

            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        disableBackButtonCallback?.remove()
                        disableBackButtonCallback = null
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError?.invoke()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onFailed?.invoke()
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock to access DashTracker")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            disableBackButtonCallback?.remove()
            disableBackButtonCallback = null
            onSuccess()
        }
    }
}