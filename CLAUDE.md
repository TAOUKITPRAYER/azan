# CLAUDE.md (root)

This file orients Claude Code across the whole repository. It is a map, not
the detail — follow the pointers below for the rules that actually govern
each part.

## Repository layout

```
app/                    Main Android app "Tawkit" (net.tawkit.mobile)
  src/main/assets/      WebView content — see app/src/main/assets/CLAUDE.md
                         for the Golden Rule (core files vs spec/ files) and
                         everything about the JS/CSS layer. READ THAT FILE
                         before touching anything under assets/.
  src/main/java/net/tawkit/mobile/   Native Kotlin layer (see below)

hijri-admin/            Separate, standalone Android app (net.tawkit.hijriadmin).
                         Personal tool, installed independently on the
                         developer's own phone, to maintain the Supabase
                         hijri_month_starts table (official per-country Hijri
                         month-start dates). Not part of Tawkit's runtime —
                         a different app entirely, own package/APK.

release/                Build & signing scripts (PowerShell), both apps share
                         the same keystore (tawkit.jks, root — gitignored,
                         never commit it):
  instapk.ps1            Tawkit: apk | install | fullinstall | all |
                          setversion <v> | getversion | help
  instapk_hijriadmin.ps1  hijri-admin: apk | install | help

Supabase backend         Project "tawkit.net" (id tjmjmlzwzebocfdmifrg,
                          eu-west-3). Credentials (anon/publishable key + URL)
                          are intentionally hardcoded/duplicated in custom.js
                          and hijri-admin — personal-scale trust model, same
                          pattern used throughout, not hardened multi-tenant
                          auth. Tables:
    mosques                 Per-mosque remote config (azan offsets, iqama
                             delays, jumua/eid times...). Public read + anon
                             insert/update.
    mosque_notifications     Push notification log.
    debug_reports             In-app debug console reports (insert-only from
                             client), sent via a button in the debug console.
    hijri_month_starts        Official per-country Hijri month-start dates
    hijri_month_starts_latest (view: latest row per country). Consulted by
                             custom.js's Hijri sync feature as the top-priority
                             source, ahead of the salahhour.com API and the
                             internal tabular calculation (fallback chain:
                             official Supabase row valid for today → salahhour
                             API → internal STD calc — never treated as an
                             error, just a normal "not yet announced" state).
```

## Native Android layer (`app/src/main/java/net/tawkit/mobile/`)

The app is a `android.webkit.WebView` host (not GeckoView, despite older
notes in `assets/CLAUDE.md`'s "Android / GeckoView" section — that section
predates the current WebView-based implementation and may be stale).
`MainActivity` hosts the WebView and bridges to JS via `MobileJsBridge`
(`window.AndroidMobile` in JS).

Background prayer-time features do **not** rely on the WebView running in
the background — `MainActivity.onPause()` explicitly calls
`webView.onPause()`/`pauseTimers()`, so JS execution genuinely stops when
backgrounded. Everything that must fire while the app is closed/backgrounded
uses native `AlarmManager` instead:

- `PrayerAlarmReceiver` — fires at azan time (and N minutes before, if
  enabled); posts reminder notifications, or hands off to...
- `AzanPlaybackService` — foreground service, plays the real azan audio via
  native `MediaPlayer` when the app is backgrounded. Explicitly **skips**
  playback if `MainActivity.isAppInForeground` is true (the WebView's own
  `<audio>` already plays it in that case) — this guard is what prevents a
  double azan; it depends on the WebView genuinely being paused when
  backgrounded (see above).
- `SilentModeReceiver` — mutes/restores ringer volume around azan, reason-aware
  (`REASON_BEFORE`/`REASON_AFTER`) so the "mute before azan" and "mute after
  azan" features can both be active without one clobbering the other's window.
- `NativeEventLog` — persistent (SharedPreferences) history of the azan
  lifecycle (alarm fired, native played/skipped, app paused/resumed), exposed
  to JS via `MobileJsBridge.getNativeEventLog()` and merged into the in-app
  debug console on load — lets you review a full day of usage after the fact,
  including whatever happened while the app was closed.

Other native pieces: `ReciterDownloader`/`ReciterManager`/`AzanCatalogManager`
(Quran reciter downloads), `TawkitWidgetProvider` (home-screen widget),
`AppUpdateChecker`/`AppUpdateDownloader` (GitHub-release-based update check),
`BootReceiver`/`AutoStartPrefs`/`DeviceType` (device-aware boot behavior —
silent relaunch on phones, always-on foreground launch on Android TV boxes).

## Where to look first

- Modifying WebView content (index.html, styles, in-app UI/behavior) →
  `app/src/main/assets/CLAUDE.md`. This is the file with the Golden Rule
  (core files are never edited; all customization goes in `spec/`).
- Modifying native Android behavior (alarms, notifications, bridge methods,
  background services) → `app/src/main/java/net/tawkit/mobile/`, cross-
  referencing the JS side via `MobileJsBridge` (`@JavascriptInterface`
  methods = the full JS↔native surface).
- Modifying the Hijri-date admin tool → `hijri-admin/` (own small Kotlin
  app, no relation to `app/`'s codebase beyond sharing the Supabase project).
- Building/signing/installing either app, or checking/bumping version →
  `release/instapk.ps1` / `release/instapk_hijriadmin.ps1` (`help` action
  lists everything).
