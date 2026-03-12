# 💪 GymTracker — Android App

A clean, single-screen Android app for tracking your gym membership, personal training sessions, and workout activity.

---

## Features

- **Membership Tracking** — Set your payment date, auto-calculates 30-day expiry with a color-coded progress bar (green → yellow → red)
- **Personal Training Counter** — Track how many PT sessions you purchased vs. used vs. remaining
- **Daily Check-In** — Log today as a regular or personal training session, with undo/change support
- **Activity Heatmap** — GitHub-style 16-week grid with two colors: sky blue for regular training, periwinkle for personal training
- **Streak Counter** — Tracks consecutive training days
- **Dark theme** with cool minimal color scheme and Material You (dynamic color) support on Android 12+

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| State | ViewModel + StateFlow |
| Local DB | Room (training sessions) |
| Preferences | DataStore (membership date, PT count) |
| Language | Kotlin |

---

## Project Structure

```
app/src/main/java/com/gymtracker/
├── MainActivity.kt
├── data/
│   ├── Database.kt          # Room entity, DAO, database
│   ├── GymPreferences.kt    # DataStore for membership & PT prefs
│   └── GymRepository.kt     # Single source of truth
├── viewmodel/
│   └── GymViewModel.kt      # UI state + business logic
└── ui/
    ├── Theme.kt              # Dark theme, brand colors
    └── DashboardScreen.kt    # Entire single-screen UI
```

---

## Setup Instructions

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 11** or newer
- Android SDK with **API 26+**

### Steps

1. **Open in Android Studio**
   - File → Open → select the `GymTracker` folder

2. **Sync Gradle**
   - Android Studio will prompt to sync — click "Sync Now"
   - Wait for all dependencies to download (~2 min first time)

3. **Add launcher icons** (required to build)
   - Right-click `app/src/main/res` → New → Image Asset
   - Choose any icon or use the default Android icon
   - This creates the required `mipmap` folders

4. **Run the app**
   - Connect an Android device (API 26+) or start an emulator
   - Press the green ▶ Run button

### Minimum Requirements
- Android 8.0 (API 26) or higher
- ~10 MB storage

---

## How to Use

### Set Membership Date
1. Tap **"Set Payment Date"** in the Membership card
2. Pick the date you paid in the calendar
3. App calculates +30 days expiry automatically

### Add Personal Training Sessions
1. Tap **"Add Sessions"** in the Personal Trainings card
2. Enter how many sessions you purchased
3. Counter updates automatically as you log PT sessions

### Check In Today
- Tap **"Regular"** for a regular gym session
- Tap **"Personal"** for a personal training session
- Already checked in? Use **"Change to PT/Regular"** or **"Undo Check-in"**

### Activity Heatmap
- Sky blue cell = regular training day
- Periwinkle cell = personal training day
- Dark cell = no training
- Scroll horizontally to see older weeks

---

## Customization

To change the membership duration from 30 days, edit `GymViewModel.kt`:
```kotlin
val expiry = startDate.plusDays(30)  // ← change this number
```

To adjust heatmap weeks shown, edit `DashboardScreen.kt`:
```kotlin
val weeksToShow = 16  // ← change to show more/fewer weeks
```
