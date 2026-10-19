
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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
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

    private val cream = Color.rgb(249, 248, 243)
    private val darkGreen = Color.rgb(39, 76, 59)
    private val green = Color.rgb(69, 119, 85)
    private val sage = Color.rgb(222, 237, 218)
    private val paleSage = Color.rgb(237, 244, 232)
    private val mutedText = Color.rgb(117, 126, 114)
    private val darkText = Color.rgb(39, 47, 40)
    private val white = Color.WHITE
    private val pink = Color.rgb(250, 232, 227)
    private val orange = Color.rgb(225, 155, 94)

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
            clipToPadding = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(32))
        }

        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(scroll)

        // Brand header with a custom camera-and-paw mark.
        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = CameraPawLogo(this)
        brandRow.addView(logo, LinearLayout.LayoutParams(dp(62), dp(62)))

        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, 0, 0)
        }
        brandText.addView(text("FUR & FOCUS", 19f, darkGreen, true))
        brandText.addView(space(3))
        brandText.addView(text("PHOTOGRAPHY PLANNER", 10f, mutedText, true))

        brandRow.addView(
            brandText,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val liveTag = TextView(this).apply {
            text = "●  PLANNER"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(paleSage, 20)
        }
        brandRow.addView(liveTag)
        root.addView(brandRow)
        root.addView(space(27))

        root.addView(text("LET'S MAKE", 12f, green, true))
        root.addView(space(4))
        root.addView(text("tails wag.", 36f, darkText, true))
        root.addView(text("One beautiful session at a time.", 16f, mutedText))
        root.addView(space(22))

        // Main call-to-action panel.
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(21), dp(21), dp(21), dp(21))
            background = roundedGradient(darkGreen, green, 25)
            elevation = dp(3).toFloat()
        }

        hero.addView(text("YOUR PHOTOGRAPHY BUSINESS", 10f, sage, true))
        hero.addView(space(10))
        hero.addView(
            text("Every wag deserves\na little spotlight.", 25f, white, true)
        )
        hero.addView(space(10))
        hero.addView(
            text(
                "Keep your clients, sessions and reminders all in one place.",
                13f,
                Color.rgb(226, 237, 225)
            )
        )
        hero.addView(space(19))

        val addButton = Button(this).apply {
            text = "＋   New photography session"
            textSize = 14f
            isAllCaps = false
            setTextColor(darkGreen)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(white, 16)
            elevation = dp(2).toFloat()
            setOnClickListener { showAddBookingDialog() }
        }
        hero.addView(addButton, LinearLayout.LayoutParams(-1, dp(54)))
        root.addView(hero)
        root.addView(space(25))

        sectionLabel(root, "YOUR OVERVIEW", "A little look at what's ahead")

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val upcomingCard = statCard("UPCOMING", "0", "sessions to come", "↗")
        upcomingCount = upcomingCard.second

        val monthCard = statCard("THIS MONTH", "0", "sessions scheduled", "✦")
        monthCount = monthCard.second

        statsRow.addView(
            upcomingCard.first,
            LinearLayout.LayoutParams(0, dp(132), 1f).apply {
                marginEnd = dp(7)
            }
        )
        statsRow.addView(
            monthCard.first,
            LinearLayout.LayoutParams(0, dp(132), 1f).apply {
                marginStart = dp(7)
            }
        )
        root.addView(statsRow)
        root.addView(space(26))

        sectionLabel(root, "QUICK TOOLS", "Make sure you're ready for shoot day")

        root.addView(
            toolButton("Send a test notification", "●") {
                sendPlannerNotification(
                    "Fur & Focus test",
                    "Your notifications are working."
                )
            }
        )
        root.addView(space(8))
        root.addView(
            toolButton("Test a scheduled reminder · 1 min", "◷") {
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
                        "Test scheduled. Wait about a minute.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
        root.addView(space(8))
        root.addView(
            toolButton("Notify me of upcoming bookings", "♧") {
                sendUpcomingBookingsNotification()
            }
        )
        root.addView(space(27))

        val headingRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headingRow.addView(
            text("Your sessions", 23f, darkText, true),
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        countLabel = text("0 sessions", 12f, mutedText)
        headingRow.addView(countLabel)
        root.addView(headingRow)
        root.addView(space(5))
        root.addView(text("All your favourite faces, neatly organised.", 13f, mutedText))
        root.addView(space(16))

        filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        root.addView(filterRow)
        root.addView(space(15))

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(listContainer)

        refreshFilters()
        animateIn(root)
    }

    private fun sectionLabel(
        root: LinearLayout,
        title: String,
        subtitle: String
    ) {
        root.addView(text(title, 11f, green, true))
        root.addView(space(5))
        root.addView(text(subtitle, 13f, mutedText))
        root.addView(space(13))
    }

    private fun statCard(
        title: String,
        initialNumber: String,
        subtitle: String,
        symbol: String
    ): Pair<LinearLayout, TextView> {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(12), dp(13))
            background = rounded(white, 20)
            elevation = dp(2).toFloat()
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(
            text(title, 10f, mutedText, true),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val icon = TextView(this).apply {
            text = symbol
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(green)
            background = rounded(paleSage, 12)
        }
        top.addView(icon, LinearLayout.LayoutParams(dp(30), dp(30)))
        card.addView(top)
        card.addView(space(7))

        val number = text(initialNumber, 30f, darkGreen, true)
        card.addView(number)
        card.addView(space(2))
        card.addView(text(subtitle, 11f, mutedText))

        return Pair(card, number)
    }

    private fun toolButton(
        label: String,
        symbol: String,
        action: () -> Unit
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(11), dp(13), dp(11))
            background = rounded(white, 16)
            elevation = dp(1).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener {
                tapFeedback(this)
                action()
            }
        }

        val icon = TextView(this).apply {
            text = symbol
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(green)
            background = rounded(paleSage, 12)
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(38), dp(38)))

        row.addView(
            text(label, 13f, darkText, true),
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = dp(12)
            }
        )
        row.addView(text("›", 24f, mutedText))

        return row
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
                setPadding(dp(19), dp(10), dp(19), dp(10))
                setTextColor(if (selected) white else darkGreen)
                background = rounded(
                    if (selected) darkGreen else white,
                    24
                )
                elevation = if (selected) dp(2).toFloat() else 0f

                setOnClickListener {
                    selectedFilter = filter
                    refreshFilters()
                    refreshBookings()
                }
            }

            filterRow.addView(
                chip,
                LinearLayout.LayoutParams(-2, dp(43)).apply {
                    marginEnd = dp(8)
                }
            )
        }
    }

    private fun showAddBookingDialog() {
        selectedDateTime = Calendar.getInstance()

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        val clientInput = input("Client's name")
        val dogInput = input("Dog's name")
        val sessionTypeInput = input("Session type")
        sessionTypeInput.setText("Pet portrait")
        val locationInput = input("Session location (optional)")
        val notesInput = input("Notes (optional)")
        notesInput.minLines = 2
        notesInput.maxLines = 3
        notesInput.setSingleLine(false)

        val reminderInput = input("Reminders: e.g. 2w, 1w, 1d, 2h")
        reminderInput.setText("1d, 2h")
        reminderInput.minLines = 2
        reminderInput.maxLines = 3
        reminderInput.setSingleLine(false)

        addFormField(form, "CLIENT", clientInput)
        addFormField(form, "DOG", dogInput)
        addFormField(form, "SESSION TYPE", sessionTypeInput)
        addFormField(form, "LOCATION (OPTIONAL)", locationInput)
        addFormField(form, "NOTES (OPTIONAL)", notesInput)

        form.addView(space(5))
        form.addView(text("REMINDER SCHEDULE", 11f, green, true))
        form.addView(space(5))
        form.addView(reminderInput)
        form.addView(space(5))
        form.addView(
            text(
                "Use w for weeks, d for days, h for hours or m for minutes. " +
                    "Separate reminders with commas. Each entry creates one reminder.",
                11f,
                mutedText
            )
        )
        form.addView(space(12))

        val dateButton = pickerButton("")
        val timeButton = pickerButton("")
        updatePickerLabels(dateButton, timeButton)

        dateButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDateTime.set(Calendar.YEAR, year)
                    selectedDateTime.set(Calendar.MONTH, month)
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, day)
                    updatePickerLabels(dateButton, timeButton)
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        timeButton.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                    selectedDateTime.set(Calendar.MINUTE, minute)
                    selectedDateTime.set(Calendar.SECOND, 0)
                    selectedDateTime.set(Calendar.MILLISECOND, 0)
                    updatePickerLabels(dateButton, timeButton)
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true
            ).show()
        }

        form.addView(dateButton)
        form.addView(space(8))
        form.addView(timeButton)

        val formScroll = ScrollView(this).apply {
            isFillViewport = false
            addView(form)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("New photography session")
            .setView(formScroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save session", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(darkGreen)
                setOnClickListener {
                    val client = clientInput.text.toString().trim()
                    val dog = dogInput.text.toString().trim()
                    val offsets = parseReminderOffsets(
                        reminderInput.text.toString().trim()
                    )

                    when {
                        client.isBlank() || dog.isBlank() -> {
                            Toast.makeText(
                                this@MainActivity,
                                "Enter both the client's and dog's names.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        offsets == null -> {
                            Toast.makeText(
                                this@MainActivity,
                                "Check reminders. Example: 2w, 1w, 3d, 2h",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        selectedDateTime.timeInMillis <= System.currentTimeMillis() -> {
                            Toast.makeText(
                                this@MainActivity,
                                "Choose a future date and time.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> {
                            val booking = Booking(
                                clientName = client,
                                dogName = dog,
                                dateTimeMillis = selectedDateTime.timeInMillis,
                                location = locationInput.text.toString().trim(),
                                notes = notesInput.text.toString().trim(),
                                sessionType = sessionTypeInput.text.toString()
                                    .trim().ifBlank { "Pet portrait" },
                                reminderOffsetsMinutes = offsets
                            )

                            bookings.add(booking)
                            bookings.sortBy { it.dateTimeMillis }
                            BookingStore.saveAll(this@MainActivity, bookings)
                            ReminderScheduler.schedule(this@MainActivity, booking)

                            refreshBookings()
                            dialog.dismiss()

                            Toast.makeText(
                                this@MainActivity,
                                "Session saved with ${offsets.size} reminder(s).",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun addFormField(
        form: LinearLayout,
        label: String,
        field: EditText
    ) {
        form.addView(text(label, 10f, green, true))
        form.addView(space(5))
        form.addView(field)
        form.addView(space(12))
    }

    private fun pickerButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(darkGreen)
            background = rounded(paleSage, 14)
            elevation = 0f
        }
    }

    private fun updatePickerLabels(dateButton: Button, timeButton: Button) {
        dateButton.text = "▦   ${dateLabel(selectedDateTime.timeInMillis)}"
        timeButton.text = "◷   ${timeLabel(selectedDateTime)}"
    }

    private fun parseReminderOffsets(raw: String): List<Int>? {
        if (raw.isBlank()) return emptyList()

        val result = mutableListOf<Int>()
        val entries = raw.split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (entry in entries) {
            val match = Regex(
                """^(\d+)\s*(w|weeks?|d|days?|h|hours?|m|minutes?)$"""
            ).matchEntire(entry.lowercase()) ?: return null

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
            setTextColor(darkText)
            setHintTextColor(mutedText)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(cream, 13)
            minHeight = dp(48)
            backgroundTintList = null
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
                setPadding(dp(22), dp(30), dp(22), dp(30))
                background = rounded(white, 22)
                elevation = dp(1).toFloat()
            }

            val paw = text("🐾", 30f, green)
            paw.gravity = Gravity.CENTER
            emptyCard.addView(paw)
            emptyCard.addView(space(10))

            val emptyTitle = when (selectedFilter) {
                "Upcoming" -> "Nothing on the calendar yet"
                "Past" -> "No past sessions to show"
                else -> "Your session list is empty"
            }

            emptyCard.addView(text(emptyTitle, 17f, darkGreen, true).apply {
                gravity = Gravity.CENTER
            })
            emptyCard.addView(space(7))
            emptyCard.addView(
                text(
                    "Your next favourite face could be one tap away. Add a session to get started.",
                    13f,
                    mutedText
                ).apply {
                    gravity = Gravity.CENTER
                }
            )
            listContainer.addView(emptyCard)
            animateIn(emptyCard)
            return
        }

        filtered.forEachIndexed { index, booking ->
            val card = createBookingCard(booking)
            listContainer.addView(card)
            listContainer.addView(space(12))
            animateIn(card, index * 35L)
        }
    }

    private fun createBookingCard(booking: Booking): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(17), dp(17), dp(15))
            background = rounded(white, 21)
            elevation = dp(2).toFloat()
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val dogName = text(booking.dogName, 21f, darkGreen, true)
        topRow.addView(
            dogName,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val isPast = booking.dateTimeMillis < System.currentTimeMillis()
        val badge = TextView(this).apply {
            text = if (isPast) "PAST SESSION" else "UPCOMING"
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isPast) mutedText else darkGreen)
            setPadding(dp(9), dp(7), dp(9), dp(7))
            background = rounded(if (isPast) pink else sage, 12)
        }
        topRow.addView(badge)
        card.addView(topRow)
        card.addView(space(5))

        val typePill = TextView(this).apply {
            text = "📷  ${booking.sessionType}"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
        }
        card.addView(typePill)
        card.addView(space(12))

        val divider = View(this).apply {
            setBackgroundColor(cream)
        }
        card.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
        card.addView(space(11))

        card.addView(text("CLIENT", 10f, mutedText, true))
        card.addView(space(3))
        card.addView(text(booking.clientName, 14f, darkText, true))
        card.addView(space(10))

        val dateLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        dateLine.addView(text("▦", 17f, green))
        dateLine.addView(
            text(
                "${dateLabel(booking.dateTimeMillis)}   ·   ${timeLabel(booking.dateTimeMillis)}",
                12f,
                darkText,
                true
            ),
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = dp(8)
            }
        )
        card.addView(dateLine)

        if (booking.location.isNotBlank()) {
            card.addView(space(8))
            card.addView(text("⌖  ${booking.location}", 12f, mutedText))
        }

        if (booking.notes.isNotBlank()) {
            card.addView(space(7))
            card.addView(text("Notes: ${booking.notes}", 12f, mutedText))
        }

        card.addView(space(12))

        val reminderPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(10), dp(11), dp(10))
            background = rounded(paleSage, 13)
        }
        reminderPanel.addView(
            text(
                "◷  ${booking.reminderOffsetsMinutes.size} reminder(s) scheduled",
                11f,
                darkGreen,
                true
            )
        )
        reminderPanel.addView(space(3))
        reminderPanel.addView(
            text(
                formatReminderOffsets(booking.reminderOffsetsMinutes),
                11f,
                mutedText
            )
        )
        card.addView(reminderPanel)
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
            background = rounded(pink, 13)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete session?")
                    .setMessage(
                        "Delete the photography session for ${booking.dogName}?"
                    )
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

        actions.addView(deleteButton, LinearLayout.LayoutParams(-2, dp(43)))
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

    private fun animateIn(view: View, delay: Long = 0L) {
        view.alpha = 0f
        view.translationY = dp(12).toFloat()
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun tapFeedback(view: View) {
        view.animate()
            .scaleX(0.985f)
            .scaleY(0.985f)
            .setDuration(75)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
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

    private fun roundedGradient(
        startColour: Int,
        endColour: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColour, endColour)
        ).apply {
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

    /**
     * Drawn directly in Android: no image download or extra library required.
     * A camera outline and paw-print details form the app's brand mark.
     */
    private class CameraPawLogo(context: Context) : View(context) {

        private val dark = Color.rgb(39, 76, 59)
        private val green = Color.rgb(69, 119, 85)
        private val cream = Color.rgb(249, 248, 243)

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpValue(2.2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val w = width.toFloat()
            val h = height.toFloat()
            val size = minOf(w, h)
            val left = (w - size) / 2f
            val top = (h - size) / 2f
            val scale = size / 62f

            canvas.save()
            canvas.translate(left, top)
            canvas.scale(scale, scale)

            fillPaint.color = cream
            canvas.drawRoundRect(
                RectF(0f, 0f, 62f, 62f),
                19f,
                19f,
                fillPaint
            )

            // Camera body.
            strokePaint.color = dark
            canvas.drawRoundRect(
                RectF(10f, 17f, 52f, 47f),
                7f,
                7f,
                strokePaint
            )

            // Camera top.
            val topPath = android.graphics.Path().apply {
                moveTo(19f, 17f)
                lineTo(23f, 11f)
                lineTo(37f, 11f)
                lineTo(41f, 17f)
            }
            canvas.drawPath(topPath, strokePaint)

            // Lens.
            strokePaint.color = green
            canvas.drawCircle(31f, 32f, 9f, strokePaint)
            fillPaint.color = green
            canvas.drawCircle(31f, 32f, 4f, fillPaint)

            // Tiny paw detail in the upper-right corner.
            fillPaint.color = orange
            canvas.drawCircle(47f, 9f, 3.2f, fillPaint)
            canvas.drawCircle(53f, 13f, 3f, fillPaint)
            canvas.drawCircle(51f, 20f, 3f, fillPaint)
            canvas.drawCircle(44f, 20f, 3f, fillPaint)

            canvas.restore()
        }

        private fun dpValue(value: Float): Float {
            return value * resources.displayMetrics.density
        }
    }
}
