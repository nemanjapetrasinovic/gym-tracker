# GymTracker

A single-screen Android app for tracking gym membership, personal training sessions, and workout activity.

## Features

- **Membership Tracking** — Set your payment date, auto-calculates monthly expiry with a color-coded progress bar
- **Personal Training Counter** — Track how many PT sessions you purchased vs. used vs. remaining
- **Daily Check-In** — Log any day as a regular or personal training session, with undo/change support
- **Activity Heatmap** — GitHub-style 16-week grid showing regular and personal training days
- **Calendar View** — Monthly calendar with session indicators and inline editing
- **Backup & Restore** — Export/import your data as CSV
- **Dark theme** with Material You (dynamic color) support on Android 12+

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| State | ViewModel + StateFlow |
| Local DB | Room |
| Preferences | DataStore |
| Language | Kotlin |

## Setup

1. Open in Android Studio (Hedgehog or newer)
2. Sync Gradle and wait for dependencies
3. Run on a device or emulator (API 26+)

## How to Use

- **Membership** — Tap "Set Payment Date", pick when you paid. Expiry is calculated as the same date next month.
- **Personal Training** — Tap "Add PT" to record purchased sessions. They decrement as you log PT check-ins.
- **Check-In** — Tap "Regular" or "Personal" to log a session. Use the date selector to log past days.
- **Heatmap** — Blue cells = regular, purple cells = personal training, dark cells = rest day.
