
package com.furandfocus.planner

import java.util.UUID

data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val clientName: String,
    val dogName: String,
    val dateTimeMillis: Long,
    val location: String = "",
    val notes: String = "",
    val sessionType: String = "Pet portrait",
    val reminderOffsetsMinutes: List<Int> = listOf(1440, 120),
    // Retained so older booking records remain compatible.
    val reminderDays: Int = 1,
    val reminderHoursBefore: Int = 24
)
