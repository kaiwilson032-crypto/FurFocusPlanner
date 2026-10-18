package com.furandfocus.planner

import android.content.Context
import org.json.JSONArray

object BookingStore {
    private const val PREFS = "fur_focus_planner"
    private const val KEY = "bookings"

    fun all(context: Context): MutableList<Booking> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val result = mutableListOf<Booking>()
        for (i in 0 until array.length()) {
            try { result.add(Booking.fromJson(array.getJSONObject(i))) } catch (_: Exception) {}
        }
        return result.sortedBy { it.dateTime }.toMutableList()
    }

    fun save(context: Context, bookings: List<Booking>) {
        val array = JSONArray()
        bookings.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }
}
