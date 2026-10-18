
package com.furandfocus.planner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BookingStore {
    private const val PREFS = "fur_focus_bookings"
    private const val KEY_BOOKINGS = "bookings"

    fun getAll(context: Context): MutableList<Booking> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_BOOKINGS, null) ?: return mutableListOf()

        return try {
            val array = JSONArray(raw)
            val bookings = mutableListOf<Booking>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                val offsets = mutableListOf<Int>()
                val savedOffsets = item.optJSONArray("reminderOffsetsMinutes")

                if (savedOffsets != null) {
                    for (j in 0 until savedOffsets.length()) {
                        val minutes = savedOffsets.optInt(j, 0)
                        if (minutes > 0) offsets.add(minutes)
                    }
                }

                // Older records used a lead time in hours and a second
                // reminder two hours before the session.
                if (offsets.isEmpty()) {
                    val oldHours = item.optInt("reminderHoursBefore", 24)
                    if (oldHours > 0) offsets.add(oldHours * 60)
                    offsets.add(120)
                }

                bookings.add(
                    Booking(
                        id = item.optString("id"),
                        clientName = item.optString("clientName"),
                        dogName = item.optString("dogName"),
                        dateTimeMillis = item.optLong("dateTimeMillis"),
                        location = item.optString("location"),
                        notes = item.optString("notes"),
                        sessionType = item.optString(
                            "sessionType",
                            "Pet portrait"
                        ).ifBlank { "Pet portrait" },
                        reminderOffsetsMinutes = offsets.distinct().sortedDescending(),
                        reminderDays = item.optInt("reminderDays", 1),
                        reminderHoursBefore = item.optInt("reminderHoursBefore", 24)
                    )
                )
            }

            bookings
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveAll(context: Context, bookings: List<Booking>) {
        val array = JSONArray()

        bookings.forEach { booking ->
            val item = JSONObject()
            item.put("id", booking.id)
            item.put("clientName", booking.clientName)
            item.put("dogName", booking.dogName)
            item.put("dateTimeMillis", booking.dateTimeMillis)
            item.put("location", booking.location)
            item.put("notes", booking.notes)
            item.put("sessionType", booking.sessionType)
            item.put("reminderDays", booking.reminderDays)
            item.put("reminderHoursBefore", booking.reminderHoursBefore)

            val offsets = JSONArray()
            booking.reminderOffsetsMinutes
                .filter { it > 0 }
                .distinct()
                .forEach { offsets.put(it) }

            item.put("reminderOffsetsMinutes", offsets)
            array.put(item)
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BOOKINGS, array.toString())
            .apply()
    }
}
