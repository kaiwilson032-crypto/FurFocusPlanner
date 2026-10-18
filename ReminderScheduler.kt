package com.furandfocus.planner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun scheduleAll(context: Context) {
        BookingStore.all(context).forEach { schedule(context, it) }
    }

    fun schedule(context: Context, booking: Booking) {
        val reminders = listOf(24L * 60 * 60 * 1000, 2L * 60 * 60 * 1000)
        reminders.forEachIndexed { index, offset ->
            val at = booking.dateTime - offset
            if (at <= System.currentTimeMillis()) return@forEachIndexed
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("bookingId", booking.id)
                putExtra("client", booking.client)
                putExtra("dog", booking.dog)
                putExtra("dateTime", booking.dateTime)
                putExtra("reminderIndex", index)
            }
            val pi = PendingIntent.getBroadcast(
                context, requestCode(booking.id, index), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // Inexact while-idle alarms avoid requiring special exact-alarm access.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                alarms.set(AlarmManager.RTC_WAKEUP, at, pi)
            }
        }
    }

    fun cancel(context: Context, booking: Booking) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (index in 0..1) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, requestCode(booking.id, index), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarms.cancel(pi)
            pi.cancel()
        }
    }

    private fun requestCode(id: Long, index: Int): Int =
        ((id xor (id ushr 32)).toInt() * 31) + index
}
