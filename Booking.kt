package com.furandfocus.planner

import org.json.JSONObject

data class Booking(
    val id: Long,
    val client: String,
    val dog: String,
    val dateTime: Long,
    val location: String,
    val notes: String,
    val status: String = "Confirmed"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("client", client); put("dog", dog)
        put("dateTime", dateTime); put("location", location)
        put("notes", notes); put("status", status)
    }

    companion object {
        fun fromJson(o: JSONObject) = Booking(
            id = o.optLong("id"),
            client = o.optString("client"),
            dog = o.optString("dog"),
            dateTime = o.optLong("dateTime"),
            location = o.optString("location"),
            notes = o.optString("notes"),
            status = o.optString("status", "Confirmed")
        )
    }
}
