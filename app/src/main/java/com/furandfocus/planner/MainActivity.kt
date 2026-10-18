package com.furandfocus.planner

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
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
    private var selectedDateTime = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val background = Color.rgb(248, 245, 240)
    private val ink = Color.rgb(47, 54, 47)
    private val green = Color.rgb(69, 103, 79)
    private val muted = Color.rgb(116, 119, 109)
    private val white = Color.WHITE

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
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(background)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
        }

        val brand = TextView(this).apply {
            text = "FUR & FOCUS"
            textSize = 13f
            letterSpacing = 0.22f
            setTextColor(green)
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(brand)

        val title = TextView(this).apply {
            text = "Photography planner"
            textSize = 28f
            setTextColor(ink)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Every session, beautifully organised."
            textSize = 15f
            setTextColor(muted)
        }
        root.addView(subtitle)

        countLabel = TextView(this).apply {
            textSize = 16f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(26), 0, dp(18))
        }
        root.addView(countLabel)

        val addButton = Button(this).apply {
            text = "+  Add photography session"
            isAllCaps = false
            textSize = 16f
            setTextColor(white)
            setBackgroundColor(green)
            setOnClickListener { showAddBookingDialog() }
        }
        root.addView(
            addButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        )

        val section = TextView(this).apply {
            text = "YOUR SESSIONS"
            textSize = 12f
            letterSpacing = 0.12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(muted)
            setPadding(0, dp(28), 0, dp(12))
        }
        root.addView(section)

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(listContainer)

        val footer = TextView(this).apply {
            text = "Fur & Focus Pet Photography"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, dp(30), 0, dp(10))
        }
        root.addView(footer)

        scroll.addView(root)
        setContentView(scroll)
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
                        selectedDateTime.set(year, month, day)
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
                            "Please enter both the client and dog's name.",
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
            setSingleLine(hintText.contains("name") || hintText.contains("location"))
        }
    }

    private fun refreshBookings() {
        listContainer.removeAllViews()

        val upcoming = bookings
            .filter { it.dateTimeMillis >= System.currentTimeMillis() }
            .sortedBy { it.dateTimeMillis }

        countLabel.text = when (upcoming.size) {
            0 -> "No upcoming sessions"
            1 -> "1 upcoming session"
            else -> "${upcoming.size} upcoming sessions"
        }

        if (upcoming.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Your next session will appear here.\\nTap the button above to add a booking."
                textSize = 15f
                setTextColor(muted)
                setPadding(dp(18), dp(24), dp(18), dp(24))
                setBackgroundColor(white)
            }
            listContainer.addView(empty)
            return
        }

        upcoming.forEach { booking ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(14))
                setBackgroundColor(white)
            }

            val date = TextView(this).apply {
                text = dateLabel(booking.dateTimeMillis) +
                    "  ·  " + timeLabel(
                        Calendar.getInstance().apply {
                            timeInMillis = booking.dateTimeMillis
                        }
                    )
                textSize = 13f
                setTextColor(green)
                typeface = Typeface.DEFAULT_BOLD
            }
            card.addView(date)

            val dog = TextView(this).apply {
                text = booking.dogName
                textSize = 21f
                setTextColor(ink)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(8), 0, dp(2))
            }
            card.addView(dog)

            val client = TextView(this).apply {
                text = "Client: ${booking.clientName}"
                textSize = 14f
                setTextColor(muted)
            }
            card.addView(client)

            if (booking.location.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = "Location: ${booking.location}"
                    textSize = 14f
                    setTextColor(muted)
                    setPadding(0, dp(4), 0, 0)
                })
            }

            if (booking.notes.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = booking.notes
                    textSize = 14f
                    setTextColor(ink)
                    setPadding(0, dp(8), 0, 0)
                })
            }

            val delete = Button(this).apply {
                text = "Delete session"
                isAllCaps = false
                setOnClickListener {
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Delete this session?")
                        .setMessage("The booking and its reminders will be removed.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ ->
                            ReminderScheduler.cancel(this@MainActivity, booking)
                            bookings.removeAll { it.id == booking.id }
                            BookingStore.saveAll(this@MainActivity, bookings)
                            refreshBookings()
                        }
                        .show()
                }
            }
            card.addView(delete)

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dp(12)
            listContainer.addView(card, params)
        }
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
