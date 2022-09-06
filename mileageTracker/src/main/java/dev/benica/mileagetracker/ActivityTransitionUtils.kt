package dev.benica.mileagetracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.wtb.notificationUtil.BuildConfig
import dev.benica.mileagetracker.ActivityTransitionReceiver.Companion.getPendingIntent
import dev.benica.mileagetracker.ActivityUpdateReceiver.Companion.getUpdatesPendingIntent

const val REQUEST_CODE = 1234
const val UPDATE_REQ_CODE = 2345

class ActivityTransitionUtils(private val context: Context) {
    private val requestedTransitions: MutableList<ActivityTransition>
        get() = mutableListOf(
            ActivityTransition.Builder()
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .setActivityType(DetectedActivity.STILL)
                .build(),
            ActivityTransition.Builder()
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .setActivityType(DetectedActivity.STILL)
                .build(),
            ActivityTransition.Builder()
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .build(),
            ActivityTransition.Builder()
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .build(),
            ActivityTransition.Builder()
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .setActivityType(DetectedActivity.ON_FOOT)
                .build(),
            ActivityTransition.Builder()
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .setActivityType(DetectedActivity.ON_FOOT)
                .build()
        )

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
                Log.d(TAG, "Request activity updates success")
            }
            addOnFailureListener {
                Log.d(TAG, "Request activity updates failed")
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
                Log.d(TAG, "Remove activity updates success")
            }
            addOnFailureListener {
                Log.d(TAG, "Remove activity updates failed")
            }
        }
    }

    internal fun registerForActivityTransitionUpdates() {
        val request = ActivityTransitionRequest(requestedTransitions)

        val task = if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw Exception("Missing permission 'android.permission.ACTIVITY_RECOGNITION'")
        } else {
            ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(request, getPendingIntent(context))
        }

        task.run {
            addOnSuccessListener {
                Log.d(TAG, "Request transition updates success")
            }
            addOnFailureListener {
                Log.d(TAG, "Request transition updates failed")
            }
        }
    }

    internal fun removeActivityTransitionUpdates() {
        val task = if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw Exception("Missing permission 'android.permission.ACTIVITY_RECOGNITION'")
        } else {
            ActivityRecognition.getClient(context)
                .removeActivityTransitionUpdates(getPendingIntent(context))
        }

        task.run {
            addOnSuccessListener {
                Log.d(TAG, "Remove transition updates success")
            }
            addOnFailureListener {
                Log.d(TAG, "Remove transition updates failed")
            }
        }
    }
}

class ActivityUpdateReceiver : BroadcastReceiver() {
    var action: ((event: ActivityRecognitionResult) -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "ActivityUpdate received")
        if (ActivityRecognitionResult.hasResult(intent)) {
            Log.d(TAG, "And it has a result")
            val result = ActivityRecognitionResult.extractResult(intent)!!

            action?.invoke(result)
        }
    }

    companion object {
        const val ACT_UPDATE_INTENT = "${BuildConfig.LIBRARY_PACKAGE_NAME}.act_update_intent"

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

class ActivityTransitionReceiver : BroadcastReceiver() {
    var action: ((event: ActivityTransitionEvent) -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!
            for (event: ActivityTransitionEvent in result.transitionEvents) {
                action?.invoke(event)
            }
        }
    }

    companion object {
        private const val ACT_TRANS_INTENT = "LocationLibrary.act_trans_intent"

        @SuppressLint("UnspecifiedImmutableFlag")
        fun getPendingIntent(context: Context): PendingIntent {
            val intent = Intent(ACT_TRANS_INTENT)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        }
    }
}
