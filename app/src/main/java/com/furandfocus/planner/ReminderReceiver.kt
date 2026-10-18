
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
        val channelId = "fur_focus_reminders"
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Photography session reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled reminders for Fur & Focus sessions"
            }
            manager.createNotificationChannel(channel)
        }

        if (
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (intent.action == ReminderScheduler.ACTION_TEST_REMINDER) {
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle("Scheduled reminder test")
                .setContentText("Your scheduled notification has fired.")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Success! Android delivered the test alarm. " +
                            "Your phone can receive scheduled Fur & Focus reminders."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(872641, notification)
            return
        }

        val bookingId = intent.getStringExtra("bookingId") ?: return
        val clientName = intent.getStringExtra("clientName") ?: "Client"
        val dogName = intent.getStringExtra("dogName") ?: "Dog"
        val sessionType = intent.getStringExtra("sessionType") ?: "Photography session"
        val bookingTime = intent.getLongExtra("bookingTime", 0L)
        val offsetMinutes = intent.getIntExtra("reminderOffsetMinutes", 0)

        val formattedTime = SimpleDateFormat(
            "EEE d MMM yyyy 'at' HH:mm",
            Locale.UK
        ).format(Date(bookingTime))

        val leadTime = when {
            offsetMinutes >= 10080 && offsetMinutes % 10080 == 0 ->
                "${offsetMinutes / 10080} week(s)"
            offsetMinutes >= 1440 && offsetMinutes % 1440 == 0 ->
                "${offsetMinutes / 1440} day(s)"
            offsetMinutes >= 60 && offsetMinutes % 60 == 0 ->
                "${offsetMinutes / 60} hour(s)"
            else -> "$offsetMinutes minutes"
        }

        val message =
            "$clientName's $sessionType session with $dogName is scheduled " +
                "for $formattedTime. Reminder: $leadTime beforehand."

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Upcoming photography session")
            .setContentText("$dogName · $formattedTime")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify("$bookingId:$offsetMinutes".hashCode(), notification)
    }
}
