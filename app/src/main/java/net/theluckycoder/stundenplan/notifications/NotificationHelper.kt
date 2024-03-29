package net.theluckycoder.stundenplan.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.theluckycoder.stundenplan.R
import net.theluckycoder.stundenplan.ui.AppColors

object NotificationHelper {

    private const val CHANNEL_ID_DEFAULT = "notifications"
    private const val CHANNEL_ID_HIGH_SCHOOL = "high_school"
    private const val CHANNEL_ID_MIDDLE_SCHOOL = "middle_school"

    private val channelIds =
        listOf(CHANNEL_ID_DEFAULT, CHANNEL_ID_HIGH_SCHOOL, CHANNEL_ID_MIDDLE_SCHOOL)

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val manager = NotificationManagerCompat.from(context)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_DEFAULT,
                context.getString(R.string.notifications_channel_default),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_HIGH_SCHOOL,
                context.getString(R.string.notifications_channel_high_school),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID_MIDDLE_SCHOOL,
                context.getString(R.string.notifications_channel_middle_school),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    fun postNotification(
        context: Context,
        title: String,
        text: String,
        pendingIntent: PendingIntent,
        notificationId: Int,
        channelId: String?
    ) {
        val channel = channelId?.takeIf { channelIds.contains(channelId) } ?: CHANNEL_ID_DEFAULT
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channel)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(AppColors.Primary.toArgb())
            .setContentIntent(pendingIntent)
            .setSound(defaultSoundUri)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context)
                .notify(notificationId, notification)
        }
    }
}