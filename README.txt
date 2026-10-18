FUR & FOCUS PHOTOGRAPHY PLANNER — BUILD PACKAGE
Version 1.0

This is a native Android Kotlin project, not an HTML wrapper.

Features in this first build:
- Create bookings with client name, dog's name, date/time, location and notes.
- Stores bookings locally on the phone.
- Shows upcoming and past bookings with a days-to-go countdown.
- Schedules local Android notifications 24 hours and 2 hours before each shoot.
- Restores eligible reminders after device restart or time/time-zone changes.
- Requests Android 13+ notification permission.

Important limitations:
- Alarms use Android's inexact setAndAllowWhileIdle API to avoid requiring special exact-alarm access. Android may delay notifications due to Doze, battery optimisation, manufacturer settings, or permission state.
- This MVP has no booking edit feature, calendar month grid, custom reminder editor, export/backup, or payment tracking yet.
- Test with a booking a few minutes in the future before relying on this for real client appointments.
- On first launch, allow notifications. On Samsung, if reminders are delayed, check Settings > Apps > Fur & Focus Planner > Notifications and Settings > Battery > Background usage limits / battery restrictions.
- Android 15 devices may apply battery restrictions. This project does not guarantee exact-to-the-minute delivery.

Build:
Upload the entire ZIP to a cloud Android Gradle builder that accepts standard Android Studio/Gradle projects.
Expected application ID: com.furandfocus.planner
Minimum Android version: Android 8.0 (API 26)
Target SDK: 35
