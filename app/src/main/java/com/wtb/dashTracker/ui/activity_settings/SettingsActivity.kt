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
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardMileageTrackingScreen
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity.Companion.EXTRA_PERMISSIONS_ROUTE
import com.wtb.dashTracker.ui.activity_main.MainActivity.Companion.ACTIVITY_RESULT_NEEDS_RESTART
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.util.PermissionsHelper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BG_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_NOTIFICATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_NOTIFICATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_SUMMARY_SCREEN
import com.wtb.dashTracker.util.hasPermissions
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val permissionHelper
            get() = PermissionsHelper(requireContext())

        var mileageTrackingEnabledPref: SwitchPreference? = null
        var notificationEnabledPref: SwitchPreference? = null
        var bgBatteryEnabledPref: SwitchPreference? = null

        @SuppressLint("NotifyDataSetChanged")
        val listener: OnSharedPreferenceChangeListener =
            OnSharedPreferenceChangeListener { sharedPreferences, key ->
                Log.d(TAG, "onSharedPrefChangeListener | $key")
                when (key) {
                    getString(R.string.prefs_enable_location) -> {
                        val isChecked = if (sharedPreferences.all.keys.contains(key))
                            sharedPreferences.getBoolean(key, false) else null
                        Log.d(TAG, "location is checked: $isChecked")
                        mileageTrackingEnabledPref?.isChecked = isChecked ?: false


                        if (isChecked == true) {
                            sharedPreferences?.edit()?.apply {
                                putBoolean(requireContext().ASK_AGAIN_LOCATION, false)
                                putBoolean(requireContext().ASK_AGAIN_BG_LOCATION, false)
                                putBoolean(requireContext().ASK_AGAIN_NOTIFICATION, false)
                                putBoolean(requireContext().ASK_AGAIN_BATTERY_OPTIMIZER, false)
                                putBoolean(requireContext().OPT_OUT_LOCATION, false)
                                putBoolean(requireContext().PREF_SHOW_SUMMARY_SCREEN, true)
                                apply()
                            }

                            val intent =
                                Intent(requireContext(), OnboardingMileageActivity::class.java)
                            startActivity(intent)
                        }
                    }
                    getString(R.string.prefs_enable_notification) -> {
                        val isChecked = if (sharedPreferences.all.keys.contains(key))
                            sharedPreferences.getBoolean(key, false) else null

                        if (isChecked == true) {
                            sharedPreferences?.edit()?.apply {
                                putBoolean(requireContext().OPT_OUT_NOTIFICATION, false)
                                putBoolean(requireContext().ASK_AGAIN_NOTIFICATION, false)
                                apply()
                            }

                            if (!requireContext().hasPermissions(POST_NOTIFICATIONS)) {
                                val intent =
                                    Intent(requireContext(), OnboardingMileageActivity::class.java)
                                        .putExtra(
                                            EXTRA_PERMISSIONS_ROUTE,
                                            OnboardMileageTrackingScreen.NotificationScreen
                                        )
                                startActivity(intent)
                            }
                        } else {
                            if (SDK_INT >= TIRAMISU) {
                                requireContext().revokeSelfPermissionOnKill(POST_NOTIFICATIONS)

                                ConfirmationDialog.newInstance(
                                    text = R.string.dialog_restart,
                                    requestKey = ConfirmType.RESTART.key,
                                    posButton = R.string.restart,
                                    negButton = R.string.later
                                ).show(parentFragmentManager, null)
                            }
                        }
                    }
                    getString(R.string.prefs_enable_bg_battery) -> {
                        val isChecked = if (sharedPreferences.all.keys.contains(key))
                            sharedPreferences.getBoolean(key, false) else null
                        Log.d(TAG, "bg battery is checked: $isChecked")
                        bgBatteryEnabledPref?.isChecked = isChecked ?: false

                        if (isChecked == true) {
                            sharedPreferences?.edit()?.apply {
                                putBoolean(requireContext().OPT_OUT_BATTERY_OPTIMIZER, false)
                                putBoolean(requireContext().ASK_AGAIN_BATTERY_OPTIMIZER, false)
                                apply()
                            }

                            if (!permissionHelper.hasBatteryPermission) {
                                val intent =
                                    Intent(requireContext(), OnboardingMileageActivity::class.java)
                                        .putExtra(
                                            EXTRA_PERMISSIONS_ROUTE,
                                            OnboardMileageTrackingScreen.BatteryOptimizationScreen
                                        )
                                startActivity(intent)
                            }
                        } else {
                            val pm =
                                requireContext().getSystemService(POWER_SERVICE) as PowerManager
                            pm.isIgnoringBatteryOptimizations(requireContext().packageName)
                        }
                    }
                }
            }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {

            setFragmentResultListener(
                ConfirmType.RESTART.key,
            ) { _, bundle ->
                val result = bundle.getBoolean(ARG_CONFIRM)
                if (result) {
                    val intent = Intent().apply {
                        putExtra(ACTIVITY_RESULT_NEEDS_RESTART, true)
                    }
                    activity?.apply {
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }
            }

            return super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            Log.d(TAG, "onCreatePreferences | ")

            mileageTrackingEnabledPref = findPreference(getString(R.string.prefs_enable_location))
            notificationEnabledPref = findPreference(getString(R.string.prefs_enable_notification))
            bgBatteryEnabledPref = findPreference(getString(R.string.prefs_enable_bg_battery))

            if (SDK_INT < TIRAMISU) {
                notificationEnabledPref?.isVisible = false
            }
        }

        override fun onResume() {
            super.onResume()

            preferenceScreen.removeAll()
            addPreferencesFromResource(R.xml.root_preferences)

            val prefs = preferenceManager.sharedPreferences
            Log.d(TAG, "onResume | prefs is null: ${prefs == null}")
            prefs?.registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onPause() {
            super.onPause()
            val prefs = preferenceManager.sharedPreferences
            Log.d(TAG, "onPause | prefs is null: ${prefs == null}")
            prefs?.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}