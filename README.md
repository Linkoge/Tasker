# Tasker

A basic, offline-only native Android task app (Kotlin, Jetpack Compose, Room). No accounts, no network, dark theme by default.
Functionally inspired by simple list-based task apps; it uses its own name and icon.

## Features

- **Categories as top tabs** with open-task counts. Defaults: Daily, Weekly, Monthly. Unicode names (e.g. Georgian) work.
- **Create / rename / delete categories** from the overflow menu → *Manage categories*. Deleting a category deletes its tasks (confirmation shown); the last remaining category cannot be deleted.
- **Reorder categories** in *Manage categories* with up/down buttons; tab order follows it and is persisted (`position` column).
- **Tasks**: title, optional notes, priority, category. Add, edit, complete/uncomplete (circle checkbox), delete (trash icon in the editor).
- **Move a task** to another category with the *Category* dropdown in the task editor.
- **Priority 1–10** (10 = most urgent), set on create and edit. Open tasks are sorted by priority descending, ties by creation time (oldest first). Each row shows on the right the number plus a coloured vertical bar, going from calm teal (1) through yellow/orange to red (10).
- **Completed (N)** collapsible section per category, most recently completed first.

## Project layout

- `app/src/main/java/dev/tasker/domain/` – pure Kotlin rules (`TaskLogic.kt`: sorting, counts, reorder, move, priority colour) and `TaskRepository` over a `TaskStore` interface.
- `app/src/main/java/dev/tasker/data/` – Room entities/DAO/database and the `RoomTaskStore` adapter. Schema export in `app/schemas/`.
- `app/src/main/java/dev/tasker/ui/` – Compose screens (home, task editor, manage categories) and `MainViewModel`.
- `app/src/test/` – JVM unit tests (sorting, reorder persistence, moving tasks) using an in-memory `TaskStore`.
- `tests/test_smoke.py` – pytest smoke suite (`python -m pytest -q -m smoke`): structure, APK presence/signature, and runs the JVM unit tests.

## Build

Requirements: JDK 17 and the Android SDK (platform 36, build-tools). Point Gradle at the SDK with `ANDROID_HOME` (or a git-ignored `local.properties` containing `sdk.dir=/path/to/Sdk`).

```bash
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk (debug-signed, installable)
cp app/build/outputs/apk/debug/app-debug.apk Tasker.apk
./gradlew testDebugUnitTest             # JVM unit tests
python -m pytest -q -m smoke            # smoke suite
```

The prebuilt, debug-signed `Tasker.apk` is committed at the repository root. Install with `adb install -r Tasker.apk` or copy it to the phone (allow installs from unknown sources). Debug signing means updates install over it only when built with the same debug keystore.
