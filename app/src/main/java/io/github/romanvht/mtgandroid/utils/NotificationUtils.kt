package io.github.romanvht.mtgandroid.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import io.github.romanvht.mtgandroid.R
import io.github.romanvht.mtgandroid.data.STOP_ACTION
import io.github.romanvht.mtgandroid.service.MtgProxyService
import io.github.romanvht.mtgandroid.ui.activities.MainActivity

fun registerNotificationChannel(context: Context, id: String, @StringRes name: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(
            id,
            context.getString(name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.enableLights(false)
        channel.enableVibration(false)
        channel.setShowBadge(false)

        manager.createNotificationChannel(channel)
    }
}

fun createServiceNotification(
    context: Context,
    channelId: String,
    ip: String,
    port: Int
): Notification =
    NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setSilent(true)
        .setContentTitle(context.getString(R.string.notification_title))
        .setContentText(context.getString(R.string.notification_text, ip, port))
        .addAction(
            0,
            context.getString(R.string.disconnect),
            PendingIntent.getService(
                context,
                0,
                Intent(context, MtgProxyService::class.java).setAction(STOP_ACTION),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )
        .build()
