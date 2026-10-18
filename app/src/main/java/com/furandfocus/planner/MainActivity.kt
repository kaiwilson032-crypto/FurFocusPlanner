package com.furandfocus.planner

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val bookings = mutableListOf<Booking>()
    private lateinit var listContainer: LinearLayout
    private lateinit var countLabel: TextView
    private lateinit var monthCount: TextView
    private lateinit var filterRow: LinearLayout
    private var selectedFilter = "Upcoming"

    private var selectedDateTime = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val cream = Color.rgb(248, 246, 240)
    private val white = Color.WHITE
    private val ink = Color.rgb(43, 54, 44)
    private val green = Color.rgb(67, 100, 76)
    private val paleGreen = Color.rgb(230, 237, 226)
    private val muted = Color.rgb(123, 127, 116)
    private val border = Color.rgb(230, 229, 219)
    private val gold = Color.rgb(190, 153, 94)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookings.addAll(BookingStore.getAll(this))
        requestNotificationPermission()
        ReminderScheduler.scheduleAll(this, bookings)

        buildScreen()
        refreshBookings()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(cream)
            clipToPadding = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "F"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(green, 15)
        }

        brandRow.addView(logo, LinearLayout.LayoutParams(dp(46), dp(46)))

        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        brandText.addView(text(
            "FUR & FOCUS",
            13f,
            green,
            true
        ).apply {
            letterSpacing = 0.18f
        })

        brandText.addView(text(
            "PET PHOTOGRAPHY",
            10f,
            muted,
            true
        ).apply {
            letterSpacing = 0.12f
            setPadding(0, dp(3), 0, 0)
        })

        brandRow.addView(
            brandText,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        val today = text(
            SimpleDateFormat("EEE, d MMM", Locale.UK).format(Date()),
            12f,
            muted,
            true
        )
        today.gravity = Gravity.CENTER
        today.background = rounded(white, 18)
        today.setPadding(dp(12), dp(10), dp(12), dp(10))
        brandRow.addView(today)

        root.addView(brandRow)

        val heading = text("Your studio.", 32f, ink, true)
        heading.setPadding(0, dp(28), 0, dp(5))
        root.addView(heading)

        root.addView(text(
            "All your sessions, in one place.",
            15f,
            muted
        ))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(green, 24)
            setPadding(dp(22), dp(22), dp(22), dp(22))
        }

        val heroEyebrow = text("YOUR PHOTOGRAPHY DIARY", 11f, paleGreen, true)
        heroEyebrow.letterSpacing = 0.12f
        hero.addView(heroEyebrow)

        countLabel = text("Ready for your next session?", 23f, white, true)
        countLabel.setPadding(0, dp(12), 0, dp(7))
        hero.addView(countLabel)

        hero.addView(text(
            "Keep track of every client and every wagging tail.",
            13f,
            Color.rgb(224, 232, 219)
        ))

        val heroButton = Button(this).apply {
            text = "+   Add a session"
            isAllCaps = false
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            background = rounded(cream, 15)
            setOnClickListener { showAddBookingDialog() }
        }

        val heroButtonParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(52)
        )
        heroButtonParams.topMargin = dp(20)
        hero.addView(heroButton, heroButtonParams)

        val heroParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        heroParams.topMargin = dp(25)
        root.addView(hero, heroParams)

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val upcomingCard = statCard(
            "UPCOMING",
            "0",
            "sessions booked"
        )
        monthCount = upcomingCard.second

        val monthCard = statCard(
            "THIS MONTH",
            "0",
            "sessions scheduled"
        )

        statsRow.addView(
            upcomingCard.first,
            LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                marginEnd = dp(7)
            }
        )
        statsRow.addView(
            monthCard.first,
            LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                marginStart = dp(7)
            }
        )

        val statsParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        statsParams.topMargin = dp(14)
        root.addView(statsRow, statsParams)

        val sectionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(30), 0, dp(14))
        }

        sectionRow.addView(
            text("Your sessions", 21f, ink, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        sectionRow.addView(text(
            "BOOKINGS",
            10f,
            green,
            true
        ).apply {
            letterSpacing = 0.1f
        })

        root.addView(sectionRow)

        filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        root.addView(filterRow)
        refreshFilters()

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val listParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        listParams.topMargin = dp(14)
        root.addView(listContainer, listParams)

        root.addView(text(
            "Made with care for every furry face.",
            12f,
            muted
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, dp(8))
        })

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun statCard(
        label: String,
        initialValue: String,
        caption: String
    ): Pair<LinearLayout, TextView> {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(white, 20)
            setPadding(dp(16), dp(15), dp(12), dp(12))
        }

        card.addView(text(label, 10f, muted, true).apply {
            letterSpacing = 0.06f
        })

        val number = text(initialValue, 27f, green, true)
        number.setPadding(0, dp(7), 0, dp(1))
        card.addView(number)

        card.addView(text(caption, 11f, muted))

        return Pair(card, number)
    }

    private fun refreshFilters() {
        if (!::filterRow.isInitialized) return
        filterRow.removeAllViews()

        listOf("Upcoming", "Past", "All").forEach { filter ->
            val selected = filter == selectedFilter
            val chip = TextView(this).apply {
                text = filter
                textSize = 13f
                gravity = Gravity.CENTER
                typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (selected) white else muted)
                background = rounded(if (selected) green else white, 18)
                setPadding(dp(17), dp(11), dp(17), dp(11))
                setOnClickListener {
                    selectedFilter = filter
                    refreshFilters()
                    refreshBookings()
                }
            }

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = dp(8)
            filterRow.addView(chip, params)
        }
    }

    private fun showAddBookingDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        val clientInput = input("Client's name")
        val dogInput = input("Dog's name")
        val locationInput = input("Session location (optional)")
        val notesInput = input("Notes (optional)")

        form.addView(clientInput)
        form.addView(dogInput)
        form.addView(locationInput)
        form.addView(notesInput)

        val dateButton = Button(this).apply {
            isAllCaps = false
            text = "Date: ${dateLabel(selectedDateTime.timeInMillis)}"
            setOnClickListener {
                DatePickerDialog(
                    this@MainActivity,
                    { _, year, month, day ->
                        selectedDateTime.set(
                            Calendar.YEAR,
                            year
                        )
                        selectedDateTime.set(Calendar.MONTH, month)
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, day)
                        text = "Date: ${dateLabel(selectedDateTime.timeInMillis)}"
                    },
                    selectedDateTime.get(Calendar.YEAR),
                    selectedDateTime.get(Calendar.MONTH),
                    selectedDateTime.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
        form.addView(dateButton)

        val timeButton = Button(this).apply {
            isAllCaps = false
            text = "Time: ${timeLabel(selectedDateTime)}"
            setOnClickListener {
                TimePickerDialog(
                    this@MainActivity,
                    { _, hour, minute ->
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                        selectedDateTime.set(Calendar.MINUTE, minute)
                        text = "Time: ${timeLabel(selectedDateTime)}"
                    },
                    selectedDateTime.get(Calendar.HOUR_OF_DAY),
                    selectedDateTime.get(Calendar.MINUTE),
                    true
                ).show()
            }
        }
        form.addView(timeButton)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("New photography session")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save session", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val client = clientInput.text.toString().trim()
                    val dog = dogInput.text.toString().trim()

                    if (client.isBlank() || dog.isBlank()) {
                        Toast.makeText(
                            this,
                            "Please enter both names.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    val booking = Booking(
                        clientName = client,
                        dogName = dog,
                        dateTimeMillis = selectedDateTime.timeInMillis,
                        location = locationInput.text.toString().trim(),
                        notes = notesInput.text.toString().trim(),
                        reminderDays = 1,
                        reminderHoursBefore = 24
                    )

                    bookings.add(booking)
                    bookings.sortBy { it.dateTimeMillis }
                    BookingStore.saveAll(this, bookings)
                    ReminderScheduler.schedule(this, booking)
                    refreshBookings()
                    dialog.dismiss()
                    Toast.makeText(this, "Session saved", Toast.LENGTH_SHORT).show()
                }
        }
        dialog.show()
    }

    private fun input(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 15f
            setSingleLine(
                hintText.contains("name") ||
                    hintText.contains("location")
            )
        }
    }

    private fun refreshBookings() {
        if (!::listContainer.isInitialized) return

        listContainer.removeAllViews()

        val now = System.currentTimeMillis()
        val all = bookings.sortedBy { it.dateTimeMillis }
        val upcoming = all.filter { it.dateTimeMillis >= now }
        val thisMonth = Calendar.getInstance()

        val monthTotal = upcoming.count {
            val date = Calendar.getInstance().apply {
                timeInMillis = it.dateTimeMillis
            }
            date.get(Calendar.YEAR) == thisMonth.get(Calendar.YEAR) &&
                date.get(Calendar.MONTH) == thisMonth.get(Calendar.MONTH)
        }

        countLabel.text = when (upcoming.size) {
            0 -> "Ready for your next session?"
            1 -> "1 session on the horizon"
            else -> "${upcoming.size} sessions on the horizon"
        }

        // The two dashboard figures sit in separate cards.
        val stats = findStatNumbers()
        if (stats.size >= 2) {
            stats[0].text = upcoming.size.toString()
            stats[1].text = monthTotal.toString()
        }

        val shown = when (selectedFilter) {
            "Past" -> all.filter { it.dateTimeMillis < now }.reversed()
            "All" -> all
            else -> upcoming
        }

        if (shown.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(white, 22)
                setPadding(dp(22), dp(30), dp(22), dp(30))
            }

            val symbol = text("✦", 30f, gold, true)
            symbol.gravity = Gravity.CENTER
            empty.addView(symbol)

            val title = when (selectedFilter) {
                "Past" -> "No past sessions"
                "All" -> "Your diary is empty"
                else -> "Nothing booked just yet"
            }

            empty.addView(text(title, 17f, ink, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(6))
            })

            empty.addView(text(
                if (selectedFilter == "Past")
                    "Completed sessions will appear here."
                else
                    "Add your first session and start building your diary.",
                13f,
                muted
            ).apply {
                gravity = Gravity.CENTER
            })

            listContainer.addView(empty)
            return
        }

        shown.forEach { booking ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(white, 22)
                setPadding(dp(18), dp(18), dp(18), dp(16))
            }

            val datePill = TextView(this).apply {
                text = dateLabel(booking.dateTimeMillis).uppercase(Locale.UK) +
                    "   ·   " + timeLabel(
                    Calendar.getInstance().apply {
                        timeInMillis = booking.dateTimeMillis
                    }
                )
                textSize = 11f
                letterSpacing = 0.03f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(green)
                background = rounded(paleGreen, 12)
                setPadding(dp(11), dp(10), dp(11), dp(10))
            }
            card.addView(datePill)

            val dog = text(booking.dogName, 23f, ink, true)
            dog.setPadding(0, dp(15), 0, dp(4))
            card.addView(dog)

            card.addView(text(
                "CLIENT  ·  ${booking.clientName}",
                12f,
                muted,
                true
            ))

            if (booking.location.isNotBlank()) {
                card.addView(text(
                    "⌖  ${booking.location}",
                    13f,
                    muted
                ).apply {
                    setPadding(0, dp(10), 0, 0)
                })
            }

            if (booking.notes.isNotBlank()) {
                card.addView(text(
                    booking.notes,
                    13f,
                    ink
                ).apply {
                    setPadding(0, dp(10), 0, 0)
                })
            }

            val delete = TextView(this).apply {
                text = "Delete booking"
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(muted)
                background = rounded(cream, 12)
                setPadding(dp(14), dp(11), dp(14), dp(11))
                setOnClickListener {
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Delete this session?")
                        .setMessage("The booking and its reminders will be removed.")
                        .setNegativeButton("Keep booking", null)
                        .setPositiveButton("Delete") { _, _ ->
                            ReminderScheduler.cancel(this@MainActivity, booking)
                            bookings.removeAll { it.id == booking.id }
                            BookingStore.saveAll(this@MainActivity, bookings)
                            refreshBookings()
                        }
                        .show()
                }
            }

            val deleteParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            deleteParams.topMargin = dp(16)
            card.addView(delete, deleteParams)

            val cardParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            cardParams.bottomMargin = dp(13)
            listContainer.addView(card, cardParams)
        }
    }

    private fun findStatNumbers(): List<TextView> {
        val results = mutableListOf<TextView>()
        val scroll = findViewById<ScrollView>(android.R.id.content)
        collectStatNumbers(scroll, results)
        return results
    }

    private fun collectStatNumbers(view: View, results: MutableList<TextView>) {
        if (view is TextView && view.text.toString() in listOf("0", "1", "2", "3", "4", "5")) {
            if (view.textSize > 20f) results.add(view)
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectStatNumbers(view.getChildAt(i), results)
            }
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dateLabel(timeMillis: Long): String =
        SimpleDateFormat("EEE d MMM yyyy", Locale.UK).format(Date(timeMillis))

    private fun timeLabel(calendar: Calendar): String =
        String.format(
            Locale.UK,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
