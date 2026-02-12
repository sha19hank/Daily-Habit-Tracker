![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin)
![Jetpack%20Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-03DAC6?logo=android)
![API%2034](https://img.shields.io/badge/API-34-brightgreen?logo=android)
![License](https://img.shields.io/badge/License-MIT-yellow)

# Daily Habit Tracker

Offline-first Android habit tracker built with Kotlin and Jetpack Compose. Tracks habits, streaks, stats, reminders, and a focused daily view. No login or cloud dependency; all data stays on device.

## Screenshots
![Home](docs/screenshots/home.png)
![Calendar](docs/screenshots/calendar.png)
![Stats](docs/screenshots/stats.png)

## Features
- Daily habit list with streak tracking
- Calendar and stats views
- Reminders and scheduling
- Focus mode and pause support
- Offline-first local storage

## Architecture
- MVVM with repository layer
- Room for persistence
- DataStore for settings
- WorkManager for reminders

## Tech Stack
- Kotlin
- Jetpack Compose (Material 3)
- Room
- DataStore
- WorkManager

## How to Build
1. Open the project in Android Studio Hedgehog.
2. Let Gradle sync.
3. Run the `app` configuration.

## Requirements
- Android Studio Hedgehog
- Android API 34

## License
MIT License. See [LICENSE](LICENSE).

## Future Improvements
- App widgets
- Cloud backup (optional)
- More analytics views
