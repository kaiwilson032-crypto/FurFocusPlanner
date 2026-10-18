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
                bookings.add(
                    Booking(
                        id = item.optString("id"),
                        clientName = item.optString("clientName"),
                        dogName = item.optString("dogName"),
                        dateTimeMillis = item.optLong("dateTimeMillis"),
                        location = item.optString("location"),
                        notes = item.optString("notes"),
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
            item.put("reminderDays", booking.reminderDays)
            item.put("reminderHoursBefore", booking.reminderHoursBefore)
            array.put(item)
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BOOKINGS, array.toString())
            .apply()
    }
}
