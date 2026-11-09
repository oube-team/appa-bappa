package studio.oube.appabappa.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import studio.oube.appabappa.R

internal object BusyNotification {
    const val ID = 1
    private const val CHANNEL_ID = "busy"

    fun create(context: Context): Notification {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_appa_bappa)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_text))
            .setOngoing(true)
            .build()
    }
}
