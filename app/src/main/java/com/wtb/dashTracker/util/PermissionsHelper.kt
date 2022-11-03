package com.wtb.dashTracker.util

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_main.TAG
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Location Permissions: [ACCESS_FINE_LOCATION], [ACCESS_COARSE_LOCATION], [ACTIVITY_RECOGNITION]
 */
internal val LOCATION_PERMISSIONS =
    arrayOf(
        ACCESS_FINE_LOCATION,
        ACCESS_COARSE_LOCATION,
        ACTIVITY_RECOGNITION
    )

/**
 * Required Permissions: [ACCESS_BACKGROUND_LOCATION], [LOCATION_PERMISSIONS]
 */
internal val REQUIRED_PERMISSIONS =
    arrayOf(
        ACCESS_BACKGROUND_LOCATION,
        *LOCATION_PERMISSIONS,
    )

/**
 * Optional Permissions If Android version >= TIRAMISU/33, [POST_NOTIFICATIONS]
 */
internal val OPTIONAL_PERMISSIONS: Array<String> =
    if (SDK_INT >= TIRAMISU) {
        arrayOf(POST_NOTIFICATIONS)
    } else {
        arrayOf()
    }

internal fun Context.hasPermissions(vararg permissions: String): Boolean =
    permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

internal fun Context.hasBatteryPermission(): Boolean {
    val pm = getSystemService(POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(packageName)
}

/**
 * Registers the [AppCompatActivity] for a request to start an Activity for result that requests
 * multiple permissions
 *
 * @param onGranted the function to call if permissions are granted
 * @return an [ActivityResultLauncher]
 */
@ExperimentalCoroutinesApi
internal fun ComponentActivity.registerMultiplePermissionsLauncher(
    onGranted: (() -> Unit)? = null,
    onNotGranted: (() -> Unit)? = null
): ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        it?.let { permissionMap ->
            val permissionGranted = permissionMap.toList().all { p -> p.second }
            if (permissionGranted) {
                Log.d(TAG, "I can haz permission?")
                onGranted?.invoke()
            } else {
                Log.d(TAG, "Missing permission")
                onNotGranted?.invoke()
            }
        }
    }

/**
 * Registers the [AppCompatActivity] for a request to start an Activity for result that requests a
 * single permission
 *
 * @param onGranted the function to call if permission is granted
 * @return an [ActivityResultLauncher]
 */
internal fun ComponentActivity.registerSinglePermissionLauncher(onGranted: (() -> Unit)? = null):
        ActivityResultLauncher<String> =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            onGranted?.invoke()
        }
    }


class PermissionsHelper(val context: Context) {

    internal val sharedPrefs
        get() = PreferenceManager.getDefaultSharedPreferences(context)

    internal val hasBatteryPermission: Boolean
        get() {
            val pm = context.getSystemService(POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }

    internal val locationEnabled
        get() = context.hasPermissions(*REQUIRED_PERMISSIONS)
                && sharedPrefs.getBoolean(context.LOCATION_ENABLED, false)
                && !sharedPrefs.getBoolean(context.OPT_OUT_LOCATION, true)

    internal val fgLocationEnabled
        get() = context.hasPermissions(ACCESS_FINE_LOCATION)
                && sharedPrefs.getBoolean(context.LOCATION_ENABLED, false)
                && !sharedPrefs.getBoolean(context.OPT_OUT_LOCATION, true)

    internal val bgLocationEnabled
        get() = context.hasPermissions(ACCESS_BACKGROUND_LOCATION)
                && sharedPrefs.getBoolean(context.LOCATION_ENABLED, false)
                && !sharedPrefs.getBoolean(context.OPT_OUT_LOCATION, true)

    internal val notificationsEnabled
        get() = ((SDK_INT < TIRAMISU) || context.hasPermissions(POST_NOTIFICATIONS))
                && sharedPrefs.getBoolean(context.NOTIFICATION_ENABLED, false)
                && !sharedPrefs.getBoolean(context.OPT_OUT_NOTIFICATION, true)

    internal val batteryOptimizationDisabled
        get() = hasBatteryPermission && sharedPrefs.getBoolean(context.LOCATION_ENABLED, false)
                && !sharedPrefs.getBoolean(context.OPT_OUT_LOCATION, true)

    @SuppressLint("ApplySharedPref")
    fun setBooleanPref(
        prefKey: String,
        prefValue: Boolean,
        onPrefSet: (() -> Unit)? = null
    ) {
        sharedPrefs?.edit()?.putBoolean(
            prefKey,
            prefValue
        )?.commit()
        onPrefSet?.invoke()
    }

    companion object {
        internal val Context.LOCATION_ENABLED
            get() = getString(R.string.prefs_enable_location)

        internal val Context.NOTIFICATION_ENABLED
            get() = getString(R.string.prefs_enable_notification)

        internal val Context.BG_BATTERY_ENABLED
            get() = getString(R.string.prefs_enable_bg_battery)

        internal val Context.AUTHENTICATION_ENABLED
            get() = getString(R.string.prefs_authentication_enabled)

        internal val Context.AUTHENTICATION_ENABLED_REVERTED
            get() = getString(R.string.prefs_authentication_enabled_reverted)

        internal val Context.OPT_OUT_LOCATION
            get() = getString(R.string.prefs_opt_out_location)

        internal val Context.OPT_OUT_NOTIFICATION
            get() = getString(R.string.prefs_opt_out_notification)

        internal val Context.OPT_OUT_BATTERY_OPTIMIZER
            get() = getString(R.string.prefs_opt_out_battery_optimizer)

        internal val Context.ASK_AGAIN_LOCATION
            get() = getString(R.string.prefs_ask_again_location)

        internal val Context.ASK_AGAIN_BG_LOCATION
            get() = getString(R.string.prefs_ask_again_bg_location)

        internal val Context.ASK_AGAIN_NOTIFICATION
            get() = getString(R.string.prefs_ask_again_notification)

        internal val Context.ASK_AGAIN_BATTERY_OPTIMIZER
            get() = getString(R.string.prefs_ask_again_battery_optimizer)

        internal val Context.PREF_SHOW_ONBOARD_INTRO
            get() = getString(R.string.prefs_show_onboard_intro)

        internal val Context.PREF_SHOW_SUMMARY_SCREEN
            get() = getString(R.string.prefs_show_summary_screen)

    }
}
