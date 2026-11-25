# QuestList — Pop-up Reminder To-Do App (Android)

A mobile to-do app where every task is a **"Daily Quest"**. You add tasks with a time window and a repeat interval. During that window the app throws a **pop-up over whatever you're doing** (even other apps) until you tick the task. Ticking it marks the quest complete and the pop-ups stop.

Visual inspiration: the glowing blue "system window" from Solo Leveling:
`[Daily Quest: Strength Training has arrived.]` → GOAL list with checkboxes → WARNING line.

> **Platform decision:** Android only. iOS does not allow apps to draw pop-ups over other apps, so this core feature is impossible there. Do not try to build for iOS.

---

## 1. Tech Stack (use exactly this)

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3, custom dark/neon theme) |
| Local DB | Room (SQLite) |
| Scheduling | AlarmManager (`setExactAndAllowWhileIdle`) |
| Pop-up over other apps | `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`) hosting a `ComposeView`, run from a Foreground Service |
| Background work | Foreground Service + BroadcastReceivers |
| DI | Manual DI |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 37 |
| Build | Gradle (Kotlin DSL) |

No backend, no login, no internet. Everything is stored on the device.

---

## 2. Core Features

### 2.1 Task management
- Add, edit, delete tasks.
- Each task has:
  - **Title** (e.g. "Push-ups")
  - **Optional goal text** (e.g. "100 reps")
  - **Start time** and **End time** (the active window, e.g. 06:00 → 08:00)
  - **Repeat interval** in minutes (e.g. every 10 min) — how often the pop-up re-appears during the window
  - **Repeat days** (daily, or pick days of week)
  - **Enabled** toggle
- Home screen shows today's quests as a checklist with status: Pending / Active / Completed / Failed.

### 2.2 Pop-up reminders (the main feature)
- At the task's start time, show an overlay pop-up on top of any app/home screen.
- If the task is not completed, **re-show the pop-up every N minutes** until the end time.
- Pop-up contents:
  - Header: `[Daily Quest: <Task title> has arrived.]`
  - `GOAL` underlined heading
  - Task row with a **checkbox**
  - Warning text at the bottom, e.g. `WARNING: Failure to complete the daily quest will result in a penalty.` (editable text in settings)
  - Buttons: **Snooze 5 min** and **Dismiss** (dismiss only hides until next interval)
- **Tapping the checkbox** → task marked complete instantly, pop-up closes with a short fade/glow animation, all remaining alarms for that task today are cancelled.
- Pop-up must work when screen is on and the user is inside another app.
- When the screen is off or locked, fall back to a high-priority full-screen-intent notification with a "Mark done" action button.

---

## 3. License
MIT License.
