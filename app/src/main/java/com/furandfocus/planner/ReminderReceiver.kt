package com.furandfocus.planner

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bookingId = intent.getStringExtra("bookingId") ?: return
        val clientName = intent.getStringExtra("clientName") ?: "Client"
        val dogName = intent.getStringExtra("dogName") ?: "Dog"
        val bookingTime = intent.getLongExtra("bookingTime", 0L)
        val reminderIndex = intent.getIntExtra("reminderIndex", 0)

        val channelId = "fur_focus_reminders"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Photography booking reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming Fur & Focus photo sessions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val formattedTime = SimpleDateFormat(
            "EEE d MMM 'at' HH:mm",
            Locale.UK
        ).format(Date(bookingTime))

        val title = if (reminderIndex == 0) {
            "Photography session coming up"
        } else {
            "Photography session in 2 hours"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText("$clientName • $dogName • $formattedTime")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$clientName's session with $dogName is scheduled for $formattedTime.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify((bookingId + reminderIndex).hashCode(), notification)
    }
}
