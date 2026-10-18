package com.furandfocus.planner

data class Booking(
    val id: String = java.util.UUID.randomUUID().toString(),
    val clientName: String,
    val dogName: String,
    val dateTimeMillis: Long,
    val location: String = "",
    val notes: String = "",
    val reminderDays: Int = 1,
    val reminderHoursBefore: Int = 24
)
