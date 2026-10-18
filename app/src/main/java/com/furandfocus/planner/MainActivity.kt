
package com.furandfocus.planner

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val bookings = mutableListOf<Booking>()

    private lateinit var listContainer: LinearLayout
    private lateinit var countLabel: TextView
    private lateinit var upcomingCount: TextView
    private lateinit var monthCount: TextView
    private lateinit var filterRow: LinearLayout

    private var selectedFilter = "Upcoming"
    private var selectedDateTime = Calendar.getInstance()

    private val cream = Color.rgb(248, 246, 239)
    private val darkGreen = Color.rgb(47, 78, 63)
    private val sage = Color.rgb(220, 231, 217)
    private val mutedGreen = Color.rgb(106, 129, 108)
    private val darkText = Color.rgb(42, 49, 42)
    private val mutedText = Color.rgb(115, 119, 108)
    private val white = Color.WHITE
    private val palePink = Color.rgb(247, 229, 223)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookings.addAll(BookingStore.getAll(this))
        ReminderScheduler.scheduleAll(this, bookings)

        buildScreen()
        refreshBookings()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(cream)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(30))
        }

        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(scroll)

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "F&F"
            textSize = 19f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(darkGreen, 16)
        }
        brandRow.addView(logo, LinearLayout.LayoutParams(dp(58), dp(58)))

        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brandText.addView(text("FUR & FOCUS", 19f, darkGreen, true))
        brandText.addView(text("PHOTOGRAPHY PLANNER", 10f, mutedText, true))
        brandRow.addView(
            brandText,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        root.addView(brandRow)
        root.addView(space(25))
        root.addView(text("Your sessions,", 29f, darkText, true))
        root.addView(text("beautifully organised.", 25f, darkGreen, true))
        root.addView(space(8))
        root.addView(
            text(
                "Clients, dogs and shoot reminders — all in one place.",
                14f,
                mutedText
            )
        )
        root.addView(space(22))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rounded(darkGreen, 22)
        }
        hero.addView(text("YOUR NEXT CHAPTER", 11f, sage, true))
        hero.addView(space(8))
        hero.addView(
            text("Every wag deserves\na little spotlight.", 23f, white, true)
        )
        hero.addView(space(16))

        val addButton = Button(this).apply {
            text = "+  Add a photography session"
            textSize = 14f
            isAllCaps = false
            setTextColor(darkGreen)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(sage, 14)
            setOnClickListener { showAddBookingDialog() }
        }
        hero.addView(addButton, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(hero)
        root.addView(space(22))

        root.addView(text("YOUR OVERVIEW", 12f, mutedText, true))
        root.addView(space(10))

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val upcomingCard = statCard("UPCOMING", "0", "sessions booked")
        upcomingCount = upcomingCard.second
        val monthCard = statCard("THIS MONTH", "0", "sessions scheduled")
        monthCount = monthCard.second

        statsRow.addView(
            upcomingCard.first,
            LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                marginEnd = dp(6)
            }
        )
        statsRow.addView(
            monthCard.first,
            LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                marginStart = dp(6)
            }
        )
        root.addView(statsRow)
        root.addView(space(24))

        root.addView(text("NOTIFICATION TOOLS", 12f, mutedText, true))
        root.addView(space(8))

        root.addView(
            toolButton("Test notification now") {
                sendPlannerNotification(
                    "Fur & Focus test",
                    "Your notifications are working."
                )
            }
        )
        root.addView(space(7))
        root.addView(
            toolButton("Test scheduled reminder · 1 minute") {
                requestNotificationPermissionIfNeeded()
                if (
                    Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(
                        this,
                        "Allow notifications first, then try again.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    ReminderScheduler.scheduleTest(this)
                    Toast.makeText(
                        this,
                        "Test scheduled. Keep the app installed and wait about a minute.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
        root.addView(space(7))
        root.addView(
            toolButton("Notify me of all upcoming bookings") {
                sendUpcomingBookingsNotification()
            }
        )
        root.addView(space(24))

        val headingRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headingRow.addView(
            text("Your sessions", 21f, darkText, true),
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        countLabel = text("0 sessions", 12f, mutedText)
        headingRow.addView(countLabel)
        root.addView(headingRow)
        root.addView(space(12))

        filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        root.addView(filterRow)
        root.addView(space(14))

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(listContainer)
        refreshFilters()
    }

    private fun statCard(
        title: String,
        initialNumber: String,
        subtitle: String
    ): Pair<LinearLayout, TextView> {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(12), dp(12))
            background = rounded(white, 18)
        }
        card.addView(text(title, 10f, mutedText, true))
        card.addView(space(7))
        val number = text(initialNumber, 27f, darkGreen, true)
        card.addView(number)
        card.addView(space(2))
        card.addView(text(subtitle, 11f, mutedText))
        return Pair(card, number)
    }

    private fun toolButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(darkGreen)
            background = rounded(white, 13)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(-1, dp(48))
        }
    }

    private fun refreshFilters() {
        filterRow.removeAllViews()

        listOf("Upcoming", "Past", "All").forEach { filter ->
            val selected = filter == selectedFilter
            val chip = TextView(this).apply {
                text = filter
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(10), dp(18), dp(10))
                setTextColor(if (selected) white else darkGreen)
                background = rounded(if (selected) darkGreen else white, 22)
                setOnClickListener {
                    selectedFilter = filter
                    refreshFilters()
                    refreshBookings()
                }
            }
            filterRow.addView(
                chip,
                LinearLayout.LayoutParams(-2, dp(42)).apply {
                    marginEnd = dp(8)
                }
            )
        }
    }

    private fun showAddBookingDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(4))
        }

        val clientInput = input("Client's name")
        val dogInput = input("Dog's name")
        val locationInput = input("Session location (optional)")
        val notesInput = input("Notes (optional)")
        val sessionTypeInput = input("Session type (e.g. Outdoor portrait)")
        sessionTypeInput.setText("Pet portrait")

        val reminderInput = input("Reminders: e.g. 2w, 1w, 3d, 1d, 2h")
        reminderInput.setText("1d, 2h")
        reminderInput.setSingleLine(false)
        reminderInput.minLines = 2
        reminderInput.maxLines = 3

        form.addView(clientInput)
        form.addView(space(6))
        form.addView(dogInput)
        form.addView(space(6))
        form.addView(sessionTypeInput)
        form.addView(space(6))
        form.addView(locationInput)
        form.addView(space(6))
        form.addView(notesInput)
        form.addView(space(6))
        form.addView(text("REMINDER SCHEDULE", 11f, mutedGreen, true))
        form.addView(space(3))
        form.addView(reminderInput)
        form.addView(
            text(
                "Use w for weeks, d for days, h for hours, or m for minutes. " +
                    "Separate reminders with commas. The number of entries is the reminder count.",
                11f,
                mutedText
            )
        )

        val dateButton = Button(this).apply {
            isAllCaps = false
            text = "Choose date: ${dateLabel(selectedDateTime.timeInMillis)}"
            setOnClickListener {
                DatePickerDialog(
                    this@MainActivity,
                    { _, year, month, day ->
                        selectedDateTime.set(Calendar.YEAR, year)
                        selectedDateTime.set(Calendar.MONTH, month)
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, day)
                        text = "Choose date: ${dateLabel(selectedDateTime.timeInMillis)}"
                    },
                    selectedDateTime.get(Calendar.YEAR),
                    selectedDateTime.get(Calendar.MONTH),
                    selectedDateTime.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }

        val timeButton = Button(this).apply {
            isAllCaps = false
            text = "Choose time: ${timeLabel(selectedDateTime)}"
            setOnClickListener {
                TimePickerDialog(
                    this@MainActivity,
                    { _, hour, minute ->
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                        selectedDateTime.set(Calendar.MINUTE, minute)
                        selectedDateTime.set(Calendar.SECOND, 0)
                        text = "Choose time: ${timeLabel(selectedDateTime)}"
                    },
                    selectedDateTime.get(Calendar.HOUR_OF_DAY),
                    selectedDateTime.get(Calendar.MINUTE),
                    true
                ).show()
            }
        }

        form.addView(space(8))
        form.addView(dateButton)
        form.addView(timeButton)

        AlertDialog.Builder(this)
            .setTitle("New photography session")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save session") { _, _ ->
                val client = clientInput.text.toString().trim()
                val dog = dogInput.text.toString().trim()
                val reminderText = reminderInput.text.toString().trim()

                if (client.isBlank() || dog.isBlank()) {
                    Toast.makeText(
                        this,
                        "Please enter both the client's and dog's names.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }

                val offsets = parseReminderOffsets(reminderText)
                if (offsets == null) {
                    Toast.makeText(
                        this,
                        "Check your reminders. Example: 2w, 1w, 3d, 2h",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }

                val booking = Booking(
                    clientName = client,
                    dogName = dog,
                    dateTimeMillis = selectedDateTime.timeInMillis,
                    location = locationInput.text.toString().trim(),
                    notes = notesInput.text.toString().trim(),
                    sessionType = sessionTypeInput.text.toString().trim()
                        .ifBlank { "Pet portrait" },
                    reminderOffsetsMinutes = offsets
                )

                bookings.add(booking)
                bookings.sortBy { it.dateTimeMillis }
                BookingStore.saveAll(this, bookings)
                ReminderScheduler.schedule(this, booking)

                selectedDateTime = Calendar.getInstance()
                refreshBookings()

                Toast.makeText(
                    this,
                    "Session saved with ${offsets.size} reminder(s).",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun parseReminderOffsets(raw: String): List<Int>? {
        if (raw.isBlank()) return emptyList()

        val result = mutableListOf<Int>()
        val entries = raw.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }

        for (entry in entries) {
            val match = Regex("""^(\d+)\s*(w|weeks?|d|days?|h|hours?|m|minutes?)$""")
                .matchEntire(entry.lowercase())
                ?: return null

            val amount = match.groupValues[1].toIntOrNull() ?: return null
            if (amount <= 0) return null

            val unit = match.groupValues[2]
            val multiplier = when {
                unit.startsWith("w") -> 10080L
                unit.startsWith("d") -> 1440L
                unit.startsWith("h") -> 60L
                else -> 1L
            }

            val minutes = amount.toLong() * multiplier
            if (minutes > Int.MAX_VALUE) return null
            result.add(minutes.toInt())
        }

        return result.distinct().sortedDescending()
    }

    private fun input(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 14f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(white, 12)

            if (
                hintText == "Notes (optional)" ||
                hintText.startsWith("Reminders:")
            ) {
                minLines = 2
                maxLines = 4
                setSingleLine(false)
            } else {
                setSingleLine(true)
            }
        }
    }

    private fun refreshBookings() {
        listContainer.removeAllViews()

        val now = System.currentTimeMillis()
        val upcoming = bookings.filter { it.dateTimeMillis >= now }
        val currentMonth = Calendar.getInstance()

        val monthTotal = bookings.count { booking ->
            val date = Calendar.getInstance().apply {
                timeInMillis = booking.dateTimeMillis
            }
            date.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                date.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
        }

        upcomingCount.text = upcoming.size.toString()
        monthCount.text = monthTotal.toString()

        val filtered = when (selectedFilter) {
            "Upcoming" -> bookings.filter { it.dateTimeMillis >= now }
                .sortedBy { it.dateTimeMillis }
            "Past" -> bookings.filter { it.dateTimeMillis < now }
                .sortedByDescending { it.dateTimeMillis }
            else -> bookings.sortedBy { it.dateTimeMillis }
        }

        countLabel.text = "${filtered.size} ${
            if (filtered.size == 1) "session" else "sessions"
        }"

        if (filtered.isEmpty()) {
            val emptyCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(28), dp(20), dp(28))
                background = rounded(white, 18)
            }
            val emptyTitle = when (selectedFilter) {
                "Upcoming" -> "No upcoming sessions yet"
                "Past" -> "No past sessions to show"
                else -> "Your session list is empty"
            }
            emptyCard.addView(text(emptyTitle, 17f, darkGreen, true))
            emptyCard.addView(space(8))
            emptyCard.addView(
                text(
                    "Add a session to keep your photography plans organised.",
                    13f,
                    mutedText
                )
            )
            listContainer.addView(emptyCard)
            return
        }

        filtered.forEach { booking ->
            listContainer.addView(createBookingCard(booking))
            listContainer.addView(space(12))
        }
    }

    private fun createBookingCard(booking: Booking): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(16), dp(17), dp(15))
            background = rounded(white, 18)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topRow.addView(
            text(booking.dogName, 19f, darkGreen, true),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val isPast = booking.dateTimeMillis < System.currentTimeMillis()
        val badge = TextView(this).apply {
            text = if (isPast) "COMPLETED" else "UPCOMING"
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isPast) mutedText else darkGreen)
            setPadding(dp(9), dp(6), dp(9), dp(6))
            background = rounded(if (isPast) palePink else sage, 12)
        }
        topRow.addView(badge)
        card.addView(topRow)
        card.addView(space(5))
        card.addView(text(booking.sessionType, 13f, mutedGreen, true))
        card.addView(space(4))
        card.addView(text("Client: ${booking.clientName}", 14f, darkText))
        card.addView(space(8))
        card.addView(
            text(
                "${dateLabel(booking.dateTimeMillis)} · ${timeLabel(booking.dateTimeMillis)}",
                13f,
                mutedGreen,
                true
            )
        )

        if (booking.location.isNotBlank()) {
            card.addView(space(5))
            card.addView(text("Location: ${booking.location}", 13f, mutedText))
        }
        if (booking.notes.isNotBlank()) {
            card.addView(space(5))
            card.addView(text("Notes: ${booking.notes}", 13f, mutedText))
        }

        card.addView(space(8))
        card.addView(
            text(
                "${booking.reminderOffsetsMinutes.size} reminder(s): " +
                    formatReminderOffsets(booking.reminderOffsetsMinutes),
                12f,
                mutedText
            )
        )
        card.addView(space(12))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val deleteButton = Button(this).apply {
            text = "Delete session"
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.rgb(145, 68, 61))
            background = rounded(palePink, 12)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete session?")
                    .setMessage("Delete the photography session for ${booking.dogName}?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        ReminderScheduler.cancel(this@MainActivity, booking)
                        bookings.removeAll { it.id == booking.id }
                        BookingStore.saveAll(this@MainActivity, bookings)
                        refreshBookings()
                        Toast.makeText(
                            this@MainActivity,
                            "Session deleted.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .show()
            }
        }
        actions.addView(deleteButton, LinearLayout.LayoutParams(-2, dp(42)))
        card.addView(actions)
        return card
    }

    private fun formatReminderOffsets(offsets: List<Int>): String {
        if (offsets.isEmpty()) return "No reminders set"

        return offsets.joinToString(", ") { minutes ->
            when {
                minutes >= 10080 && minutes % 10080 == 0 ->
                    "${minutes / 10080}w"
                minutes >= 1440 && minutes % 1440 == 0 ->
                    "${minutes / 1440}d"
                minutes >= 60 && minutes % 60 == 0 ->
                    "${minutes / 60}h"
                else -> "${minutes}m"
            }
        }
    }

    private fun sendPlannerNotification(title: String, message: String) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionIfNeeded()
            Toast.makeText(
                this,
                "Allow notifications, then tap the button again.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val channelId = "fur_focus_manual_notifications"
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Fur & Focus planner",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            notification
        )
    }

    private fun sendUpcomingBookingsNotification() {
        val upcoming = bookings
            .filter { it.dateTimeMillis >= System.currentTimeMillis() }
            .sortedBy { it.dateTimeMillis }

        if (upcoming.isEmpty()) {
            sendPlannerNotification(
                "No upcoming sessions",
                "You don't currently have any upcoming photography bookings."
            )
            return
        }

        val formatter = SimpleDateFormat("EEE d MMM, HH:mm", Locale.UK)
        val details = upcoming.joinToString("\n\n") { booking ->
            buildString {
                append("${booking.dogName} — ${booking.clientName}")
                append("\n${booking.sessionType}")
                append("\n${formatter.format(Date(booking.dateTimeMillis))}")
                if (booking.location.isNotBlank()) append("\n${booking.location}")
            }
        }

        sendPlannerNotification(
            "${upcoming.size} upcoming photography " +
                if (upcoming.size == 1) "session" else "sessions",
            details
        )
    }

    private fun text(
        value: String,
        size: Float,
        colour: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(colour)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun space(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(height))
        }
    }

    private fun rounded(colour: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(colour)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dateLabel(timeMillis: Long): String {
        return SimpleDateFormat("EEE, d MMM yyyy", Locale.UK)
            .format(Date(timeMillis))
    }

    private fun timeLabel(calendar: Calendar): String {
        return SimpleDateFormat("HH:mm", Locale.UK).format(calendar.time)
    }

    private fun timeLabel(timeMillis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.UK).format(Date(timeMillis))
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
