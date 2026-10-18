package com.furandfocus.planner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleAll(context: Context, bookings: List<Booking>) {
        bookings.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, booking: Booking) {
        val reminderTimes = listOf(
            TimeUnit.HOURS.toMillis(booking.reminderHoursBefore.toLong()),
            TimeUnit.HOURS.toMillis(2)
        )

        reminderTimes.distinct().forEachIndexed { index, offset ->
            val triggerAt = booking.dateTimeMillis - offset

            if (triggerAt <= System.currentTimeMillis()) return@forEachIndexed

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("bookingId", booking.id)
                putExtra("clientName", booking.clientName)
                putExtra("dogName", booking.dogName)
                putExtra("bookingTime", booking.dateTimeMillis)
                putExtra("reminderIndex", index)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (booking.id + index).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    fun cancel(context: Context, booking: Booking) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        repeat(2) { index ->
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (booking.id + index).hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
