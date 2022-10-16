package com.wtb.dashTracker.util

import android.Manifest.permission.*
import android.app.Activity
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
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog
import com.wtb.dashTracker.ui.dialog_confirm.LambdaWrapper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREFS_OPT_OUT_BG_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREFS_OPT_OUT_LOCATION
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
                .putBoolean(PREFS_OPT_OUT_LOCATION, true)
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

class PermissionsHelper(val activity: Activity) {

    internal val sharedPrefs
        get() = activity.getSharedPreferences(DT_SHARED_PREFS, 0)

    internal fun hasPermissions(ctx: Context, vararg permissions: String): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
        }

    internal fun hasBatteryPermission(): Boolean {
        val pm = activity.getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(activity.packageName)
    }

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
     * @param missingBatteryPermission Has [REQUIRED_PERMISSIONS] + []
     * @param missingNotificationPermission SDK_INT >= TIRAMISU and has [ACCESS_BACKGROUND_LOCATION] and
     * [LOCATION_PERMISSIONS]
     * @param missingBgLocationPermission Has [LOCATION_PERMISSIONS]
     * @param missingAllPermissions also the default return value
     * @return the matching parameter
     */
    fun <T : Any> whenPermissions(
        optOutLocation: T? = null,
        hasAllPermissions: T? = null,
        missingBatteryPermission: T? = null,
        missingNotificationPermission: T? = null,
        missingBgLocationPermission: T? = null,
        missingAllPermissions: T? = null
    ): T? {
        val optOut = sharedPrefs.getBoolean(PREFS_OPT_OUT_LOCATION, false)

        val askAgainBattery = sharedPrefs.getBoolean(PREFS_ASK_AGAIN_BATTERY_OPTIMIZER, false)

        val hasDecidedBattery = hasBatteryPermission()
                || sharedPrefs.getBoolean(PREFS_OPT_OUT_BATTERY_OPTIMIZER, false)
                || askAgainBattery

        val askAgainNotification =
            sharedPrefs.getBoolean(PREFS_ASK_AGAIN_NOTIFICATION, false)

        val hasDecidedNotifs = hasPermissions(activity, *OPTIONAL_PERMISSIONS)
                || sharedPrefs.getBoolean(PREFS_OPT_OUT_NOTIFICATION, false)
                || askAgainNotification

        val hasAll = hasDecidedBattery
                && hasDecidedNotifs
                && hasPermissions(activity, *REQUIRED_PERMISSIONS)

        val missingBattery = hasDecidedNotifs && hasPermissions(activity, *REQUIRED_PERMISSIONS)

        val missingNotification = hasPermissions(activity, *REQUIRED_PERMISSIONS)

        val missingBg = hasPermissions(activity, *LOCATION_PERMISSIONS)

        Log.d(TAG, "whenPermissions | askAgainBattery: $askAgainBattery | hasDecidedBattery: " +
                "$hasDecidedBattery")
        return when {
            optOut -> {
                Log.d(TAG, "whenPermissions | optOut")
                optOutLocation
            }
            hasAll -> {
                Log.d(TAG, "whenPermissions | hasAll")
                hasAllPermissions
            }
            missingBattery -> {
                Log.d(TAG, "whenPermissions | missingBattery")
                missingBatteryPermission
            }
            missingNotification -> {
                Log.d(TAG, "whenPermissions | missingNotification")
                missingNotificationPermission
            }
            missingBg -> {
                Log.d(TAG, "whenPermissions | missingBg")
                missingBgLocationPermission
            }
            else -> {
                Log.d(TAG, "whenPermissions | else")
                missingAllPermissions
            }
        }
    }

    internal fun getPermissionsState(): PermissionsState? =
        whenPermissions(
            optOutLocation = OPT_OUT,
            hasAllPermissions = ALL_GRANTED_OR_DECIDED,
            missingBatteryPermission = MISSING_BATTERY,
            missingNotificationPermission = MISSING_NOTIFICATION,
            missingBgLocationPermission = MISSING_BG_LOCATION,
            missingAllPermissions = MISSING_LOCATION
        )

    companion object {
        internal const val DT_SHARED_PREFS = "dashtracker_prefs"
        internal const val PREFS_OPT_OUT_LOCATION = "Don't ask | location"
        internal const val PREFS_OPT_OUT_BG_LOCATION = "Don't ask | bg location"
        internal const val PREFS_OPT_OUT_NOTIFICATION = "Don't ask | notifications"
        internal const val PREFS_ASK_AGAIN_NOTIFICATION = "Decide later | notification"
        internal const val PREFS_OPT_OUT_BATTERY_OPTIMIZER = "Don't ask | battery optimization"
        internal const val PREFS_ASK_AGAIN_BATTERY_OPTIMIZER = "Decide later | battery optimization"
        internal const val PREFS_SHOULD_SHOW_INTRO = "Run Intro"

        internal enum class PermissionsState(val value: Byte) {
            OPT_OUT(1), ALL_GRANTED_OR_DECIDED(2), MISSING_BATTERY(4), MISSING_NOTIFICATION(8),
            MISSING_BG_LOCATION(16), MISSING_LOCATION(32)
        }
    }
}
