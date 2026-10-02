# TaskTracker (Android) — Room + Firebase backup

Native port of `Task_Tracker.html`: same task model (priority, categories, daily / weekly /
monthly / custom recurrence, per-day completions), with Room for offline storage and
Firestore for cloud backup.

## Why data survives uninstall/reinstall
Uninstalling deletes the Room database. The data survives because it lives in Firestore under
`users/{uid}/...`. After reinstalling, the user signs in with the SAME account -> same uid ->
`TaskRepository.sync()` pulls everything back into Room.
Anonymous auth would NOT work: a fresh install gets a new anonymous uid.

## Architecture
UI (Compose) -> TaskViewModel -> TaskRepository -> Room (source of truth)
                                     `-> FirestoreDataSource (push dirty rows / pull newer rows)
SyncWorker (WorkManager) retries a failed sync when the network returns, plus a 6-hourly sync.
The app syncs automatically (every 30 s while open, every 10 s retry after a failure), after sign-in, and via Profile -> "Sync now".
Settings (profile name, contact, theme) sync too via users/{uid}/settings/app. Pull is incremental (only changed docs).
Sync errors are shown on the Profile tab (never swallowed silently).
Conflict rule: last write wins on `updatedAt`. Deletes are tombstones (`deleted = true`).

## Setup
1. Firebase console -> create project -> add Android app `com.example.tasktracker`
2. Put the downloaded `google-services.json` in `app/`
3. Enable Authentication -> Email/Password, and create a Firestore database
4. Publish `firestore.rules`
5. Open in Android Studio, sync Gradle, run

## Layout (app/src/main/java/com/example/tasktracker/)
- TaskTrackerApp.kt        DI container + schedules periodic sync
- data/local/              Room entities, DAOs, converters, database
- data/remote/             Firestore data source + map <-> entity mappers
- data/TaskRepository.kt   offline-first logic, push/pull merge
- domain/Recurrence.kt     Kotlin port of occursOn() from the HTML tracker
- sync/SyncWorker.kt       WorkManager sync
- auth/AuthManager.kt      Firebase email/password auth
- ui/                      MainActivity, ViewModel, login + today screens

## Next steps
Calendar/progress screens, edit sheet with recurrence options, reminders (AlarmManager),
Google Sign-In (Credential Manager), and a one-time importer for the web tracker's JSON
(`taskTracker.v2` localStorage key).
