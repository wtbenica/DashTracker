package com.wtb.notificationutil

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.wtb.notificationutil.NotificationUtils.NotificationData
import kotlin.reflect.KFunction1

/**
 * Utility class for creating and updating notifications
 *
 * @property context the parent [Context]
 * @property nd a [NotificationData] object that describes the static content of the [Notification]
 */
class NotificationUtils(private val context: Context, private var nd: NotificationData) {
    /**
     * Creates a [NotificationChannel] and sends
     *
     * @param mainText the text to show in the [Notification]
     * @param channel the [NotificationChannel] for the [Notification]
     * @param bigContentTitle set an alternate label when [Notification] is expanded
     * @return the initialized [Notification]
     */
    fun generateNotification(
        mainText: String,
        channel: NotificationChannel,
        bigContentTitle: Int? = null
    ): Notification {
        notificationManager.createNotificationChannel(channel)

        return getNotificationBuilder(mainText, channel.id).also { builder ->
            bigContentTitle?.let {
                val bigTextStyle = NotificationCompat.BigTextStyle()
                    .bigText(mainText)
                    .setBigContentTitle(context.getString(it))
                builder.setStyle(bigTextStyle)
            }
        }.build()
    }

    /**
     * Updates the text of an existing [Notification]
     *
     * @param mainText the updated text to show in the [Notification]
     * @param notificationId the id of the [Notification] whose text is to be updated
     * @param channel the [NotificationChannel] for the [Notification]
     * @param bigContentTitle set an alternate label when [Notification] is expanded
     */
    fun updateNotification(
        mainText: String, notificationId: Int,
        channel: NotificationChannel,
        bigContentTitle: Int? = null
    ) {
        notificationManager.notify(
            notificationId,
            getNotificationBuilder(mainText, channel.id).also { builder ->
                bigContentTitle?.let {
                    val bigTextStyle = NotificationCompat.BigTextStyle()
                        .bigText(mainText)
                        .setBigContentTitle(context.getString(it))
                    builder.setStyle(bigTextStyle)
                }
            }.build()
        )
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Initializes a [NotificationCompat.Builder] from [nd] [NotificationData]
     *
     * @param mainText the text to show in the notification
     * @param channelId the id of the [NotificationChannel] to use for the notification
     * @return an initialized [NotificationCompat.Builder]
     */
    private fun getNotificationBuilder(
        mainText: String,
        channelId: String,
    ): NotificationCompat.Builder {
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainText)
            .setBigContentTitle(context.getString(nd.bigContentTitle))

        return NotificationCompat.Builder(context, channelId)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setStyle(bigTextStyle)
            .setContentTitle(context.getString(nd.contentTitle))
            .setContentText(mainText)
            .setSmallIcon(nd.icon)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .also {
                nd.actions.forEach { action ->
                    it.addAction(action(context))
                }
            }
    }
    data class NotificationData(
        @StringRes val contentTitle: Int,
        @StringRes val bigContentTitle: Int,
        @DrawableRes val icon: Int,
        val actions: List<KFunction1<Context, NotificationCompat.Action>> = listOf()
    )

    data class NotificationAction(
        @DrawableRes val icon: Int,
        val label: String,
        val pendingIntent: KFunction1<Context, PendingIntent?>
    )

    /**
     * @param nd the new [NotificationData] to update
     */
    fun updateNotificationData(nd: NotificationData) {
        this.nd = nd
    }
}
