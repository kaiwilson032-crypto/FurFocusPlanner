
package com.furandfocus.planner

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/** Minimum time the loading screen stays up, so the animation never flashes past. */
private const val MIN_LOADER_MS = 1100L

private val TAB_ORDER = listOf("Bookings", "Reminders", "Calendar", "Overview", "Settings")

class MainActivity : Activity() {

    private val bookings = mutableListOf<Booking>()

    // Loads saved bookings off the main thread so the loading animation can run smoothly.
    private val loader = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loadingAnimators = mutableListOf<ValueAnimator>()
    private var loadingView: View? = null
    private lateinit var rootFrame: FrameLayout

    private lateinit var listContainer: LinearLayout
    private lateinit var countLabel: TextView
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
    private val rippleColour = Color.argb(38, 39, 76, 59)

    // Typography: built-in Android families, so no font files need bundling.
    private val fontRegular: Typeface by lazy { Typeface.create("sans-serif", Typeface.NORMAL) }
    private val fontMedium: Typeface by lazy { Typeface.create("sans-serif-medium", Typeface.NORMAL) }

    private lateinit var screenHost: LinearLayout
    private lateinit var navBar: LinearLayout
    private var selectedTab = "Bookings"
    private var calendarMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveNavigation()

        rootFrame = FrameLayout(this).apply { setBackgroundColor(cream) }
        setContentView(rootFrame)
        loadingView = buildLoader().also {
            rootFrame.addView(it, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }

        requestNotificationPermissionIfNeeded()
        loadBookingsInBackground()
    }

    override fun onDestroy() {
        loadingAnimators.forEach { it.cancel() }
        loadingAnimators.clear()
        mainHandler.removeCallbacksAndMessages(null)
        loader.shutdownNow()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun enableImmersiveNavigation() {
        // Hide Android's system navigation bar so it cannot cover the app's bottom tabs.
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveNavigation()
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

    // ---------------------------------------------------------------- Loading

    private fun loadBookingsInBackground() {
        val startedAt = System.currentTimeMillis()
        val appContext = applicationContext

        loader.execute {
            val loaded: MutableList<Booking> = BookingStore.getAll(appContext)
            ReminderScheduler.scheduleAll(appContext, loaded)

            // Keep the loader up long enough for the animation to feel intentional.
            val remaining = MIN_LOADER_MS - (System.currentTimeMillis() - startedAt)
            if (remaining > 0) {
                try {
                    Thread.sleep(remaining)
                } catch (_: InterruptedException) {
                    return@execute
                }
            }

            mainHandler.post {
                if (isFinishing || isDestroyed) return@post
                bookings.clear()
                bookings.addAll(loaded)
                buildScreen()
                hideLoader()
            }
        }
    }

    private fun buildLoader(): FrameLayout {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(cream)
            isClickable = true // swallow taps until the app is ready
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val logo = CameraPawLogo(this)
        column.addView(logo, LinearLayout.LayoutParams(dp(88), dp(88)))
        column.addView(space(20))
        column.addView(caps("FUR & FOCUS", 14f, darkGreen).apply { gravity = Gravity.CENTER })
        column.addView(space(6))
        column.addView(text("Loading your sessions…", 13f, mutedText).apply { gravity = Gravity.CENTER })
        column.addView(space(22))

        val dotRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val dots = List(3) {
            View(this).apply { background = oval(green) }
        }
        dots.forEach { dot ->
            dotRow.addView(dot, LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                setMargins(dp(4), 0, dp(4), 0)
            })
        }
        column.addView(dotRow)

        overlay.addView(
            column,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        if (motionEnabled()) {
            // Logo breathes gently in and out.
            loadingAnimators += ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1200L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val f = it.animatedValue as Float
                    val scale = 0.92f + 0.10f * f
                    logo.scaleX = scale
                    logo.scaleY = scale
                    logo.alpha = 0.85f + 0.15f * f
                }
                start()
            }

            // Three dots take turns bouncing, like a gentle wave.
            loadingAnimators += ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val f = it.animatedValue as Float
                    dots.forEachIndexed { i, dot ->
                        val phase = (f + 1f - i * 0.18f) % 1f
                        val lift = maxOf(0f, sin(phase * 2 * PI).toFloat())
                        dot.translationY = -dp(8) * lift
                        dot.alpha = 0.3f + 0.7f * lift
                    }
                }
                start()
            }
        }

        return overlay
    }

    private fun hideLoader() {
        val overlay = loadingView ?: return
        loadingView = null
        loadingAnimators.forEach { it.cancel() }
        loadingAnimators.clear()

        if (!motionEnabled()) {
            rootFrame.removeView(overlay)
            return
        }

        overlay.animate()
            .alpha(0f)
            .scaleX(1.06f)
            .scaleY(1.06f)
            .setDuration(480)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { rootFrame.removeView(overlay) }
            .start()
    }

    // ---------------------------------------------------------------- Shell

    private fun buildScreen() {
        val appRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cream)
        }

        screenHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cream)
        }
        appRoot.addView(
            screenHost,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(7), dp(6), dp(7))
            setBackgroundColor(white)
            elevation = dp(10).toFloat()
        }
        appRoot.addView(
            navBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68)
            )
        )

        // Added beneath the loader so the loader fades away to reveal it.
        rootFrame.addView(
            appRoot,
            0,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        showTab("Bookings")

        if (motionEnabled()) {
            appRoot.alpha = 0f
            appRoot.translationY = dp(18).toFloat()
            appRoot.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(560)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun showTab(tab: String) {
        val previous = selectedTab
        selectedTab = tab
        screenHost.removeAllViews()

        when (tab) {
            "Bookings" -> buildBookingsScreen()
            "Reminders" -> buildRemindersScreen()
            "Calendar" -> buildCalendarScreen()
            "Overview" -> buildOverviewScreen()
            "Settings" -> buildSettingsScreen()
        }

        buildBottomNavigation()

        val forward = TAB_ORDER.indexOf(tab) >= TAB_ORDER.indexOf(previous)
        screenHost.post {
            screenHost.getChildAt(0)?.let { child ->
                if (previous == tab) animateIn(child) else slideTabIn(child, forward)
            }
        }
    }

    private fun slideTabIn(view: View, forward: Boolean) {
        if (!motionEnabled()) return
        view.alpha = 0f
        view.translationX = (if (forward) 1 else -1) * dp(32).toFloat()
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(380)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()
    }

    private fun buildBottomNavigation() {
        navBar.removeAllViews()
        val items = listOf(
            Triple("Bookings", "▦", "Bookings"),
            Triple("Reminders", "◷", "Reminders"),
            Triple("Calendar", "▦", "Calendar"),
            Triple("Overview", "▥", "Overview"),
            Triple("Settings", "⚙", "Settings")
        )

        items.forEach { (tab, symbol, title) ->
            val selected = selectedTab == tab
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(4), dp(3), dp(4))
                isClickable = true
                isFocusable = true
                background = rippled(rounded(if (selected) paleSage else white, 15))
                setOnClickListener {
                    if (selectedTab != tab) showTab(tab)
                }
            }
            attachPress(item)

            val icon = text(symbol, 20f, if (selected) green else mutedText, true)
            icon.gravity = Gravity.CENTER
            item.addView(icon)
            if (selected) popIn(icon)

            val titleView = text(title, 10f, if (selected) darkGreen else mutedText, selected)
            titleView.gravity = Gravity.CENTER
            item.addView(titleView)

            navBar.addView(
                item,
                LinearLayout.LayoutParams(0, -1, 1f).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
            )
        }
    }

    private fun screenScroll(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(cream)
            isFillViewport = true
            clipToPadding = false
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(24))
        }
        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        screenHost.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )
        return Pair(scroll, root)
    }

    private fun addBrandHeader(root: LinearLayout) {
        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brandRow.addView(CameraPawLogo(this), LinearLayout.LayoutParams(dp(54), dp(54)))

        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brandText.addView(headline("FUR & FOCUS", 18f, darkGreen))
        brandText.addView(space(3))
        brandText.addView(caps("PHOTOGRAPHY PLANNER", 10f, mutedText))
        brandRow.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))

        val liveTag = TextView(this).apply {
            text = "● PLANNER"
            textSize = 10f
            typeface = fontMedium
            letterSpacing = 0.08f
            setTextColor(green)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(paleSage, 20)
        }
        brandRow.addView(liveTag)
        root.addView(brandRow)
        root.addView(space(22))
    }

    // ---------------------------------------------------------------- Bookings

    private fun buildBookingsScreen() {
        val (_, root) = screenScroll()
        addBrandHeader(root)

        root.addView(caps("YOUR SESSIONS", 12f, green))
        root.addView(space(4))
        root.addView(headline("Every wag deserves a little spotlight.", 25f, darkText))
        root.addView(space(7))
        root.addView(text("Manage your clients and upcoming photography sessions.", 13f, mutedText))
        root.addView(space(17))

        val addButton = Button(this).apply {
            text = "＋   New photography session"
            textSize = 14f
            isAllCaps = false
            setTextColor(white)
            typeface = fontMedium
            background = roundedGradient(darkGreen, green, 16)
            setOnClickListener { showAddBookingDialog() }
        }
        attachPress(addButton)
        root.addView(addButton, LinearLayout.LayoutParams(-1, dp(53)))
        root.addView(space(23))

        val headingRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headingRow.addView(
            headline("Your sessions", 23f, darkText),
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        countLabel = text("0 sessions", 12f, mutedText)
        headingRow.addView(countLabel)
        root.addView(headingRow)
        root.addView(space(5))
        root.addView(text("All your favourite faces, neatly organised.", 13f, mutedText))
        root.addView(space(16))

        filterRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(filterRow)
        root.addView(space(15))
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listContainer)

        refreshFilters()
        refreshBookings()
    }

    private fun refreshFilters() {
        filterRow.removeAllViews()

        listOf("Upcoming", "Past", "All").forEach { filter ->
            val selected = filter == selectedFilter
            val chip = TextView(this).apply {
                text = filter
                textSize = 13f
                typeface = fontMedium
                gravity = Gravity.CENTER
                setPadding(dp(19), dp(10), dp(19), dp(10))
                setTextColor(if (selected) white else darkGreen)
                background = rippled(rounded(if (selected) darkGreen else white, 24))
                elevation = if (selected) dp(2).toFloat() else 0f

                setOnClickListener {
                    selectedFilter = filter
                    refreshFilters()
                    refreshBookings()
                }
            }
            attachPress(chip)

            filterRow.addView(
                chip,
                LinearLayout.LayoutParams(-2, dp(43)).apply {
                    marginEnd = dp(8)
                }
            )
        }
    }

    private fun refreshBookings() {
        listContainer.removeAllViews()

        val now = System.currentTimeMillis()

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

            emptyCard.addView(headline(emptyTitle, 17f, darkGreen).apply {
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
            animateIn(card, index * 45L)
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

        topRow.addView(
            headline(booking.dogName, 21f, darkGreen),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val isPast = booking.dateTimeMillis < System.currentTimeMillis()
        val badge = TextView(this).apply {
            text = if (isPast) "PAST SESSION" else "UPCOMING"
            textSize = 9f
            typeface = fontMedium
            letterSpacing = 0.08f
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
            typeface = fontMedium
            setTextColor(green)
        }
        card.addView(typePill)
        card.addView(space(12))

        val divider = View(this).apply {
            setBackgroundColor(cream)
        }
        card.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
        card.addView(space(11))

        card.addView(caps("CLIENT", 10f, mutedText))
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
            typeface = fontMedium
            setTextColor(Color.rgb(145, 68, 61))
            background = rippled(rounded(pink, 13))
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
        attachPress(deleteButton)

        actions.addView(deleteButton, LinearLayout.LayoutParams(-2, dp(43)))
        card.addView(actions)

        return card
    }

    // ---------------------------------------------------------------- Reminders

    private fun buildRemindersScreen() {
        val (_, root) = screenScroll()
        addBrandHeader(root)
        root.addView(caps("REMINDER CENTRE", 12f, green))
        root.addView(space(4))
        root.addView(headline("Never miss a shoot.", 29f, darkText))
        root.addView(space(7))
        root.addView(text("Test notifications and review reminders saved with each booking.", 14f, mutedText))
        root.addView(space(22))

        val upcoming = bookings.filter { it.dateTimeMillis >= System.currentTimeMillis() }
            .sortedBy { it.dateTimeMillis }
        val reminderTotal = upcoming.sumOf { it.reminderOffsetsMinutes.size }

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(white, 20)
            elevation = dp(2).toFloat()
        }
        summary.addView(caps("UPCOMING SCHEDULE", 11f, green))
        summary.addView(space(8))
        summary.addView(headline("$reminderTotal reminders", 26f, darkGreen))
        summary.addView(space(4))
        summary.addView(text("Across ${upcoming.size} upcoming session(s)", 13f, mutedText))
        root.addView(summary)
        animateIn(summary)
        root.addView(space(18))

        root.addView(sectionCard(
            "Send a test notification",
            "Check that Fur & Focus notifications can appear on your phone."
        ) {
            sendPlannerNotification("Fur & Focus test", "Your notifications are working.")
        })
        root.addView(space(10))
        root.addView(sectionCard(
            "Test a scheduled reminder · 1 min",
            "Schedule a test reminder for approximately one minute from now."
        ) {
            requestNotificationPermissionIfNeeded()
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Allow notifications first, then try again.", Toast.LENGTH_LONG).show()
            } else {
                ReminderScheduler.scheduleTest(this)
                Toast.makeText(this, "Test scheduled. Wait about a minute.", Toast.LENGTH_LONG).show()
            }
        })
        root.addView(space(10))
        root.addView(sectionCard(
            "Notify me of upcoming bookings",
            "Send a notification listing your future photography sessions."
        ) { sendUpcomingBookingsNotification() })
        root.addView(space(24))

        root.addView(caps("REMINDERS BY SESSION", 12f, green))
        root.addView(space(10))
        if (upcoming.isEmpty()) {
            root.addView(text("No upcoming sessions yet. Add a booking to set up reminders.", 14f, mutedText))
        } else {
            upcoming.forEach { booking ->
                root.addView(sectionCard(
                    "${booking.dogName} · ${dateLabel(booking.dateTimeMillis)}",
                    "${booking.clientName} — ${formatReminderOffsets(booking.reminderOffsetsMinutes)}"
                ) { showTab("Bookings") })
                root.addView(space(9))
            }
        }
    }

    // ---------------------------------------------------------------- Calendar

    private fun buildCalendarScreen() {
        val (_, root) = screenScroll()
        addBrandHeader(root)
        root.addView(caps("SESSION CALENDAR", 12f, green))
        root.addView(space(4))
        root.addView(headline("Plan your month.", 29f, darkText))
        root.addView(space(7))
        root.addView(text("Booked dates are marked. Tap a date to see sessions or create a booking.", 14f, mutedText))
        root.addView(space(20))

        val monthCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(16))
            background = rounded(white, 20)
            elevation = dp(2).toFloat()
        }
        root.addView(monthCard)

        // direction: -1 slides the month in from the left, 1 from the right, 0 is no slide.
        fun renderMonth(direction: Int = 0) {
            monthCard.removeAllViews()
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val previous = TextView(this).apply {
                text = "‹"
                textSize = 30f
                gravity = Gravity.CENTER
                setTextColor(darkGreen)
                setOnClickListener { calendarMonth.add(Calendar.MONTH, -1); renderMonth(-1) }
            }
            attachPress(previous)
            header.addView(previous, LinearLayout.LayoutParams(dp(42), dp(44)))
            val monthTitle = headline(
                SimpleDateFormat("MMMM yyyy", Locale.UK).format(calendarMonth.time),
                17f,
                darkGreen
            )
            monthTitle.gravity = Gravity.CENTER
            header.addView(monthTitle, LinearLayout.LayoutParams(0, -2, 1f))
            val next = TextView(this).apply {
                text = "›"
                textSize = 30f
                gravity = Gravity.CENTER
                setTextColor(darkGreen)
                setOnClickListener { calendarMonth.add(Calendar.MONTH, 1); renderMonth(1) }
            }
            attachPress(next)
            header.addView(next, LinearLayout.LayoutParams(dp(42), dp(44)))
            monthCard.addView(header)
            monthCard.addView(space(8))

            val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val weekdayRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            weekdays.forEach { day ->
                val label = caps(day, 10f, mutedText).apply { gravity = Gravity.CENTER }
                weekdayRow.addView(label, LinearLayout.LayoutParams(0, dp(30), 1f))
            }
            monthCard.addView(weekdayRow)

            val first = calendarMonth.clone() as Calendar
            first.set(Calendar.DAY_OF_MONTH, 1)
            val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
            val rows = (offset + daysInMonth + 6) / 7
            val today = Calendar.getInstance()
            for (week in 0 until rows) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                for (column in 0..6) {
                    val dayNumber = week * 7 + column - offset + 1
                    if (dayNumber !in 1..daysInMonth) {
                        row.addView(View(this), LinearLayout.LayoutParams(0, dp(43), 1f))
                    } else {
                        val dayCal = calendarMonth.clone() as Calendar
                        dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                        val hasBookings = bookings.any {
                            val b = Calendar.getInstance().apply { timeInMillis = it.dateTimeMillis }
                            b.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                b.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                                b.get(Calendar.DAY_OF_MONTH) == dayNumber
                        }
                        val isToday = today.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                            today.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) == dayNumber
                        val cell = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER
                            setPadding(0, dp(3), 0, dp(3))
                            background = rippled(
                                rounded(
                                    when { isToday -> darkGreen; hasBookings -> sage; else -> white },
                                    10
                                )
                            )
                            isClickable = true
                            isFocusable = true
                            setOnClickListener { showCalendarDay(dayCal) }
                        }
                        attachPress(cell)
                        cell.addView(text(dayNumber.toString(), 13f,
                            if (isToday) white else darkText, isToday || hasBookings).apply { gravity = Gravity.CENTER })
                        val dot = text(if (hasBookings) "•" else " ", 12f, if (isToday) white else green, true)
                        dot.gravity = Gravity.CENTER
                        cell.addView(dot)
                        row.addView(cell, LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                            setMargins(dp(2), dp(2), dp(2), dp(2))
                        })
                    }
                }
                monthCard.addView(row)
            }

            if (direction != 0 && motionEnabled()) {
                monthCard.translationX = direction * dp(40).toFloat()
                monthCard.alpha = 0.3f
                monthCard.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(340)
                    .setInterpolator(DecelerateInterpolator(1.4f))
                    .start()
            }
        }
        renderMonth()
        root.addView(space(15))
        root.addView(sectionCard("＋ Create a booking", "Add a photography session and choose its date and time.") {
            showAddBookingDialog()
        })
    }

    private fun showCalendarDay(day: Calendar) {
        val start = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val dayBookings = bookings.filter { it.dateTimeMillis >= start.timeInMillis && it.dateTimeMillis < end.timeInMillis }
            .sortedBy { it.dateTimeMillis }
        val message = if (dayBookings.isEmpty()) "No sessions booked for this date yet." else
            dayBookings.joinToString("\n\n") { "${timeLabel(it.dateTimeMillis)} · ${it.dogName}\n${it.clientName}\n${it.sessionType}" }
        AlertDialog.Builder(this)
            .setTitle(SimpleDateFormat("EEEE d MMMM", Locale.UK).format(day.time))
            .setMessage(message)
            .setNegativeButton("Close", null)
            .setPositiveButton("Create booking") { _, _ ->
                val bookingDate = (day.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                showAddBookingDialog(bookingDate)
            }
            .show()
    }

    // ---------------------------------------------------------------- Overview

    private fun buildOverviewScreen() {
        val (_, root) = screenScroll()
        addBrandHeader(root)
        root.addView(caps("YOUR BUSINESS", 12f, green))
        root.addView(space(4))
        root.addView(headline("A little overview.", 29f, darkText))
        root.addView(space(7))
        root.addView(text("A quick snapshot of your photography calendar.", 14f, mutedText))
        root.addView(space(22))

        val now = System.currentTimeMillis()
        val upcoming = bookings.count { it.dateTimeMillis >= now }
        val past = bookings.count { it.dateTimeMillis < now }
        val cal = Calendar.getInstance()
        val thisMonth = bookings.count {
            val date = Calendar.getInstance().apply { timeInMillis = it.dateTimeMillis }
            date.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                date.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        }

        listOf(
            Triple("UPCOMING SESSIONS", upcoming, "Sessions still to come"),
            Triple("THIS MONTH", thisMonth, "Sessions scheduled this month"),
            Triple("PAST SESSIONS", past, "Sessions in your history"),
            Triple("TOTAL BOOKINGS", bookings.size, "All saved sessions")
        ).forEachIndexed { index, (heading, target, subtitle) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(17), dp(18), dp(17))
                background = rounded(white, 18)
                elevation = dp(1).toFloat()
            }
            card.addView(caps(heading, 11f, green))
            card.addView(space(4))
            val number = headline("0", 30f, darkGreen)
            card.addView(number)
            card.addView(space(2))
            card.addView(text(subtitle, 12f, mutedText))
            root.addView(card)
            animateIn(card, index * 70L)
            countUp(number, target, index * 70L + 150L)
            root.addView(space(10))
        }

        root.addView(space(8))
        root.addView(sectionCard("View upcoming bookings", "Go to your booking list and manage sessions.") {
            selectedFilter = "Upcoming"
            showTab("Bookings")
        })
        root.addView(space(10))
        root.addView(sectionCard("Add a photography session", "Create a new booking with custom reminders.") {
            showTab("Bookings")
            screenHost.post { showAddBookingDialog() }
        })
    }

    // ---------------------------------------------------------------- Settings

    private fun buildSettingsScreen() {
        val (_, root) = screenScroll()
        addBrandHeader(root)
        root.addView(caps("APP SETTINGS", 12f, green))
        root.addView(space(4))
        root.addView(headline("Your planner, your way.", 27f, darkText))
        root.addView(space(7))
        root.addView(text("Useful tools and information for your app.", 14f, mutedText))
        root.addView(space(22))

        root.addView(sectionCard(
            "Notification permission",
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) "Notifications are not currently permitted. Tap to request permission."
            else "Notification permission is granted or not required on this Android version."
        ) { requestNotificationPermissionIfNeeded() })
        root.addView(space(10))
        root.addView(sectionCard(
            "Send a test notification",
            "Check whether notifications appear on this device."
        ) { sendPlannerNotification("Fur & Focus test", "Your notifications are working.") })
        root.addView(space(10))
        root.addView(sectionCard(
            "Notify me of upcoming bookings",
            "Send a summary of all upcoming sessions."
        ) { sendUpcomingBookingsNotification() })
        root.addView(space(22))

        val about = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(paleSage, 18)
        }
        about.addView(caps("FUR & FOCUS", 12f, green))
        about.addView(space(5))
        about.addView(headline("Photography Planner", 20f, darkGreen))
        about.addView(space(5))
        about.addView(text("Made to help organise pet photography clients, sessions and reminders.", 13f, mutedText))
        root.addView(about)
    }

    // ---------------------------------------------------------------- Add / edit dialog

    private fun showAddBookingDialog(initialDateTime: Calendar? = null) {
        selectedDateTime = initialDateTime?.clone() as? Calendar ?: Calendar.getInstance()

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
        form.addView(caps("REMINDER SCHEDULE", 11f, green))
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

        attachPress(dateButton)
        attachPress(timeButton)
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
        form.addView(caps(label, 10f, green))
        form.addView(space(5))
        form.addView(field)
        form.addView(space(12))
    }

    private fun pickerButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            typeface = fontMedium
            setTextColor(darkGreen)
            background = rippled(rounded(paleSage, 14))
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
            typeface = fontRegular
            setTextColor(darkText)
            setHintTextColor(mutedText)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(cream, 13)
            minHeight = dp(48)
            backgroundTintList = null
        }
    }

    // ---------------------------------------------------------------- Shared UI pieces

    private fun sectionCard(title: String, description: String, action: () -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
            background = rippled(rounded(white, 17))
            elevation = dp(1).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        attachPress(card)
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        copy.addView(headline(title, 14f, darkText))
        copy.addView(space(4))
        copy.addView(text(description, 12f, mutedText))
        card.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(text("›", 24f, green, true))
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

    // ---------------------------------------------------------------- Motion

    /** False when the user has turned off animations in system settings. */
    private fun motionEnabled(): Boolean = try {
        Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    } catch (_: Exception) {
        true
    }

    private fun animateIn(view: View, delay: Long = 0L) {
        if (!motionEnabled()) {
            view.alpha = 1f
            view.translationY = 0f
            return
        }
        view.alpha = 0f
        view.translationY = dp(14).toFloat()
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(420)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    /** Gentle scale-down while pressed, with a soft spring back on release. */
    private fun attachPress(view: View) {
        view.setOnTouchListener { v, event ->
            if (motionEnabled()) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> v.animate()
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .setDuration(110)
                        .setInterpolator(DecelerateInterpolator())
                        .start()

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(260)
                        .setInterpolator(OvershootInterpolator(1.6f))
                        .start()
                }
            }
            false // let the normal click listener still run
        }
    }

    private fun popIn(view: View) {
        if (!motionEnabled()) return
        view.scaleX = 0.6f
        view.scaleY = 0.6f
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(480)
            .setInterpolator(OvershootInterpolator(2.2f))
            .start()
    }

    private fun countUp(view: TextView, target: Int, delay: Long) {
        if (!motionEnabled() || target == 0) {
            view.text = target.toString()
            return
        }
        ValueAnimator.ofInt(0, target).apply {
            duration = 900L
            startDelay = delay
            interpolator = DecelerateInterpolator()
            addUpdateListener { view.text = (it.animatedValue as Int).toString() }
            start()
        }
    }

    // ---------------------------------------------------------------- Helpers

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
            typeface = if (bold) fontMedium else fontRegular
            includeFontPadding = false
        }
    }

    /** Large, medium-weight heading with slightly tightened spacing. */
    private fun headline(value: String, size: Float, colour: Int): TextView {
        return text(value, size, colour, bold = true).apply {
            letterSpacing = -0.01f
        }
    }

    /** Small uppercase label with open letter-spacing. */
    private fun caps(value: String, size: Float, colour: Int): TextView {
        return text(value, size, colour, bold = true).apply {
            letterSpacing = 0.12f
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

    private fun oval(colour: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colour)
        }
    }

    /** Wraps a background so it shows a touch ripple. */
    private fun rippled(content: GradientDrawable): RippleDrawable {
        return RippleDrawable(
            ColorStateList.valueOf(rippleColour),
            content,
            null
        )
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
        private val orange = Color.rgb(225, 155, 94)

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
            val topPath = Path().apply {
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
