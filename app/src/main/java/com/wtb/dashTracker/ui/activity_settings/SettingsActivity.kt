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

package com.wtb.dashTracker.ui.activity_settings

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_authenticated.AuthenticatedActivity
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity.Companion.EXTRA_PERMISSIONS_ROUTE
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingScreen.*
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmRestartDialog
import com.wtb.dashTracker.ui.dialog_confirm.SimpleConfirmationDialog.Companion.ARG_IS_CONFIRMED
import com.wtb.dashTracker.util.PermissionsHelper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BG_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_NOTIFICATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.AUTHENTICATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.AUTHENTICATION_ENABLED_REVERTED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.BG_BATTERY_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.LOCATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.NOTIFICATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_NOTIFICATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_SUMMARY_SCREEN
import com.wtb.dashTracker.util.PermissionsHelper.Companion.UI_MODE_PREF
import com.wtb.dashTracker.util.REQUIRED_PERMISSIONS
import com.wtb.dashTracker.util.hasBatteryPermission
import com.wtb.dashTracker.util.hasPermissions
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
class SettingsActivity : AuthenticatedActivity() {
    private val permissionsHelper: PermissionsHelper
        get() = PermissionsHelper(this)

    var mileageTrackingEnabledPref: SwitchPreference? = null
    var notificationEnabledPref: SwitchPreference? = null
    var bgBatteryEnabledPref: SwitchPreference? = null
    var authenticationEnabledPref: SwitchPreference? = null
    var uiModePref: ListPreference? = null

    private val activityResult = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        if (savedInstanceState == null) {
            @Suppress("CommitTransaction")
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        if (savedInstanceState?.getBoolean(ARG_EXPECTED_EXIT) == true) {
            isAuthenticated = true
            expectedExit = false
        }

        supportFragmentManager.setFragmentResultListener(
            /* requestKey = */ REQUEST_KEY_SETTINGS_ACTIVITY_RESULT,
            /* lifecycleOwner = */ this
        ) { _, bundle ->
            val needsRestart = bundle.getBoolean(ACTIVITY_RESULT_NEEDS_RESTART)
            activityResult.apply {
                putExtra(ACTIVITY_RESULT_NEEDS_RESTART, needsRestart)
                putExtra(EXTRA_SETTINGS_ACTIVITY_IS_AUTHENTICATED, isAuthenticated)
            }
            setResult(RESULT_OK, activityResult)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)

        if (!isAuthenticated && intent?.getBooleanExtra(INTENT_EXTRA_PRE_AUTH, false) != true) {
            authenticate()
        } else {
            isAuthenticated = true
            intent.removeExtra(INTENT_EXTRA_PRE_AUTH)
        }
    }

    override fun onPause() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)

        super.onPause()
    }

    override val onAuthentication: () -> Unit
        get() = fun() {
            if (findViewById<FrameLayout>(R.id.settings)?.isVisible == false) {
                supportActionBar?.show()
                findViewById<FrameLayout>(R.id.settings)?.isVisible = true
            }
        }

    override val onAuthFailed: (() -> Unit)? = null

    override val onAuthError: (() -> Unit)? = null

    override fun lockScreen() {
        supportActionBar?.hide()
        findViewById<FrameLayout>(R.id.settings)?.isVisible = false
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val permissionsHelper: PermissionsHelper
            get() = PermissionsHelper(requireContext())

        private val sharedPrefs: SharedPreferences
            get() = permissionsHelper.sharedPrefs

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            setFragmentResultListener(
                ConfirmDialog.RESTART.key,
            ) { _, bundle ->
                val result = bundle.getBoolean(ARG_IS_CONFIRMED)

                if (result) {
                    parentFragmentManager.setFragmentResult(
                        REQUEST_KEY_SETTINGS_ACTIVITY_RESULT,
                        Bundle().apply { putBoolean(ACTIVITY_RESULT_NEEDS_RESTART, true) })
                }
            }

            return super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            (activity as SettingsActivity).mileageTrackingEnabledPref =
                findPreference(requireContext().LOCATION_ENABLED)

            (activity as SettingsActivity).notificationEnabledPref =
                findPreference<SwitchPreference>(requireContext().NOTIFICATION_ENABLED)?.apply {
                    if (SDK_INT < TIRAMISU) isVisible = false
                }

            (activity as SettingsActivity).bgBatteryEnabledPref =
                findPreference(requireContext().BG_BATTERY_ENABLED)

            (activity as SettingsActivity).authenticationEnabledPref =
                findPreference(requireContext().AUTHENTICATION_ENABLED)

            updatePreferencesToReflectCurrentPermissions()
        }

        override fun onResume() {
            super.onResume()

            preferenceScreen.removeAll()
            addPreferencesFromResource(R.xml.root_preferences)

            updatePreferencesToReflectCurrentPermissions()
        }

        private fun updatePreferencesToReflectCurrentPermissions() {
            (activity as SettingsActivity).mileageTrackingEnabledPref?.apply {
                if (!context.hasPermissions(*REQUIRED_PERMISSIONS)) {
                    isChecked = false
                }
            }

            (activity as SettingsActivity).notificationEnabledPref?.apply {
                if (SDK_INT >= TIRAMISU && !context.hasPermissions(POST_NOTIFICATIONS)) {
                    isChecked = false
                }
            }

            (activity as SettingsActivity).bgBatteryEnabledPref?.apply {
                isChecked = context.hasBatteryPermission()
            }

            (activity as SettingsActivity).authenticationEnabledPref?.apply {
                isChecked = sharedPrefs.getBoolean(requireContext().AUTHENTICATION_ENABLED, true)
            }

            (activity as SettingsActivity).uiModePref?.apply {
                value = permissionsHelper.getUiModeDisplayName()
            }
        }
    }

    private val listener: OnSharedPreferenceChangeListener =
        OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                PREF_SHOW_BASE_PAY_ADJUSTS -> {
                    ConfirmRestartDialog.newInstance().show(supportFragmentManager, null)
                }
                LOCATION_ENABLED -> {
                    val isChecked = sharedPrefs.getBoolean(key, false)
                    if (isChecked) {
                        sharedPreferences?.edit()?.apply {
                            putBoolean(ASK_AGAIN_LOCATION, false)
                            putBoolean(ASK_AGAIN_BG_LOCATION, false)
                            putBoolean(ASK_AGAIN_NOTIFICATION, false)
                            putBoolean(ASK_AGAIN_BATTERY_OPTIMIZER, false)
                            putBoolean(OPT_OUT_LOCATION, false)
                            putBoolean(PREF_SHOW_SUMMARY_SCREEN, true)
                            apply()
                        }

                        expectedExit = true

                        fun startOnboarding() =
                            startActivity(Intent(this, OnboardingMileageActivity::class.java))

                        permissionsHelper.whenHasDecided(
                            hasNotification = ::startOnboarding,
                            hasBgLocation = ::startOnboarding,
                            hasLocation = ::startOnboarding,
                            noPermissions = ::startOnboarding
                        )?.invoke()
                    } else {
                        sharedPreferences?.edit()?.apply {
                            putBoolean(ASK_AGAIN_LOCATION, false)
                            putBoolean(ASK_AGAIN_BG_LOCATION, false)
                            putBoolean(ASK_AGAIN_NOTIFICATION, false)
                            putBoolean(ASK_AGAIN_BATTERY_OPTIMIZER, false)
                            putBoolean(OPT_OUT_LOCATION, true)
                            putBoolean(PREF_SHOW_SUMMARY_SCREEN, false)
                            apply()
                        }
                    }
                }
                NOTIFICATION_ENABLED -> {
                    val isChecked = sharedPrefs.getBoolean(key, false)
                    if (isChecked) {
                        sharedPreferences?.edit()?.apply {
                            putBoolean(OPT_OUT_NOTIFICATION, false)
                            putBoolean(ASK_AGAIN_NOTIFICATION, false)
                            apply()
                        }

                        if (!hasPermissions(POST_NOTIFICATIONS)) {
                            startActivity(
                                Intent(this, OnboardingMileageActivity::class.java)
                                    .putExtra(EXTRA_PERMISSIONS_ROUTE, NOTIFICATION_SCREEN)
                            )
                        }
                    } else {
                        if (SDK_INT >= TIRAMISU) {
                            sharedPreferences?.edit()?.apply {
                                putBoolean(OPT_OUT_NOTIFICATION, true)
                                putBoolean(ASK_AGAIN_NOTIFICATION, true)
                                apply()
                            }

                            revokeSelfPermissionOnKill(POST_NOTIFICATIONS)

                            ConfirmRestartDialog.newInstance().show(supportFragmentManager, null)
                        }
                    }
                }
                BG_BATTERY_ENABLED -> {
                    val isChecked = sharedPrefs.getBoolean(key, false)
                    if (isChecked) {
                        sharedPrefs.edit()
                            .putBoolean(OPT_OUT_BATTERY_OPTIMIZER, false)
                            .putBoolean(ASK_AGAIN_BATTERY_OPTIMIZER, false)
                            .apply()

                        if (!hasBatteryPermission()) {
                            expectedExit = true
                            startActivity(
                                Intent(this, OnboardingMileageActivity::class.java)
                                    .putExtra(EXTRA_PERMISSIONS_ROUTE, OPTIMIZATION_OFF_SCREEN)
                            )
                        }
                    } else {
                        sharedPreferences?.edit()?.apply {
                            putBoolean(OPT_OUT_BATTERY_OPTIMIZER, true)
                            putBoolean(ASK_AGAIN_BATTERY_OPTIMIZER, true)
                            apply()
                        }

                        if (hasBatteryPermission()) {
                            expectedExit = true
                            startActivity(
                                Intent(this, OnboardingMileageActivity::class.java)
                                    .putExtra(EXTRA_PERMISSIONS_ROUTE, OPTIMIZATION_ON_SCREEN)
                            )
                        }
                    }
                }
                AUTHENTICATION_ENABLED -> {
                    val wasReverted = sharedPrefs.getBoolean(AUTHENTICATION_ENABLED_REVERTED, false)

                    if (!wasReverted) {
                        val isChecked = sharedPrefs.getBoolean(AUTHENTICATION_ENABLED, true)
                        fun revert() {
                            sharedPrefs.edit()
                                .putBoolean(AUTHENTICATION_ENABLED, !isChecked)
                                .putBoolean(AUTHENTICATION_ENABLED_REVERTED, true)
                                .commit()

/*
                             TODO: This is what causes the nasty redraw when biometric
                              authentication fails when authentication enabled preference is
                              changed (attempted) | not sure if I've grown accustomed to it, or if
                              it has actually gotten better, but it doesn't seem -that- nasty to
                              me now
*/
                            val settingsFragment =
                                supportFragmentManager.findFragmentById(R.id.settings) as SettingsFragment?
                            settingsFragment?.setPreferencesFromResource(
                                R.xml.root_preferences,
                                null
                            )

                        }
                        authenticate(
                            onSuccess = { },
                            onError = ::revert,
                            onFailed = ::revert,
                            titleText = if (isChecked) {
                                getString(R.string.bioprompt_title_settings_enable_auth)
                            } else {
                                getString(R.string.bioprompt_title_settings_disable_auth)
                            },
                            descriptionText = if (isChecked) {
                                getString(R.string.bioprompt_desc_settings_enable_auth)
                            } else {
                                getString(R.string.bioprompt_desc_settings_disable_auth)
                            },
                            forceAuthentication = true
                        )
                    } else {
                        sharedPrefs.edit().putBoolean(AUTHENTICATION_ENABLED_REVERTED, false)
                            .apply()
                    }
                }
                UI_MODE_PREF -> {
                    permissionsHelper.setUiModeFromPrefs()
//                    AppCompatDelegate.setDefaultNightMode(permissionsHelper.uiMode)
                }
            }
        }

    companion object {
        internal const val ACTIVITY_RESULT_NEEDS_RESTART =
            "${BuildConfig.APPLICATION_ID}.result_needs_restart"

        internal const val ACTIVITY_RESULT_LOCATION_ENABLED =
            "${BuildConfig.APPLICATION_ID}.location_enabled"

        internal const val REQUEST_KEY_SETTINGS_ACTIVITY_RESULT =
            "${BuildConfig.APPLICATION_ID}.result_settings_fragment"

        internal const val EXTRA_SETTINGS_ACTIVITY_IS_AUTHENTICATED =
            "${BuildConfig.APPLICATION_ID}.extra_settings_activity_is_authenticated"

        internal const val INTENT_EXTRA_PRE_AUTH = "${BuildConfig.APPLICATION_ID}.pre_auth_settings"
    }
}

class DTListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ListPreference(context, attrs) {
    override fun onClick() {
        AlertDialog.Builder(context, R.style.DTAlertDialogStyle)
            .setCustomTitle(View.inflate(context, R.layout.prefs_dialog_title, null))
            .setSingleChoiceItems(entries, entries.indexOf(value)) { dialog, pos ->
                this.value = entries[pos].toString()
                dialog.dismiss()
            }
            .show()
    }
}
