package com.wtb.dashTracker.util

import android.Manifest.permission.*
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
import com.wtb.dashTracker.ui.activity_main.MainActivity.Companion.PREFS_DONT_ASK_BG_LOCATION
import com.wtb.dashTracker.ui.activity_main.MainActivity.Companion.PREFS_OPT_OUT_LOCATION
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog
import com.wtb.dashTracker.ui.dialog_confirm.LambdaWrapper
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
 * Required Permissions: [ACCESS_BACKGROUND_LOCATION], [LOCATION_PERMISSIONS], and, if SDK >=
 * TIRAMISU, [POST_NOTIFICATIONS]
 */
internal val REQUIRED_PERMISSIONS =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            ACCESS_BACKGROUND_LOCATION,
            POST_NOTIFICATIONS,
            *LOCATION_PERMISSIONS,
        )
    } else {
        arrayOf(
            ACCESS_BACKGROUND_LOCATION,
            *LOCATION_PERMISSIONS,
        )
    }

internal fun hasPermissions(context: Context, vararg permissions: String): Boolean =
    permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

internal fun AppCompatActivity.hasBatteryPermission(): Boolean {
    val pm = getSystemService(POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(packageName)
}

/**
 *  wraps a when expression that checks for missing permissions in this order:
 *  none > battery > notification > bg location > location & activity > all
 *
 * @param T return type
 * @param hasAllPermissions Has [REQUIRED_PERMISSIONS] + [AppCompatActivity.hasBatteryPermission]
 * @param missingBatteryPerm Has [REQUIRED_PERMISSIONS]
 * @param missingNotificationPerm SDK_INT >= TIRAMISU and has [ACCESS_BACKGROUND_LOCATION] and
 * [LOCATION_PERMISSIONS]
 * @param missingBgLocation Has [LOCATION_PERMISSIONS]
 * @param hasNoPermissions also the default return value
 * @return the matching parameter
 */
fun <T : Any> AppCompatActivity.whenPermissions(
    hasAllPermissions: T?,
    missingBatteryPerm: T?,
    missingNotificationPerm: T?,
    missingBgLocation: T?,
    hasNoPermissions: T?
): T? = when {
    hasBatteryPermission() && hasPermissions(this, *REQUIRED_PERMISSIONS) -> {
        hasAllPermissions
    }
    hasPermissions(this, *REQUIRED_PERMISSIONS) -> {
        missingBatteryPerm
    }
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            hasPermissions(this, *LOCATION_PERMISSIONS, ACCESS_BACKGROUND_LOCATION) -> {
        missingNotificationPerm
    }
    hasPermissions(this, *LOCATION_PERMISSIONS) -> {
        missingBgLocation
    }
    else -> {
        hasNoPermissions
    }
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
    onGranted: () -> Unit,
    onNotGranted: (() -> Unit)? = null
): ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        it?.let { permissionMap ->
            val permissionGranted = permissionMap.toList().let { permissions ->
                permissions.foldRight(true) { p, b ->
                    p.second && b
                }
            }
            if (permissionGranted) {
                Log.d(TAG, "I can haz permission?")
                onGranted()
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


@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@OptIn(ExperimentalCoroutinesApi::class)
internal fun MainActivity.showRationaleLocation(onGranted: () -> Unit) {
    ConfirmationDialog.newInstance(
        text = R.string.dialog_location_permission,
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

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@OptIn(ExperimentalCoroutinesApi::class)
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
                .putBoolean(PREFS_DONT_ASK_BG_LOCATION, true)
                .apply()
        }
    ).show(supportFragmentManager, null)
}