package com.wtb.dashTracker.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

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

internal fun Fragment.registerMultiplePermissionsLauncher(onGranted: () -> Unit) =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        it?.let { permissionMap ->
            val permissionGranted = permissionMap.toList().let { permissions ->
                permissions.foldRight(true) { p, b ->
                    p.second && b
                }
            }
            if (permissionGranted) {
                onGranted()
            }
        }
    }

internal fun Fragment.registerSinglePermissionLauncher(onGranted: () -> Unit) =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            onGranted()
        }
    }

internal fun Fragment.showRationaleLocation(onGranted: () -> Unit) {
    AlertDialog.Builder(activity as Context)
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

internal fun Fragment.showRationaleBgLocation(onGranted: () -> Unit) {
    AlertDialog.Builder(activity as Context)
        .setTitle("Grant Location Access Permissions")
        .setMessage(
            "LocationModule uses background location so we always know where you are."
        )
        .setPositiveButton("Yes") { dialog, which ->
            onGranted()
        }
        .setNeutralButton("Don't ask again") { dialog, which ->
            dialog.dismiss()
        }
        .setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }.show()
}