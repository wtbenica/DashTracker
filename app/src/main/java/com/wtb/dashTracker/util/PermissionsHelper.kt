package com.wtb.dashTracker.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog
import com.wtb.dashTracker.ui.dialog_confirm.LambdaWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi

internal val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACTIVITY_RECOGNITION
)

internal val LOCATION_PERMISSIONS =
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION
    )

internal fun hasPermissions(context: Context, vararg permissions: String): Boolean =
    permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
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
) =
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
internal fun AppCompatActivity.registerSinglePermissionLauncher(onGranted: () -> Unit) =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            onGranted()
        }
    }

// TODO: Update to use ConfirmatonDialog
internal fun AppCompatActivity.showRationaleLocation(onGranted: () -> Unit) {
    AlertDialog.Builder(this as Context)
        .setTitle("Grant Location Access Permissions")
        .setMessage(
            "LocationModule uses location data to automatically track your " +
                    "mileage. We do not share this data with anyone, nor do we " +
                    "access or store it.\nIf you choose not to grant location " +
                    "access, you can still track your mileage by manually " +
                    "entering your starting end ending odometer readings."
        )
        .setPositiveButton("Yes") { dialog, which ->
            onGranted()
        }
        .setNeutralButton("Don't ask again") { dialog, which ->
            dialog.dismiss()
            // TODO: Save to prefs
        }
        .setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }.show()
}

internal fun AppCompatActivity.showRationaleBgLocation(onGranted: () -> Unit) {
    ConfirmationDialog.newInstance(
        text = R.string.dialog_bg_location_text, requestKey = "Bob",
        posButton = R.string.yes,
        posAction = LambdaWrapper { onGranted() },
        negButton = R.string.no,
        negAction = LambdaWrapper { },
        posButton2 = R.string.dont_ask,
        posAction2 = LambdaWrapper { }
    ).show(supportFragmentManager, null)
}