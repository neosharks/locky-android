# Locky 🔒

A tiny, native Android app locker with a bit of personality. Lock any app behind your
**fingerprint** — only fingerprint, no PIN/pattern fallback. Built with plain Kotlin +
Android Views + Material 3 — no heavy frameworks, small APK.

## What it does

- **Entry gate** — opening Locky itself requires your fingerprint.
- **Locked apps list** — the home screen shows every app you've locked. Tap **Unlock**
  (or long-press a row) to remove one — after confirming your fingerprint.
- **Add apps (＋)** — the + button opens a list of all installed apps. Tap or long-press
  an app to lock / unlock it; each action asks for your fingerprint first.
- **Enforcement** — when you open a locked app, a full-screen gate appears and asks for
  your fingerprint. Cancel → you're sent to the home screen, never into the locked app.
- **Re-locks on background** — the moment a locked app leaves the foreground (Home,
  switching apps, recents / quick settings, or screen off) it re-locks, so reopening
  always asks for the fingerprint again.
- **Home-screen shortcuts** — long-press Locky's own icon for **Lock an app** /
  **Unlock an app** quick actions (they open the picker after the fingerprint gate).

## How it works (native systems only)

| Concern | Android system used |
|---|---|
| Auth (fingerprint only) | `androidx.biometric.BiometricPrompt` with `BIOMETRIC_STRONG` |
| Detecting which app is open | `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`) |
| No flash before the lock | opaque `TYPE_APPLICATION_OVERLAY` bridge window added the instant a locked app is detected |
| Hosting the fingerprint prompt over an app | a transparent-then-opaque top `Activity` (BiometricPrompt requires a top activity) |
| Listing / naming / icons of apps | `PackageManager` |
| Storing which apps are locked | `SharedPreferences` |

> **Android limitation (by design):** a third-party app **cannot** add a "Lock / Unlock"
> item to *another* app's launcher long-press menu (e.g. WhatsApp's icon) — the launcher
> owns that menu, there is no public API. The closest possible thing, which Locky does,
> is App Shortcuts on **its own** icon. Locking/unlocking is otherwise done *inside*
> Locky (tap or long-press an app in the picker), like every app locker (Norton, etc.).

## Two permissions are required

On first launch Locky will prompt for:
1. **Display over other apps** — to draw the unlock gate on top of a locked app.
2. **Accessibility** — to detect when a locked app comes to the foreground.
   (Locky sets `canRetrieveWindowContent="false"` — it never reads screen content.)

## Build & run

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in **Android Studio** and press Run.

- Package `com.neosharks.locky`, Kotlin, Gradle 8.7, AGP 8.6.1
- `minSdk 30` (Android 11), `targetSdk 35` (Android 15)
- Dependencies: core-ktx, appcompat, material, biometric, recyclerview

## Project layout

```
app/src/main/java/com/neosharks/locky/
  MainActivity.kt                 – entry gate + locked-apps list + permission onboarding
  AppPickerActivity.kt            – all-apps list, tap/long-press to lock/unlock
  LockScreenActivity.kt           – opaque gate that hosts the fingerprint prompt
  AppLockAccessibilityService.kt  – detects foreground app, bridge overlay, relocks
  BiometricHelper.kt              – wraps BiometricPrompt (fingerprint only)
  LockRepository.kt               – SharedPreferences store of locked packages
  UnlockState.kt                  – in-memory "unlocked this session" set
  AppsProvider.kt                 – PackageManager queries
  AllAppsAdapter.kt / LockedAppsAdapter.kt – RecyclerView adapters
```
