
package com.furandfocus.planner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    const val ACTION_TEST_REMINDER =
        "com.furandfocus.planner.ACTION_TEST_REMINDER"

    private const val TEST_REQUEST_CODE = 872641

    fun scheduleAll(context: Context, bookings: List<Booking>) {
        bookings.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, booking: Booking) {
        cancel(context, booking)

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        booking.reminderOffsetsMinutes
            .filter { it > 0 }
            .distinct()
            .forEachIndexed { index, offsetMinutes ->
                val triggerAt = booking.dateTimeMillis -
                    offsetMinutes * 60_000L

                if (triggerAt <= System.currentTimeMillis()) {
                    return@forEachIndexed
                }

                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("bookingId", booking.id)
                    putExtra("clientName", booking.clientName)
                    putExtra("dogName", booking.dogName)
                    putExtra("bookingTime", booking.dateTimeMillis)
                    putExtra("reminderIndex", index)
                    putExtra("reminderOffsetMinutes", offsetMinutes)
                    putExtra("sessionType", booking.sessionType)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode(booking.id, index),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

                setAlarm(alarmManager, triggerAt, pendingIntent)
            }
    }

    fun scheduleTest(context: Context): Boolean {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TEST_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + 60_000L
        setAlarm(alarmManager, triggerAt, pendingIntent)
        return true
    }

    private fun setAlarm(
        alarmManager: AlarmManager,
        triggerAt: Long,
        pendingIntent: PendingIntent
    ) {
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

    fun cancel(context: Context, booking: Booking) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel all reminder slots that could have been created for this booking.
        repeat(50) { index ->
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(booking.id, index),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun requestCode(bookingId: String, index: Int): Int {
        return "$bookingId:$index".hashCode()
    }
}
