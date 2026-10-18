package com.furandfocus.planner

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val ink = Color.rgb(48, 54, 49)
    private val green = Color.rgb(83, 111, 86)
    private val cream = Color.rgb(250, 247, 240)
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        ReminderScheduler.scheduleAll(this)
        render()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }

    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(cream) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(28))
        }
        scroll.addView(page)
        val brand = TextView(this).apply {
            text = "FUR & FOCUS"
            textSize = 12f
            letterSpacing = .18f
            setTextColor(green)
            typeface = Typeface.DEFAULT_BOLD
        }
        page.addView(brand)
        page.addView(TextView(this).apply {
            text = "Photography Planner"
            textSize = 29f
            setTextColor(ink)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, dp(3), 0, dp(5))
        })
        page.addView(TextView(this).apply {
            text = "Your shoots, organised."
            textSize = 15f
            setTextColor(Color.DKGRAY)
        })
        val add = Button(this).apply {
            text = "+  Add a booking"
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener { showAddDialog() }
        }
        val lp = LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(18); bottomMargin = dp(20) }
        page.addView(add, lp)
        val now = System.currentTimeMillis()
        val upcoming = BookingStore.all(this).filter { it.dateTime >= now }
        val completed = BookingStore.all(this).filter { it.dateTime < now }
        page.addView(sectionTitle("UPCOMING SHOOTS  ·  ${upcoming.size}"))
        if (upcoming.isEmpty()) page.addView(emptyText("No shoots booked yet. Add your first client above."))
        upcoming.forEach { page.addView(bookingCard(it)) }
        page.addView(sectionTitle("PAST SHOOTS  ·  ${completed.size}"))
        if (completed.isEmpty()) page.addView(emptyText("Completed or past bookings will appear here."))
        completed.forEach { page.addView(bookingCard(it)) }
        setContentView(scroll)
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 12f
        letterSpacing = .1f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(green)
        setPadding(0, dp(10), 0, dp(10))
    }

    private fun emptyText(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setTextColor(Color.GRAY)
        setPadding(dp(2), dp(4), dp(2), dp(18))
    }

    private fun bookingCard(b: Booking): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        val date = SimpleDateFormat("EEE d MMM yyyy  •  HH:mm", Locale.getDefault()).format(Date(b.dateTime))
        card.addView(TextView(this).apply {
            text = b.dog.ifBlank { "Photo session" }
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })
        card.addView(TextView(this).apply {
            text = "${b.client}  ·  $date"
            textSize = 13f
            setTextColor(green)
            setPadding(0, dp(4), 0, dp(6))
        })
        if (b.location.isNotBlank()) card.addView(TextView(this).apply {
            text = "Location: ${b.location}"
            textSize = 14f; setTextColor(ink)
        })
        if (b.notes.isNotBlank()) card.addView(TextView(this).apply {
            text = b.notes
            textSize = 13f; setTextColor(Color.DKGRAY)
            setPadding(0, dp(4), 0, 0)
        })
        val remaining = b.dateTime - System.currentTimeMillis()
        if (remaining > 0) {
            val days = remaining / 86400000L
            card.addView(TextView(this).apply {
                text = if (days == 0L) "Shoot is today" else "$days day${if (days == 1L) "" else "s"} to go"
                textSize = 12f; setTextColor(green)
                setPadding(0, dp(6), 0, 0)
            })
        }
        val remove = TextView(this).apply {
            text = "Delete booking"
            textSize = 12f
            setTextColor(Color.rgb(160, 65, 65))
            gravity = Gravity.END
            setPadding(0, dp(10), 0, 0)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete this booking?")
                    .setMessage("Its scheduled reminders will also be cancelled.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        ReminderScheduler.cancel(this@MainActivity, b)
                        val all = BookingStore.all(this@MainActivity)
                        BookingStore.save(this@MainActivity, all.filterNot { it.id == b.id })
                        render()
                    }.show()
            }
        }
        card.addView(remove)
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
            addView(card)
        }
        return wrapper
    }

    private fun showAddDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
        }
        fun field(hint: String): EditText {
            val e = EditText(this)
            e.hint = hint
            e.textSize = 15f
            form.addView(e, LinearLayout.LayoutParams(-1, dp(48)))
            return e
        }
        val client = field("Client's full name *")
        val dog = field("Dog's name *")
        val location = field("Shoot location")
        val notes = field("Notes / special requirements")
        val chosen = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        val dateButton = Button(this).apply { text = dateTimeLabel(chosen.timeInMillis) }
        form.addView(dateButton)
        dateButton.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                chosen.set(Calendar.YEAR, y); chosen.set(Calendar.MONTH, m); chosen.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(this, { _, h, min ->
                    chosen.set(Calendar.HOUR_OF_DAY, h); chosen.set(Calendar.MINUTE, min)
                    dateButton.text = dateTimeLabel(chosen.timeInMillis)
                }, chosen.get(Calendar.HOUR_OF_DAY), chosen.get(Calendar.MINUTE), true).show()
                dateButton.text = dateTimeLabel(chosen.timeInMillis)
            }, chosen.get(Calendar.YEAR), chosen.get(Calendar.MONTH), chosen.get(Calendar.DAY_OF_MONTH)).show()
        }
        AlertDialog.Builder(this)
            .setTitle("New photography booking")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save booking", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (client.text.toString().trim().isEmpty() || dog.text.toString().trim().isEmpty()) {
                            Toast.makeText(this, "Enter the client and dog's name.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        if (chosen.timeInMillis <= System.currentTimeMillis()) {
                            Toast.makeText(this, "Choose a future date and time.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val booking = Booking(
                            id = System.currentTimeMillis(),
                            client = client.text.toString().trim(),
                            dog = dog.text.toString().trim(),
                            dateTime = chosen.timeInMillis,
                            location = location.text.toString().trim(),
                            notes = notes.text.toString().trim()
                        )
                        val all = BookingStore.all(this).apply { add(booking) }
                        BookingStore.save(this, all)
                        ReminderScheduler.schedule(this, booking)
                        Toast.makeText(this, "Booking saved. Reminders scheduled.", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        render()
                    }
                }
                dialog.show()
            }
    }

    private fun dateTimeLabel(millis: Long) =
        "Shoot: " + SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
