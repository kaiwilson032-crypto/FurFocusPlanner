package com.furandfocus.planner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "shoot_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Photography reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val client = intent.getStringExtra("client") ?: "Client"
        val dog = intent.getStringExtra("dog") ?: "Dog"
        val dateTime = intent.getLongExtra("dateTime", 0L)
        val index = intent.getIntExtra("reminderIndex", 0)
        val whenText = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(dateTime))
        val title = if (index == 0) "Shoot tomorrow" else "Shoot in 2 hours"
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("$title: $dog")
            .setContentText("$client • $whenText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$client's photo session with $dog is scheduled for $whenText."))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify((intent.getLongExtra("bookingId", 1L) % Int.MAX_VALUE).toInt() + index, notification)
        } catch (_: SecurityException) {
            // The app will ask for notification permission when opened.
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.scheduleAll(context)
    }
}
