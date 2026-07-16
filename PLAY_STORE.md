# Publishing Locky to Google Play

This repo is wired for **Gradle Play Publisher (GPP)**. Once you do the one-time
manual setup below, releases push from the command line.

---

## 0. Artifact
Play requires a signed **AAB** (not APK):

```bash
./gradlew bundleRelease
# -> app/build/outputs/bundle/release/app-release.aab   (signed with keystore/locky-release.jks)
```

The same signing key that produced the GitHub release signs this. **Guard the keystore + password** — losing them means you can never update this app.

> Tip: enrol in **Play App Signing** (default for new apps). You upload with *your*
> key; Google re-signs for distribution. If you ever lose your upload key, Google
> can reset it — a safety net you do NOT get for the app signing key itself.

---

## 1. Store listing — already drafted
GPP reads these from `app/src/main/play/` (edit freely):

| File | Content | Play limit |
|---|---|---|
| `listings/en-US/title.txt` | `Locky: Fingerprint App Lock` | 30 chars |
| `listings/en-US/short-description.txt` | one-liner | 80 chars |
| `listings/en-US/full-description.txt` | full copy | 4000 chars |
| `release-notes/en-US/default.txt` | v1.0 notes | 500 chars |
| `contact-email.txt` | support email | — |

### Graphics you must still create and drop in (GPP will upload them):
- `listings/en-US/graphics/icon/*.png` — **512×512** app icon (32-bit PNG).
- `listings/en-US/graphics/feature-graphic/*.png` — **1024×500** feature graphic.
- `listings/en-US/graphics/phone-screenshots/*.png` — **at least 2** (up to 8), 16:9 or 9:16, 320–3840 px per side. Grab from a device/emulator running the release build.

---

## 2. Console declarations you must fill by hand (policy/legal — I can't answer for you)
In Play Console → your app → these are interactive forms:
- **App category:** Tools.  **Tags:** utility / privacy.
- **Content rating** questionnaire → likely *Everyone* (no violence/ads/UGC).
- **Target audience:** 18+ or 13+ (not a kids app).
- **Ads:** contains ads → **No**.
- **Data safety:** no data collected, no data shared, processed on-device. (Locky stores locked-app list only in local `SharedPreferences`.)
- **Privacy policy URL:** **REQUIRED** even if you collect nothing. Host a short page (a GitHub Pages / gist link works) and paste the URL.

### ⚠️ Accessibility permission — expect scrutiny
Locky uses `AccessibilityService` to detect the foreground app. Google restricts this
API to genuine accessibility use. On submission you'll hit the **Permissions Declaration
Form**. Be ready to:
- Explain the core function needs it (app-locking / foreground detection) and that
  `canRetrieveWindowContent="false"` — Locky never reads screen content.
- Accept that reviewers may still reject it. Fallback options: migrate to
  `UsageStatsManager` + overlay (see `ACCESSIBILITY_MIGRATION.md`), or distribute the
  APK/AAB via the GitHub Release / F-Droid instead.

---

## 3. One-time setup for API publishing (you do this — needs your Google account)
1. **Create the app** in Play Console (name, default language, app/game, free/paid). The
   very first AAB usually must be uploaded **manually** in the Console (Internal testing).
2. **Service account** (Google Cloud):
   - Play Console → *Setup → API access* → link/create a Google Cloud project.
   - Create a **service account**, grant it access, download the **JSON key**.
   - Back in Play Console → *Users & permissions* → invite that service-account email →
     grant *Admin (all)* or at least *Release* + *Edit store listing* on this app.
3. Drop the JSON key here:
   ```
   /play-service-account.json      (repo root — already gitignored)
   ```
   GPP auto-activates when this file exists (see `app/build.gradle.kts`).

---

## 4. Publish from the CLI (after step 3)
```bash
# Upload AAB + listing + release notes to the INTERNAL testing track (default):
./gradlew publishReleaseBundle

# Just the store listing / graphics, no new build:
./gradlew publishReleaseListing

# Promote what's on internal -> production when ready:
./gradlew promoteReleaseArtifact --from-track internal --promote-track production
```
Track/format defaults live in the `play { }` block in `app/build.gradle.kts`
(`track = "internal"`, `defaultToAppBundles = true`). Change `track` to `production`
to push straight to prod once the app has passed initial review.

---

## Quick checklist
- [ ] Play Console account ($25, verified)
- [ ] App created in Console; first AAB uploaded manually to Internal testing
- [ ] Service account JSON at `/play-service-account.json`
- [ ] Service account invited + permissioned on the app
- [ ] Icon 512², feature graphic 1024×500, ≥2 screenshots added under `play/.../graphics/`
- [ ] Privacy policy URL live
- [ ] Data safety + content rating + ads + target-audience forms completed
- [ ] Accessibility permission declaration submitted
- [ ] `./gradlew publishReleaseBundle`
