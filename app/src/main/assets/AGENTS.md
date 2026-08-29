# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

Tawkit is a browser-based Islamic prayer times display application (tawkit.net). It runs as a fully static HTML page — no build system, no server required. Open `index.html` in a browser to run it.

## The Golden Rule: Custom Files Only

**All local modifications must go exclusively in `spec/custom.js`, `spec/custom.css`, and `spec/mosques-registry.js`.** Never modify core files (`m2body.js`, `m1prime.js`, `style1.css`, `style2.css`, `style0.css`, `settings-defaults.js`, `languages/lang-*.js`, etc.). Core files are overwritten on every app update. Never edit `spec/mosquee.js` — it is a selector shim, not a config file (see below).

All real mosque configuration lives exclusively in Supabase (single table `mosques`, editable anytime via the `mosque-admin` app — see the dedicated section below), never in per-mosque `.js` files and never baked into the APK. This used to be split between an embedded `spec/mosques-registry.js` and Supabase, which caused real data-drift bugs (a stale embedded value silently winning over a corrected Supabase value after every APK release) — the embedded copy of real mosques was removed for that reason (August 2026).

`spec/mosques-registry.js` now contains **only** the `anonymous.generic` entry — not a mosque, but the offline bootstrap template used on a brand-new install with zero network connectivity (see `_ucFirstRunFallbackAnonymous` in `custom.js`). Never add a real mosque back into this file.

`spec/mosquee.js` is loaded before `spec/custom.js` and picks the right config into `window.MOSQUE_CONFIG` — from that one embedded entry for `anonymous.generic`, or from Supabase-restored data (already written into `localStorage` by `_restoreFromJson`) for every real mosque.

## Script Loading Order

Understanding load order is critical to avoid timing bugs:

```
settings-defaults.js   → defines JS_DATA (core settings)
lang-XX.js             → defines JS_eLang (UI strings, based on JS_DATA.ucLangNOW)
ayats, ahadith, countries, messages, azkar files
m1prime.js             → prayer time calculation engine
  └─ inlines: m2body.js    → main app logic; calls initUILabels() at line ~8749
              spec/custom.css
              spec/mosques-registry.js  ← embedded mosque configs (all mosques)
              spec/mosquee.js  ← selector shim, sets window.MOSQUE_CONFIG
              spec/custom.js   ← our code runs last
```

`initUILabels()` runs **before** `custom.js` loads. This means:
- Overriding a `JS_eLang` string alone is not enough — the DOM has already been populated.
- Always update both the JS object AND the DOM element directly in `custom.js`.

## spec/mosques-registry.js — Offline Bootstrap Template Only

This file holds a single entry, `anonymous.generic` (flagged `_UC_ANONYMOUS: true`) — a generic placeholder config, not a real mosque. It exists purely so a brand-new install with zero network connectivity can still boot into a working state (`_ucFirstRunFallbackAnonymous` in `custom.js` sets `UC_MOSQUE_ID` to this id with no network round-trip needed). `index.html` loads it as a blocking, synchronous `<script>` tag before `spec/mosquee.js`, guaranteeing it's available offline on first paint.

**Never add a real mosque back into this file.** Real mosques used to be embedded here too, but that created a second, driftable copy of data already in Supabase — a stale embedded value could silently win over a corrected Supabase value after every APK release. All real mosque configuration is deployed exclusively via Supabase now (see below).

**Deployment procedure (any real mosque):** create/update its row in the single Supabase table `mosques` (via the `mosque-admin` app). No APK release needed; every device picks it up via the mosque selector in `custom.js` (`_ucSyncFromSupabase` → `_applyRow`), and stays in sync afterward via the polling loop (`_installRemoteConfigPolling`, `custom.js`).

## Supabase `mosques` — the single mosque referential (consolidation 27/08/2026)

There used to be **two** tables: `mosques` (structured columns) and
`mosque_config_backups` (the full serialized config blob + the new-mosque
proposal queue). They overlapped and drifted. `mosque_config_backups` has been
**dropped**; everything lives in `mosques` now:

- **Structured columns** (`jumua`, `iqama_delay`, `iqama_fixed`, `azan_offsets`,
  `time_flags`, `eid`, `profile`, `automation`, `quran_settings`, `pin_hash`,
  `image_url`, lat/lng, `mosque_name`, `location_code`, `status`) — authoritative
  for remote admin + the 18 s polling sync (`_applyRow` in `custom.js`).
- **`backup_json`** (jsonb) — the full device-config blob (same shape as
  `_buildBackup()`), used as the defensive full-restore fallback on import
  (`_pullRemoteBackup` → `_restoreFromJson`) and to carry an imam's complete
  config on a new-mosque proposal.
- **`status`** — `approved` (live, ~1814 rows: 9 administered + the whole Tunisia
  catalog promoted from `spec/mosquee/liste/osm_tunisia.json`) / `pending` (a
  proposal awaiting review in `mosque-admin`) / `rejected`.

custom.js reads/writes only `mosques`: `_fetchRemoteListByCity` &
`_fetchRemoteList` (selector list), `_pullRemoteBackup` (`backup_json`),
`_pushRemoteBackup` (single upsert: identity + profile + image + `backup_json`,
`status='pending'` only for a proposal), `_ucSyncFromSupabase` (`select=*`).

### `on_mosque_update` trigger — visible "prayer times updated" notification

`AFTER UPDATE ON mosques` → `supabase_functions.http_request` → `rapid-service`
(webhook mode) → OneSignal visible push "Prayer times updated" to every
subscriber of that mosque. Since migration
`mosque_update_notify_only_on_schedule_change` (29/08/2026) it carries a **`WHEN`
clause**: it fires **only** when a schedule column actually changes
(`azan_offsets`, `iqama_delay`, `iqama_fixed`, `jumua`, `eid`, `time_flags`,
`primary_azan`, `dohr_before_asr_min`, `quran_settings`). A write that touches
only `backup_json` / `profile` / `automation` / `mosque_name` / `image_url` no
longer spams subscribers. `updated_at` (via `trg_mosques_updated_at`) still
bumps on every write, so the 18 s polling sync is unaffected.

### Auto-export of `backup_json` after a remote edit (box only)

`rapid-service` (mode `remote_config_update`) PATCHes only the structured
schedule columns — never `backup_json`. So after an imam edits times/Quran
delays from their phone, the box's `backup_json` blob would go stale (a later
"Import config → Distant" or directory re-selection would restore old times).
`_installAutoExportAfterRemoteEdit` (`custom.js`, box only) fixes this: when the
box applies a remotely-received change (`ucConfigSync` listener /
`_installRemoteConfigPolling` set `localStorage.UC_PENDING_AUTO_EXPORT`), the
**next** page load (right after `_applyRow`'s reload) calls `_ucPushRemoteBackup`
to re-persist the full blob. Anti-reload-loop: that write bumps `updated_at`
without touching schedule columns → no notification (WHEN clause above) and
`_installRemoteConfigPolling` skips it (`UC_SELF_WRITE_INFLIGHT` +
`UC_SELF_WRITE_UPDATED_AT` guards). Traced as `SYNC AUTO_EXPORT_ARMED/START/OK/
ERR/SKIP`.

### Shared PIN field (`_installRemoteMosqueAdmin`)

Both tabs (Actions + Settings) share **one** PIN input, `#ucRAActionsPin`, placed
in `#ucRASharedPin` directly under the mosque-name display and above the tab bar
(was two separate fields `#ucRAActionsPin` / `#ucRAPin` that had to be retyped
when switching tabs). `_sendRemoteAction` and `_submit` both read
`#ucRAActionsPin`.

## _applyMosqueConfig() — Auto-reload on First Install

When `MOSQUE_CONFIG.VERSION` differs from the stored version in `JS_DATA_CUSTOM`:
1. All parameters are written to `JS_DATA` (in memory) and saved to `localStorage`
2. `JS_CUSTOM.ucMosqueConfigVersion` is set to `MOSQUE_CONFIG.VERSION` and saved
3. `location.reload()` fires after 200 ms

On the next load `storedVersion === VERSION`, the function returns immediately — **no infinite loop**. This also handles the RESET case (empty localStorage).

The stored version key is `JS_CUSTOM.ucMosqueConfigVersion` (inside `JS_DATA_CUSTOM`).

## QR Code / PS Flag Toggle (_patchPsFlagForQR)

When `MOSQUE_CONFIG.QR_ENABLED = 1`:
- `ucPsFlag = 1` (checkbox checked) → shows `ps.png` normally (circular, opacity 0.8)
- `ucPsFlag = 0` (checkbox unchecked) → shows QR code at the same position

QR image source priority:
1. `spec/images/qrmosquee.png` — pre-generated static PNG (preferred; works offline)
2. `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=<URL>` — dynamic (requires internet)

The PNG is probed asynchronously on load. If it exists, `_useLocal = true` and the local path is used. If the probe fails, the API URL is used (requires `QR_URL` to be non-empty).

CSS class `uc-qr-mode` is added to `#psFlagImageVertical` and `#psFlagImageHorizontal` when in QR mode; `custom.css` removes the circular `border-radius` and slightly enlarges the element.

**To generate the static PNG** (run once from the project root, requires internet):
```
powershell -ExecutionPolicy Bypass -File spec\genqr.ps1 -Url "https://maps.app.goo.gl/XXXXX"
```

## _patchJomoaDisplay() — Fix for "0AUTO" Bug

**Problem:** The internal variable `jomoaFixedTime` (a `let` in `m2body.js` scope, NOT on `window`) is only updated from `JS_DATA.ucJomoaFixedTime` inside `if (isFriday)`. On non-Friday days it retains its initial value, so `updateMosqueAndDateDisplayFunction()` displays `'0AUTO'` instead of the configured time (e.g. `'13:15'`).

**Solution:** `_patchJomoaDisplay()` monkey-patches `window.updateMosqueAndDateDisplayFunction` to call the original and then immediately overwrite `jomoaTimeDisplayHorizontal.innerHTML` with the correctly formatted value from `JS_DATA.ucJomoaFixedTime`. The patch is a no-op when `ucJomoaFixedTime` is `'AUTO'` or empty.

## Settings Storage

Two completely separate `localStorage` keys:

| Store | Key | Object | Purpose |
|---|---|---|---|
| Core app | `'JS_DATA'` | `JS_DATA` | All standard app settings (never add keys here) |
| Custom | `'JS_DATA_CUSTOM'` | `JS_CUSTOM` | Custom additions; persisted via `saveCustomSettingsFunction()` |

Defaults for custom settings are declared in `JS_CUSTOM_DEFAULTS` at the top of `custom.js`. `JS_CUSTOM` is then built with `Object.assign({}, JS_CUSTOM_DEFAULTS, storedValues)`.

## Event System (UC_EVT)

React to app lifecycle events from `custom.js` using:

```javascript
ucOn(UC_EVT.EVENT_NAME, function(e) { /* e contains event data */ });
```

Available events and their payloads:

| Event | Payload |
|---|---|
| `UC_EVT.AZAN_TIME` | `{ prayer, timeInMinutes, isDohaPrayer }` |
| `UC_EVT.AZAN_SHOW` | `{ prayer, timeInMinutes, isDohaPrayer }` |
| `UC_EVT.AZAN_HIDE` | `{}` |
| `UC_EVT.IQAMA_TIME` | `{ prayer }` |
| `UC_EVT.IQAMA_SHOW` | `{ prayer }` |
| `UC_EVT.IQAMA_HIDE` | `{}` |
| `UC_EVT.COUNTDOWN_TICK` | `{ remainingSeconds, minutes, seconds, display }` |
| `UC_EVT.BLACK_SHOW` | `{}` |
| `UC_EVT.BLACK_HIDE` | `{}` |

## Key Global State

- `remainingSeconds` — global countdown variable; readable and writable from console/custom.js
- `isIqamaCounterActive` — boolean; true while iqama countdown is running
- `isDohaCounter` — boolean; true when the current counter is for Doha (not a fard prayer)
- `JS_DATA.ucIqamaHadith` — 1 if the hadith-before-iqama feature is enabled
- `blackScreenVertical/HorizontalElement` — black screen divs; active when `style.transform === 'scaleX(1)'`

## Azan Popup Safety Timer

A `setTimeout` of **4 minutes** (240 000 ms) auto-closes the azan popup if `_ucReleaseAzanBlock()` was not called normally (e.g. app left unattended). This prevents the screen from staying locked on the azan overlay indefinitely.

## Counter Drift Correction (`_installCounterDriftCorrection`)

`clockTickFunction` fires via `setInterval` (~1 s). Under CPU load, ticks can run slightly late, causing the iqama countdown to show more seconds than the actual elapsed time. The IIFE installs a `UC_EVT.COUNTDOWN_TICK` listener that computes `expected = startRem - elapsed` using a `Date.now()` baseline and writes it directly to `remainingSeconds` if drift exceeds 2 s. The baseline resets on `UC_EVT.IQAMA_TIME`.

## Iqama Sequence Timing (m2body.js line ~1132)

When the iqama counter hits 0, `startIqamaSequenceFunction()` runs:
- T+0s: iqama popup shown (`iqamaPopupVertical`, `iqamaPopupHorizontal`)
- T+10s: popup hidden via `hideElementFunction`
- T+12s: `activateBlackScreenIfEnabled()` → black screen

The custom.js currently extends these durations by 5 s by overriding both functions.

## Testing in the Browser Console

Paste `spec/ucSim.js` into the browser console to install the simulator, then:

```javascript
ucSim()           // full automated simulation at x20 speed
ucSim(10)         // slower simulation at x10

// Step-by-step:
ucSim.azan()           // show azan popup
ucSim.closeAzan()      // close it
ucSim.counter(60)      // start iqama counter from 60 s (calls showIqamaCounter() internally)
ucSim.jump(33)         // jump to 33 s remaining (triggers hadith overlay)
ucSim.jump(15)         // jump to 15 s remaining (triggers second hadith overlay)
ucSim.blackScreen()    // activate black screen
ucSim.end()            // reset everything
```

You can also directly call core functions:
```javascript
startIqamaCounterFunction(1, 15, 1);  // (prayerIndex, iqamaMinutes, isTest)
startIqamaSequenceFunction();
activateBlackScreenFunction();
deactivateBlackScreen();
```

## Architecture of spec/custom.js

The file is organized in sections (in order):

1. **Version override** — patches the version string displayed in-app
2. **JS_CUSTOM_DEFAULTS / JS_CUSTOM** — custom settings definition and localStorage merge
3. **MOSQUE_CONFIG** — ternary alias to `window.MOSQUE_CONFIG` (from mosquee.js) with full fallback; followed by `_applyMosqueConfig()` IIFE which writes all parameters to JS_DATA, saves to localStorage, and triggers `location.reload()` on first run or after a VERSION bump
4. **`_patchJomoaDisplay()`** — monkey-patches `updateMosqueAndDateDisplayFunction` to fix "0AUTO" display for Jumu'ah time on non-Friday days
5. **`_patchPsFlagForQR()`** — when `QR_ENABLED=1` and `ucPsFlag=0`, replaces `ps.png` with the QR code image (local PNG or API fallback); adds/removes CSS class `uc-qr-mode`
6. **Light/automation config** (`_lightProgramConfig`) — trigger rules for external HTTP calls (amplifier, minaret, strobing) keyed on UC_EVT events
7. **Quran player** — overlay for playing Quran audio before azan with per-prayer scheduling
8. **UC_EVT bridge** — the `ucOn` / `UC_EVT` system (defined here; used throughout)
9. **Blinking prayer row** — highlights the active prayer row in the times table
10. **No-mobile reminder** (`_initNoMobileReminder`) — periodic overlay showing a "no phones" image; suppressed during black screen and modals
11. **Jumu'ah adab overlay** — Friday-specific overlay
12. **Iqama hadith overlay** (`ucIqamaHadithOverlay`) — shows a hadith at 33 s → 20 s before iqama
13. **15 s hadith overlay** (`ucIqama15sOverlay`) — shows JS_IqamaRULE hadith at 15 s → 7 s before iqama; uses `position:fixed; z-index:19999` to appear above the fullscreen counter
14. **Iqama popup text** — replaces the standard iqama text with Quranic verses (Al-Mu'minun 23:1-2); must update both `JS_eLang.cx_IQAMAT_SALAT` and the DOM directly
15. **Iqama popup duration extension** — overrides `hideElementFunction` and `activateBlackScreenIfEnabled` to add +5 s to the iqama popup display time
16. **`_installCounterDriftCorrection`** — IIFE; corrects `setInterval` drift on the iqama countdown using a `Date.now()` baseline; resets on `UC_EVT.IQAMA_TIME`
17. **`_fixFixedIqamaDisplayUpdate`** — IIFE; monkey-patches `editFixedIqamaXxxFunction` wrappers so that `updateAthanIqamaDisplayFunction()` is re-called after `calculateAndDisplayTimesFunction()` completes on the first dialog confirm (fixes stale display on initial value change)
18. **`_installIqamaBlinkGuard`** — IIFE; grays out / disables `iqamaZeroMinaretBlinkCb` when Minaret is unchecked or URL is empty; same for `iqamaZeroMihrabBlinkCb` when Mihrab is unchecked, URL is empty, or Mihrab URL equals Minaret URL
19. **`_installMihrabMinaretConflictGuard`** — IIFE; monkey-patches `_ucHttpCall` to silently block all Mihrab HTTP calls at runtime when the Mihrab URL matches the Minaret URL, preventing channel conflicts regardless of trigger path

## Common Pitfalls

- **`JS_eLang` override alone is insufficient** — `initUILabels()` has already set the DOM before custom.js runs. Always set `element.innerHTML` directly too.
- **Z-index wars** — `fullScreenCounterContainerHorizontal/Vertical` sits at z-index 999. Any overlay that must appear on top of the counter needs `position:fixed; z-index:19999` or higher.
- **`remainingSeconds` start value is not deterministic** — the formula is `(iqamaMinutes * 60) - getSeconds()` using the current wall-clock second at call time. Force a known value with `remainingSeconds = N` shortly after calling `startIqamaCounterFunction`.
- **RTL bracket rendering** — in RTL Arabic text, the Unicode open bracket placed at the logical start of a string renders visually on the right (curving outward). Swap brackets if you need an inward-facing one.
- **`const` at module scope** — `const` declarations in `m2body.js` (e.g., `blackScreenVerticalElement`) are NOT on `window`. Use `document.getElementById(...)` to access them from `custom.js`.
- **`showIqamaCounter()` must be called explicitly** — `startIqamaCounterFunction()` initialises the counter state but does not show the visual element. Always follow with `showIqamaCounter()` inside a short `setTimeout` (>=200 ms) after calling it.
- **`jomoaFixedTime` is not on `window`** — it is a `let` at m2body.js module scope and is only updated on Fridays. Always read `JS_DATA.ucJomoaFixedTime` directly and patch `updateMosqueAndDateDisplayFunction` if you need to reflect the value every day.
- **Edit tool truncates files with Arabic/Unicode** — when using the Edit or Write tool on files containing Arabic characters in comments or strings, the file may be silently truncated. Always verify with `node --check` and `wc -l` after any edit. Use Python `open(...,'rb').read()` / `open(...,'wb').write()` for surgical byte-level edits when the Edit tool truncates.
- **`updateAthanIqamaDisplayFunction()` called before recalculation** — core `editFixedIqamaXxxFunction` calls `updateAthanIqamaDisplayFunction()` before `calculateAndDisplayTimesFunction()`, so the display shows the old value until the next tick. Monkey-patch the wrapper and re-call `updateAthanIqamaDisplayFunction()` after `calculateAndDisplayTimesFunction()` completes; use `isInputDialogOpen === true` to target the confirm phase only (not the open phase).
- **`_lightFiredSet` can reset mid-countdown** — `applyNextPrayerHighlight` resets all `_lightFiredSet` entries whenever `_getUpcomingPrayerKey()` returns a different key. A transient prayer-key flicker (prayer-time recalculation, window focus) can therefore reset the guard mid-countdown and allow a `beforeAzan` item to fire twice. The `_lightLastFireMs` map provides a 30-minute persistent cooldown that survives `_lightFiredSet` resets; it is cleared on `UC_EVT.AZAN_TIME`.
- **Noisy per-tick logs in `_applyLightTriggers`** — any `_L()` call placed before the `if (remainingMinutes > thresholdMin) return` guard will fire on every countdown tick (≈ once per second). Always place informational logs after both the threshold check and the `_lightFiredSet` check so they execute at most once per prayer cycle.

## Debug Console (`debugConsoleContent`)

An in-app debug console is injected by `custom.js`. Key points:

- `_dbgLogs` is an array of `{ text: string, isError: bool }` objects (max `_MAX_LINES` entries).
- `_render()` uses `innerHTML` to draw lines; error entries are wrapped in `<span style="color:#ff4444;">`.
- A line is treated as an error if: `level === 'error'`, `level === 'warn'`, or the message contains `'HTTP_ERR'`.
- Browser-level resource load failures (CORS blocks, 404s, etc.) are captured via a window `'error'` listener in **capture phase** and injected as error lines:
  ```javascript
  window.addEventListener('error', function(evt) { ... }, true);
  ```
- The `_L(cat, verb, data)` helper formats all log lines as `[HH:MM:SS][CAT] VERB key=val ...`.

## HTTP Calls for Light Automation (`_ucHttpCall`)

```javascript
function _ucHttpCall(url, label, silent)
```

- `silent = true` suppresses the `[LIGHTS] HTTP` trace — used for intermediate strobe ticks.
- HTTP errors always produce a `[LIGHTS] HTTP_ERR` trace (never silent) → displayed in red in the debug console.
- Strobe logging: only the **start** (`strobe_start`) and the **final OFF** (`strobe_end`) are logged; the 500 ms ON/OFF ticks are called with `silent = true`.
- `_ucHttpCall` is monkey-patched by `_installMihrabMinaretConflictGuard` to silently block all labels containing `'mihrab'` / `'Mihrab'` when the Mihrab URL equals the Minaret URL.

### `beforeAzan` double-fire guard

Two complementary guards prevent a `beforeAzan` item from firing more than once per prayer cycle:

| Guard | Mechanism | Scope |
|---|---|---|
| `_lightFiredSet[key]` | Boolean; set on first evaluation (fire or skip) | Resets when `_getUpcomingPrayerKey()` changes |
| `_lightLastFireMs[key]` | Timestamp of last actual FIRE; 30-min cooldown | Persists across `_lightFiredSet` resets; cleared on `UC_EVT.AZAN_TIME` |

When Quran auto-start is configured, the effective threshold for `beforeAzan` items becomes `quranDelay + itemDelay`. The Quran-offset log is emitted once, just before the FIRE/SKIP decision (not on every countdown tick).

### Testing light triggers from the console

```javascript
ucTestLight()                         // list all configured items + enabled/URL state
ucTestLight('ampliExtOn')             // simulate full trigger (respects SKIP checks)
ucTestLight('ampliExtOn', 'BAD')      // force bad URL → HTTP_ERR → red line in console
```

## Marquee During Countdown

The `_installMarqueeDuringCountdown` IIFE manages the scrolling text banner lifecycle:

- **`remainingSeconds > 59`** — marquee is shown (once, guarded by `_marqueeShownByUs` flag).
- **`remainingSeconds === 59`** — marquee is hidden (once, guarded by `_marqueeHiddenByUs` flag).
- **`UC_EVT.IQAMA_TIME`** — marquee is restored if we hid it; both flags are reset for the next prayer cycle.

Both flags are required to prevent repeated `updateMarqueeDisplayFunction()` calls on every tick, which would cause visible flicker.

## Quran Player

### Storage keys (localStorage)

| Key | Content |
|---|---|
| `JS_QP_POSITION` (`_QP_STORAGE_KEY`) | `{ reciter, surah }` — last playing position |
| `_QP_STORAGE_SRC_KEY` | Last audio `src` URL |
| `_QP_STORAGE_TIME_KEY` | Last playback time offset |

### Error retry logic

- `_qpErrorCount` tracks consecutive audio errors (scope: `injectQuranPlayerModal` IIFE).
- `_QP_MAX_RETRY = 10` — after 10 consecutive errors the player stops and resets the counter.
- On each error, `_qpAutoAdvance = true` then `selectQPSurah(nextSurah)` — advances to the next surah (wraps at 114).
- `_qpErrorCount` resets to 0 on the `playing` event (successful playback).
- Error codes logged: `1=ABORTED`, `2=NETWORK`, `3=DECODE`, `4=SRC_NOT_SUPPORTED`.

### Server / reciter UI rules

1. **Server disabled** — player title turns red and shows a server error message instead of the Quranic title.
2. **No reciter enabled** — only an Arabic "no reciter selected" message is shown (no buttons).
3. **Server just unchecked** — `toggleQuranServerEnabledFunction()` calls `_clearQPCache()` which removes all three storage keys, resets state variables, and pauses+clears the audio element.

`openQuranPlayerModal()` calls `_refreshQPTitle()` first so the title always reflects the current server state when the modal opens.

## Android / GeckoView (APK build)

- The APK webview uses Firefox GeckoView. The page origin is `resource://android`.
- Audio fetches from `http://127.0.0.1:8080` are treated as cross-origin. The HTTP server **must** return `Access-Control-Allow-Origin: *` on every response; otherwise GeckoView blocks the request silently and reports it as error code **4 (`SRC_NOT_SUPPORTED`)** — not as a CORS error — which makes the root cause non-obvious.
- The retry loop will then exhaust all 10 attempts (all fail with `SRC_NOT_SUPPORTED`). The fix is purely server-side (configure HTTPSimpleServer to add the CORS header), not JavaScript.
- Logcat tag: `TWKT` — custom traces use this prefix (`TWKT:_LOAD_COMPLETED_`, `TWKT:TypeError:NetworkError`, etc.).
- `location.reload()` works correctly in GeckoView and is used by `_applyMosqueConfig()` on first install.
