package dev.benica.mileagetracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.wtb.notificationUtil.BuildConfig
import dev.benica.mileagetracker.ActivityUpdateReceiver.Companion.getUpdatesPendingIntent

//internal const val REQUEST_CODE = 1234
internal const val UPDATE_REQ_CODE = 2345

class ActivityUpdateUtils(private val context: Context) {
    internal fun registerForActivityUpdates() {
        val task = if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw Exception("Missing permission 'android.permission.ACTIVITY_RECOGNITION'")
        } else {
            ActivityRecognition.getClient(context)
                .requestActivityUpdates(1000L, getUpdatesPendingIntent(context))
        }

        task.run {
            addOnSuccessListener {
                this@ActivityUpdateUtils.debugLog("Request activity updates success")
            }
            addOnFailureListener {
                this@ActivityUpdateUtils.debugLog("Request activity updates failed")
            }
        }
    }

    internal fun removeActivityUpdates() {
        val task = if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw Exception("Missing permission 'android.permission.ACTIVITY_RECOGNITION'")
        } else {
            ActivityRecognition.getClient(context)
                .removeActivityUpdates(getUpdatesPendingIntent(context))
        }

        task.run {
            addOnSuccessListener {
                this@ActivityUpdateUtils.debugLog("Remove activity updates success")
            }
            addOnFailureListener {
                this@ActivityUpdateUtils.debugLog("Remove activity updates failed")
            }
        }
    }
}

class ActivityUpdateReceiver : BroadcastReceiver() {
    var action: ((event: ActivityRecognitionResult) -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)!!

            action?.invoke(result)
        }
    }

    companion object {
        internal const val ACT_UPDATE_INTENT = "${BuildConfig.LIBRARY_PACKAGE_NAME}.act_update_intent"

        @SuppressLint("UnspecifiedImmutableFlag")
        fun getUpdatesPendingIntent(context: Context): PendingIntent {
            val intent = Intent(ACT_UPDATE_INTENT)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    context,
                    UPDATE_REQ_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    context,
                    UPDATE_REQ_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        }
    }
}