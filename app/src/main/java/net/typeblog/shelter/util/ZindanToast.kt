package net.typeblog.shelter.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import net.typeblog.shelter.R

object ZindanToast {
    const val DISPLAY_MS = 6000

    fun show(context: Context, text: CharSequence, durationMs: Int = DISPLAY_MS) {
        val app = context.applicationContext
        val displayMs = normalizeDuration(durationMs)
        if (!canPostNotifications(app)) {
            Toast.makeText(app, text, Toast.LENGTH_LONG).show()
            return
        }

        try {
            val manager = app.getSystemService(NotificationManager::class.java)
            ensureMessageChannel(app, manager)
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(app, MESSAGE_CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(app).setPriority(Notification.PRIORITY_MAX)
            }
            builder
                .setContentTitle(app.getString(R.string.app_name))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_notification_zindan_24dp)
                .setCategory(Notification.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setShowWhen(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setTimeoutAfter(displayMs.toLong())
            }
            manager.notify(MESSAGE_NOTIFICATION_ID, builder.build())
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Handler(Looper.getMainLooper()).postDelayed(
                    { manager.cancel(MESSAGE_NOTIFICATION_ID) },
                    displayMs.toLong(),
                )
            }
        } catch (_: SecurityException) {
            Toast.makeText(app, text, Toast.LENGTH_LONG).show()
        }
    }

    fun show(context: Context, resId: Int, durationMs: Int = DISPLAY_MS) {
        show(context, context.getString(resId), durationMs)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureMessageChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(MESSAGE_CHANNEL_ID) == null
        ) {
            manager.createNotificationChannel(
                NotificationChannel(
                    MESSAGE_CHANNEL_ID,
                    context.getString(R.string.notifications_important),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    enableVibration(true)
                }
            )
        }
    }

    private fun normalizeDuration(durationMs: Int): Int {
        return if (durationMs == Toast.LENGTH_SHORT || durationMs == Toast.LENGTH_LONG) {
            DISPLAY_MS
        } else {
            durationMs.coerceAtLeast(MIN_DISPLAY_MS)
        }
    }

    private const val MESSAGE_CHANNEL_ID = "ShelterUserAlerts"
    private const val MESSAGE_NOTIFICATION_ID = 0xe49d1
    private const val MIN_DISPLAY_MS = 2000
}

