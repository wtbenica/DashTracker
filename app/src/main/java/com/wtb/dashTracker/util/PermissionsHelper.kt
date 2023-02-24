package com.wtb.dashTracker.util

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.wtb.dashTracker.R
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
                onGranted?.invoke()
            } else {
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

    internal val uiModeIsDarkMode: Boolean
        get() {
            fun systemDarkMode(): Boolean =
                (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            return when (uiMode) {
                UiMode.DARK -> true
                UiMode.LIGHT -> false
                else -> systemDarkMode()
            }
        }

    internal val uiMode: UiMode
        get() = uiModeByDisplayName(
            sharedPrefs.getString(
                context.UI_MODE_PREF,
                context.getString(UiMode.SYSTEM.displayName)
            ) ?: context.getString(UiMode.SYSTEM.displayName)
        )

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

    enum class UiMode(@StringRes val displayName: Int, val mode: Int) {
        SYSTEM(
            R.string.pref_theme_option_use_device_theme,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ),
        LIGHT(R.string.pref_theme_option_light, AppCompatDelegate.MODE_NIGHT_NO),
        DARK(R.string.pref_theme_option_dark, AppCompatDelegate.MODE_NIGHT_YES);
    }

    fun uiModeByDisplayName(mode: String): UiMode {
        return UiMode.values().firstOrNull { context.getString(it.displayName) == mode }
            ?: UiMode.SYSTEM
    }

    fun uiModeByMode(mode: Int): UiMode {
        return UiMode.values().firstOrNull { it.mode == mode } ?: UiMode.SYSTEM
    }

    fun getUiModeDisplayName(): String {
        return sharedPrefs.getString(
            context.UI_MODE_PREF,
            context.getString(UiMode.SYSTEM.displayName)
        ) ?: context.getString(UiMode.SYSTEM.displayName)
    }

    fun updateUiMode(uiMode: UiMode, onPrefSet: (() -> Unit)? = null) {
        @Suppress("ApplySharedPref")
        sharedPrefs?.apply {
            edit()
                .putString(
                    context.UI_MODE_PREF,
                    context.getString(uiMode.displayName)
                )
                .commit()

            onPrefSet?.invoke()

            AppCompatDelegate.setDefaultNightMode(uiMode.mode)
        }
    }

    fun setUiModeFromPrefs() {
        val displayName = sharedPrefs.getString(
            context.UI_MODE_PREF,
            context.getString(UiMode.SYSTEM.displayName)
        ) ?: context.getString(UiMode.SYSTEM.displayName)

        val mode = uiModeByDisplayName(displayName).mode

        AppCompatDelegate.setDefaultNightMode(mode)

    }

    /**
     *  wraps a when expression that checks for missing permissions in this order:
     *  none > battery > notification > bg location > location & activity > all
     *
     * @param T return type
     * @param optOutLocation Has opted out of mileage tracking
     * @param hasAllPermissions Has [REQUIRED_PERMISSIONS] + [AppCompatActivity.hasBatteryPermission]
     * + [OPTIONAL_PERMISSIONS] or has opted out of optional permissions
     * @param hasNotification Has [REQUIRED_PERMISSIONS] + []
     * @param hasBgLocation SDK_INT >= TIRAMISU and has [ACCESS_BACKGROUND_LOCATION] and
     * [LOCATION_PERMISSIONS]
     * @param hasLocation Has [LOCATION_PERMISSIONS]
     * @param noPermissions also the default return value
     * @return the matching parameter
     */
    internal fun <T : Any> whenHasDecided(
        optOutLocation: T? = null,
        hasAllPermissions: T? = null,
        hasNotification: T? = null,
        hasBgLocation: T? = null,
        hasLocation: T? = null,
        noPermissions: T? = null
    ): T? {
        val optOut = !sharedPrefs.getBoolean(context.ASK_AGAIN_LOCATION, true)
                && sharedPrefs.getBoolean(context.OPT_OUT_LOCATION, false)

        val askAgainLocation =
            sharedPrefs.getBoolean(context.ASK_AGAIN_LOCATION, false)

        val hasDecidedLocation = context.hasPermissions(*LOCATION_PERMISSIONS)
                || sharedPrefs.getBoolean(context.OPT_OUT_LOCATION, false)
                || askAgainLocation

        val askAgainBgLocation =
            sharedPrefs.getBoolean(context.ASK_AGAIN_BG_LOCATION, false)

        val hasDecidedBgLocation =
            (context.hasPermissions(*REQUIRED_PERMISSIONS) && hasDecidedLocation)
                    || sharedPrefs.getBoolean(context.OPT_OUT_LOCATION, false)
                    || askAgainBgLocation

        val askAgainNotification =
            sharedPrefs.getBoolean(context.ASK_AGAIN_NOTIFICATION, false)

        val hasDecidedNotifs =
            (context.hasPermissions(*OPTIONAL_PERMISSIONS) && hasDecidedBgLocation)
                    || sharedPrefs.getBoolean(context.OPT_OUT_NOTIFICATION, false)
                    || askAgainNotification

        val askAgainBattery = sharedPrefs.getBoolean(context.ASK_AGAIN_BATTERY_OPTIMIZER, false)

        val hasDecidedBattery = (context.hasBatteryPermission() && hasDecidedNotifs)
                || sharedPrefs.getBoolean(context.OPT_OUT_BATTERY_OPTIMIZER, false)
                || askAgainBattery

        val hasAll = hasDecidedBattery
                && hasDecidedNotifs
                && hasDecidedBgLocation
                && hasDecidedLocation

        return when {
            optOut -> {
                optOutLocation
            }
            hasAll -> {
                hasAllPermissions
            }
            hasDecidedNotifs -> {
                hasNotification
            }
            hasDecidedBgLocation -> {
                hasBgLocation
            }
            hasDecidedLocation -> {
                hasLocation
            }
            else -> {
                noPermissions
            }
        }
    }

    companion object {
        // Some of these don't need to be stored as string resources. The reason that some
        // are is that they are keys in root_preferences, where strings are required
        // The same method is used for all prefs for consistency only.
        internal val Context.LOCATION_ENABLED
            get() = getString(R.string.prefs_enable_location)

        internal val Context.NOTIFICATION_ENABLED
            get() = getString(R.string.prefs_enable_notification)

        internal val Context.BG_BATTERY_ENABLED
            get() = getString(R.string.prefs_enable_bg_battery)

        internal val Context.AUTHENTICATION_ENABLED
            get() = getString(R.string.prefs_authentication_enabled)

        internal val Context.UI_MODE_PREF
            get() = getString(R.string.prefs_ui_mode)

        internal val Context.PREF_SHOW_BASE_PAY_ADJUSTS
            get() = getString(R.string.prefs_show_base_pay_adjusts)

        /**
         * If enabling/disabling authentication fails, this prevents re-authentication when the
         * setting is reverted
         */
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

        internal val Context.PREF_SKIP_WELCOME_SCREEN
            get() = getString(R.string.prefs_skip_welcome_screen)

        internal val Context.PREF_SHOW_ONBOARD_INTRO
            get() = getString(R.string.prefs_show_onboard_intro)

        internal val Context.PREF_SHOW_SUMMARY_SCREEN
            get() = getString(R.string.prefs_show_summary_screen)

    }
}
