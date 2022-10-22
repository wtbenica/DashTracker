package com.wtb.dashTracker.util

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog
import com.wtb.dashTracker.ui.dialog_confirm.LambdaWrapper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.LOCATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREFS_OPT_OUT_BG_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PermissionsState.*
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(POST_NOTIFICATIONS)
    } else {
        arrayOf()
    }

/**
 * All Permissions [REQUIRED_PERMISSIONS] and [OPTIONAL_PERMISSIONS]
 */
internal val ALL_PERMISSIONS: Array<String> = arrayOf(
    *OPTIONAL_PERMISSIONS,
    *REQUIRED_PERMISSIONS
)

internal fun hasPermissions(context: Context, vararg permissions: String): Boolean =
    permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

internal fun hasBatteryPermission(context: Context): Boolean {
    val pm = context.getSystemService(POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

internal fun AppCompatActivity.hasBatteryPermission(): Boolean {
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
internal fun AppCompatActivity.registerMultiplePermissionsLauncher(
    onGranted: (() -> Unit)? = null,
    onNotGranted: (() -> Unit)? = null
): ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        it?.let { permissionMap ->
            val permissionGranted = permissionMap.toList().all { it.second }
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
internal fun AppCompatActivity.registerSinglePermissionLauncher(onGranted: (() -> Unit)? = null):
        ActivityResultLauncher<String> =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            onGranted?.invoke()
        }
    }


@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
internal fun MainActivity.showRationaleLocation(onGranted: () -> Unit) {
    ConfirmationDialog.newInstance(
        text = R.string.dialog_location_permission_1,
        requestKey = "Thrust",
        title = "Allow location access",
        posButton = R.string.allow,
        posAction = LambdaWrapper { onGranted() },
        negButton = R.string.deny,
        negAction = LambdaWrapper { },
        posButton2 = R.string.dont_ask,
        posAction2 = LambdaWrapper {
            this.sharedPrefs
                .edit()
                .putBoolean(LOCATION_ENABLED, false)
                .apply()
        }
    ).show(supportFragmentManager, null)
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
internal fun MainActivity.showRationaleBgLocation(onGranted: () -> Unit) {
    ConfirmationDialog.newInstance(
        text = R.string.dialog_bg_location_text,
        requestKey = "Bob",
        title = "Allow background location",
        posButton = R.string.allow,
        posAction = LambdaWrapper { onGranted() },
        negButton = R.string.deny,
        negAction = LambdaWrapper { },
        posButton2 = R.string.dont_ask,
        posAction2 = LambdaWrapper {
            this.sharedPrefs
                .edit()
                .putBoolean(PREFS_OPT_OUT_BG_LOCATION, true)
                .apply()
        }
    ).show(supportFragmentManager, null)
}

class PermissionsHelper(val activity: Context) {

    internal val sharedPrefs
        get() = PreferenceManager.getDefaultSharedPreferences(activity)

    internal fun hasPermissions(ctx: Context, vararg permissions: String): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
        }

    internal fun hasBatteryPermission(): Boolean {
        val pm = activity.getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(activity.packageName)
    }

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
    fun <T : Any> whenPermissions(
        optOutLocation: T? = null,
        hasAllPermissions: T? = null,
        hasNotification: T? = null,
        hasBgLocation: T? = null,
        hasLocation: T? = null,
        noPermissions: T? = null
    ): T? {
        val optOut = !sharedPrefs.getBoolean(activity.LOCATION_ENABLED, true)
                || sharedPrefs.getBoolean(activity.OPT_OUT_LOCATION, false)

        val askAgainLocation =
            sharedPrefs.getBoolean(activity.ASK_AGAIN_LOCATION, false)

        val hasDecidedLocation = hasPermissions(activity, *LOCATION_PERMISSIONS)
                || sharedPrefs.getBoolean(activity.OPT_OUT_LOCATION, false)
                || askAgainLocation

        val askAgainBgLocation =
            sharedPrefs.getBoolean(activity.ASK_AGAIN_BG_LOCATION, false)

        val hasDecidedBgLocation = (hasPermissions(activity, *REQUIRED_PERMISSIONS) && hasDecidedLocation)
                || sharedPrefs.getBoolean(activity.OPT_OUT_LOCATION, false)
                || askAgainBgLocation

        val askAgainNotification =
            sharedPrefs.getBoolean(activity.ASK_AGAIN_NOTIFICATION, false)

        val hasDecidedNotifs =
            (hasPermissions(activity, *OPTIONAL_PERMISSIONS) && hasDecidedBgLocation)
                || sharedPrefs.getBoolean(activity.OPT_OUT_NOTIFICATION, false)
                || askAgainNotification

        val askAgainBattery = sharedPrefs.getBoolean(activity.ASK_AGAIN_BATTERY_OPTIMIZER, false)

        val hasDecidedBattery = (hasBatteryPermission() && hasDecidedNotifs)
                || sharedPrefs.getBoolean(activity.OPT_OUT_BATTERY_OPTIMIZER, false)
                || askAgainBattery

        val hasAll = hasDecidedBattery
                && hasDecidedNotifs
                && hasDecidedBgLocation
                && hasDecidedLocation

        Log.d(
            TAG, "whenPermissions | askAgainBattery: $askAgainBattery | hasDecidedBattery: " +
                    "$hasDecidedBattery"
        )
        return when {
            optOut -> {
                Log.d(TAG, "whenPermissions | optOut")
                optOutLocation
            }
            hasAll -> {
                Log.d(TAG, "whenPermissions | hasAll")
                hasAllPermissions
            }
            hasDecidedNotifs -> {
                Log.d(TAG, "whenPermissions | missingBattery")
                hasNotification
            }
            hasDecidedBgLocation -> {
                Log.d(TAG, "whenPermissions | missingNotification")
                hasBgLocation
            }
            hasDecidedLocation -> {
                Log.d(TAG, "whenPermissions | missingBg")
                hasLocation
            }
            else -> {
                Log.d(TAG, "whenPermissions | else")
                noPermissions
            }
        }
    }

    companion object {
        internal val Context.LOCATION_ENABLED
            get() = getString(R.string.prefs_enable_location)

        internal val Context.NOTIFICATION_ENABLED
            get() = getString(R.string.prefs_enable_notification)

        internal val Context.BG_BATTERY_ENABLED
            get() = getString(R.string.prefs_enable_bg_battery)

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

        internal const val PREFS_OPT_OUT_BG_LOCATION = "Don't ask | bg location"

        internal const val PREFS_SHOULD_SHOW_INTRO = "Run Intro"

        internal enum class PermissionsState(val value: Byte) {
            OPT_OUT(1), ALL_GRANTED_OR_DECIDED(2), MISSING_BATTERY(4), MISSING_NOTIFICATION(8),
            MISSING_BG_LOCATION(16), MISSING_LOCATION(32)
        }
    }
}
