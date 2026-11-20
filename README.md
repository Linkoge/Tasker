# Tasker

A basic offline Android task app (Kotlin, Jetpack Compose, Room). It needs no account and no network.

## Features

- **Lists as tabs.** Each list appears as a tab along the top. The app creates Daily, Weekly and Monthly on first run.
  Open **⋮ → Manage lists** to create, rename, delete and reorder lists with the up/down buttons. The saved order
  sets the tab order. Deleting a list also deletes its tasks, and you can't delete the last list.
- **Tasks.** Each task has a title and optional notes. Tap **+** to add a task. Tap a task to edit it, delete it, or
  move it to another list with the **List** picker. Tick the checkbox to complete a task.
- **Priority 1–10** (10 is the most urgent). Within a list, open tasks are sorted from highest to lowest priority,
  with older tasks first when priorities are equal. On the right of each row are the priority number and a bar.
  The bar gets taller and its color goes from faint amber to bright red as priority rises.
- **Completed** tasks move to a section at the bottom that you can collapse. Untick a task to reopen it.
- All data is stored on the device in a Room (SQLite) database.

## Install

Copy `Tasker.apk` from the repository root to the phone and open it. You may need to allow installs from unknown
sources. You can also install over USB:

```sh
adb install -r Tasker.apk
```

`Tasker.apk` is a debug build signed with the standard Android debug key.

## Build

Requirements: JDK 17 and an Android SDK with platform 36 installed (set `ANDROID_HOME` or `sdk.dir` in
`local.properties`).

```sh
./gradlew :app:testDebugUnitTest      # JVM unit tests (sorting, priority bounds, list reorder, task move)
./gradlew :app:assembleDebug          # builds app/build/outputs/apk/debug/app-debug.apk
cp app/build/outputs/apk/debug/app-debug.apk Tasker.apk
```

Project smoke checks (structure, committed APK, unit tests): `python -m pytest -q -m smoke`.

## Layout

- `app/src/main/java/dev/tasker/domain/` contains the pure logic (`TaskLogic.kt`), the repository and the storage interface
- `app/src/main/java/dev/tasker/data/Database.kt` contains the Room entities, the DAO and the database
- `app/src/main/java/dev/tasker/ui/` contains the Compose screens (home with tabs, task dialog, manage lists)
- `app/src/test/` contains the unit tests
