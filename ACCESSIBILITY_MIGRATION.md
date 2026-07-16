# Accessibility → UsageStats migration assessment

## The risk
Locky detects the foreground app with an `AccessibilityService`
(`AppLockAccessibilityService.kt`, `TYPE_WINDOW_STATE_CHANGED`). Google Play's
**Accessibility API policy** restricts this API to apps that genuinely help users with
disabilities. App-lockers are a well-known rejection/removal category, *even when* (as
here) `canRetrieveWindowContent="false"` and no screen content is read. This is the #1
reason Locky may be rejected on Play.

## The Play-compliant alternative
Detect the foreground app with **`UsageStatsManager`** + the `PACKAGE_USAGE_STATS`
special permission (user grants it in Settings → Usage access), keeping the existing
`SYSTEM_ALERT_WINDOW` overlay for the lock UI.

### What changes
| Piece | Today (accessibility) | After (usage-stats) |
|---|---|---|
| Foreground detection | push events, instant | **poll** `queryEvents()` on a loop from a foreground service |
| Service | `AccessibilityService` | `Service` (foreground, `dataSync`/`specialUse`) + `PACKAGE_USAGE_STATS` |
| Permission | Accessibility toggle | Usage-access toggle |
| Manifest | accessibility `<service>` + config xml | foreground-service perms + usage-stats perm |

### The real tradeoff — latency / "flash"
Accessibility gives a **push** the instant the window changes, which is why Locky can
throw up the opaque bridge overlay with *no flash* of the locked app. UsageStats is
**poll-only**: you loop `queryEvents()` on a foreground service, typically every
~100–300 ms. Between the app coming forward and the next poll, the locked app is briefly
visible → a flash. Mitigations (none as clean as today):
- Tight poll interval (higher battery cost).
- Keep the overlay up over *any* newly-foregrounded unknown app, hide it fast if not
  locked (aggressive, can flicker on normal use).
There is no way to fully match the zero-flash behaviour without the push signal.

## Recommendation
Pick based on distribution goal:

1. **Play Store is the goal** → migrate to UsageStats (accept the small flash), OR keep
   accessibility and fight the declaration form (may still be rejected). UsageStats is
   the safer bet for approval.
2. **GitHub Release / F-Droid / sideload is fine** → **keep accessibility as-is.** It's
   the better UX (zero flash) and the policy only binds Play distribution. You already
   ship this way via the GitHub Release.

## Migration plan (if you choose UsageStats)
1. Manifest: drop the accessibility `<service>` + `@xml/accessibility_service_config`;
   add `<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
   tools:ignore="ProtectedPermissions"/>` and a foreground-service type
   (`FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` on API 34+).
2. Replace `AppLockAccessibilityService` with a foreground `Service` that:
   - starts a notification (required for FGS),
   - loops `usageStatsManager.queryEvents(now-2s, now)` reading
     `MOVE_TO_FOREGROUND` events on a short handler/coroutine tick,
   - reuses the existing `showOverlay()` / `launchGate()` / relock logic verbatim.
3. Onboarding: swap the "Open accessibility" step for "Open usage access"
   (`Settings.ACTION_USAGE_ACCESS_SETTINGS`) + check via `AppOpsManager`.
4. Keep `LockScreenActivity`, `BiometricHelper`, `LockRepository`, `UnlockState`,
   overlay layout — all unchanged.

Estimated scope: ~1 new service file + manifest + onboarding tweak. Auth, storage, and
lock UI are untouched. This is a real change with a UX cost (the flash) — decide the
distribution goal first, then say the word and I'll implement it.
