# Jetpack Compose Migration

## Overview

xDrip+ is being migrated from its legacy XML View + Data Binding UI to
[Jetpack Compose](https://developer.android.com/jetpack/compose), alongside a
broader modernization of the design and the AndroidX dependency stack.

This document records the strategy and the design decisions made along the way,
so that the work can continue incrementally and consistently.

---

## Current State

| Area | Before migration |
| --- | --- |
| UI framework | XML layouts + Android Data Binding (`bindingcollectionadapter2`, `me.tatarka`) |
| Navigation | Legacy `NavigationDrawerFragment` + `NavDrawerBuilder` |
| Charts | `hellocharts` (local, unmaintained AAR) |
| Source | ~980 Java files, 2 Kotlin files, ~172k LOC |
| Activities / Fragments | 37 / 10 |
| `targetSdkVersion` | 26 |
| Key libraries | `appcompat 1.0.0`, `material 1.1.0`, `constraintlayout 1.1.3`, `recyclerview 1.0.0`, `preference 1.0.0` |

The single most complex screen is `Home.java` (~3,800 LOC) which owns the main
dashboard, charts, and most of the shared state.

### Where we are now

Foundation and Phase 1 are done; the app is on **AGP 9.4.1 / Gradle 9.7.1**,
`targetSdk 34`, builds minified for `debug` and `release` (R8 fixed), and is
verified running on the emulator (including background collection restarts).
Phase 2 has its component library seeded and the header state extracted, but the
header *rendering* was reverted to the original design pending a proper redesign.

**Phase 4 (settings) is in progress:** a Compose settings host
(`ui/settings/SettingsActivity`) with a hand-rolled row library renders the **General**,
**Alarms and Alerts**, **Data Source**, **Data Sync**, the whole **Advanced** category
(Other settings and Smart watch features), the **Theme editor** (Compose colour picker, ringtone/
time/slider rows, root search, legacy colour-group parity) and the **xDrip+ Extra Settings** tree
(Copying, Update, Motion, Pens, Prediction, Sync, Display/graph/number-wall), and links to the
legacy settings activity for the rest. The theme now provides the app's data colours
(`LocalXdripColors`) to Compose. Plan, status board and verification passes live in
[`Settings_Migration.md`](./Settings_Migration.md); the large Advanced phase has its own annex
[`Settings_S5a_Advanced.md`](./Settings_S5a_Advanced.md). Legacy UI libraries/widgets and their
Compose replacements are catalogued in
[`Compose_Library_Replacements.md`](./Compose_Library_Replacements.md).

---

## Strategy: Incremental Hybrid

A full rewrite of 37 activities and ~100 layouts in one pass is not viable for a
medical app of this size. The migration is **incremental and hybrid**:

1. Existing screens keep working while being replaced one at a time.
2. New UI is written in Compose and embedded via `ComposeView` inside existing
   activities, and legacy components (notably charts) are embedded via
   `AndroidView` until replaced.
3. The global navigation shell (drawer) is migrated early to establish the
   Compose navigation pattern.

### Actual order & why

The work did **not** follow the nominal 0→5 order. This is the real sequence and the reasoning:

1. **Foundation (Phase 0).** Required before any Compose.
2. **ActiveAndroid → Room (parallel track, done first).** The data foundation for
   Home/charts/settings; doing it before the UI work avoids rewriting data access twice, and
   medical-data integrity needed dedicated focus. See
   [`ActiveAndroid_to_Room.md`](./ActiveAndroid_to_Room.md).
3. **Phase 1 (theme + drawer).** Establishes `XdripTheme`, the colour system and interop patterns,
   and migrates the navigation shell early.
4. **Phase 2 (Home) — started, then paused.** The header state was extracted, but the rendering was
   reverted pending a redesign and the charts are blocked on Phase 3 (Vico).
5. **Phase 4 (settings) — prioritised next.** Self-contained, low-risk, retires a large legacy
   subsystem, and its components are reused by Track V. Within it: framework → smallest pilot →
   next-simplest category → gating-heavy categories → the bulk → retire legacy last.
6. **Phase 3 (charts → Vico), then Phase 5 (secondary views).** Phase 3 unblocks the rest of Home.

| Phase | Scope | Status |
| --- | --- | --- |
| 0 | Foundation: dependency upgrades, Compose setup, `targetSdk 34` + AGP 9.4.1 upgrade + runtime correctness sweep | **Done** |
| 1 | Theme (Material You) + interop patterns + drawer content migration | **Done** |
| 2 | Home dashboard (component library + slice-by-slice; charts via `AndroidView`) | **Paused** (state extracted, rendering reverted pending redesign) |
| 3 | Charts → Vico (line graphs; basal column editor last) | Planned (unblocks Phase 2) |
| 4 | Settings → Compose ([`Settings_Migration.md`](./Settings_Migration.md)) | **In progress** (S0–S5b done; S6 remain) |
| 5 | Secondary views → Compose (long tail; same doc) | **In progress** — Track V passes 1–6 done (quick wins, trivial, Medium Data-Binding, rich Medium, sensor/calibration forms, admin quick wins); see `Settings_Migration.md` §7 |

### Parallel modernization tracks (own backlog, not UI phases)

These are large enough to run independently of the Compose phases:

| Track | Why it matters | Status |
| --- | --- | --- |
| ActiveAndroid → Room | Data foundation for Home/charts (`BgReading`/`Calibration`/`Treatment`). A Room + `Flow` source makes Compose slices much simpler. Recommended **before** the deep Home slices. | **Done** (see `Tech_Debt.md` §5) |
| Nightscout SDK → port AndroidAPS `core/nssdk` | Replaces the unmaintained `ns-sdk-full-release.aar` with Nightscout v3 + Access Token support. | Deferred |
| Dagger → Hilt | Modern DI for new ViewModels. | Not started |
| Lombok reduction | Long-term, optional. | Not started |
| Wear module | Old support libs; does not build under AGP 9. | Not started |

### Structural conventions

- **State extraction first.** Migrated Home slices extract a `ViewModel`/state holder before
  rendering (proven: `NavDrawerMenuState`, `HomeGlucoseState`). Settings uses a lighter
  `SettingsState` (`Pref`-backed snapshot) rather than a `ViewModel`, since screens only need
  row-level state; add a `ViewModel` only for cross-row derived state.
- **Compose over composition.** Prefer thin delegate-based bridges when a legacy
  collection/view must be touched (as in `PrefsView*`).

---

## Phase 2 — Home dashboard

`Home.java` (~3,800 LOC) + `BgGraphBuilder.java` (~2,500 LOC) are the largest,
most tightly-coupled surfaces in the app (Data Binding + `hellocharts` + viewport
syncing + treatment/voice logic). It is migrated **slice-by-slice**, not in one
pass:

1. **Component library first.** Build small, reusable composables in
   `ui/home/` that mirror Home's visual language and use the Phase 1
   `XdripTheme` + `xdripColor` (reactive data colors). Seed: `CurrentGlucose`
   (value + delta, colored low/in-range/high via `ColorCache`).
2. **Chart stays `hellocharts`**, wrapped via `AndroidView` when its area is
   converted. Do not re-derive the glucose→screen mapping yet (that's Phase 3).
3. **Slice order** (each lands independently, feature-parity checked):
   a. Header (current glucose + delta + trend arrow).
   b. Status lines (notices, extra status, battery, sensor age).
   c. Treatment / note / undo-redo action cluster.
   d. Time-range buttons.
   e. Nano/expiry status + source-wizard rows.
4. **State extraction**: pull the shared screen state out of `Home.java` into a
   `ViewModel`/`StateFlow` as each slice is migrated, so the chart (via
   `BgGraphBuilder`) and the Compose UI read from the same source.

**Interop pattern** (used when a slice needs a legacy `View`): Compose's
`AndroidView(factory, modifier, update, onRelease)` embeds a framework view
inside the composable tree; `factory` creates it, `update` syncs state, and
`onRelease` releases resources. This is the bridge for `hellocharts` until Phase 3.

### Header slice — deferred (state extracted, rendering reverted)

The header state is now extracted into `HomeGlucoseState`, which captures the
value, delta, level, and the former text-formatting edge cases as **semantic
flags** (`isStale`, `isFiltered`, `isNoise`, `isPredictive`, `fromPlugin`,
`slopeArrow`). This is populated by `Home.java` at the point the level color is
computed, and is view-agnostic.

The **rendering was reverted to the original design** (big value + graphical
trend arrow, with the delta in the notices/status area) pending a proper
redesign. A Compose header (edge cases → status pills) was prototyped and then
removed; when the redesign resumes, it will read the already-extracted
`HomeGlucoseState` flags.

Mapping retained for the future redesign:

| Legacy rendering | Semantic flag |
| --- | --- |
| strikethrough (stale data) | `isStale` |
| underline (filtered value) | `isFiltered` |
| italic + "⚠" prefix (noise) | `isNoise` |
| predictive slope arrow / uncalibrated | `isPredictive` |
| "P" prefix (plugin value) | `fromPlugin` |

---

## Design Decisions

### 1. Compose setup (done)

- Compose BOM `2024.09.03` (`material3`, `ui`, `ui-graphics`, `ui-tooling-preview`).
- `androidx.activity:activity-compose` and `androidx.navigation:navigation-compose`.
- The Kotlin 2.2 **Compose compiler plugin**
  (`org.jetbrains.kotlin.plugin.compose`) — no `composeOptions` block is needed.
- `buildFeatures { compose true }` in `app/build.gradle`.

### 2. Charting: wrap `hellocharts` first, adopt Vico later

`hellocharts` is an abandoned local AAR that is deeply embedded (the Home/BGHistory
line charts with synchronized viewports and the interactive basal column editor).

**Decision:** do **not** migrate charts to a Compose library upfront.

- Short term: wrap `hellocharts` in `AndroidView` so screens can go Compose
  without re-deriving the glucose→screen mapping in `BgGraphBuilder`.
- Mid term: replace the **line graphs** (`Home`, `BGHistory`) with
  [Vico](https://github.com/patrykandpatrick/vico), a Compose-native time-series
  chart library, reusing the existing `BgGraphBuilder` data pipeline.
- Last: replace the interactive **basal column editor** (custom drag-to-edit
  behavior that no Compose chart library reproduces out of the box).

Rationale: gets the Compose win fast while de-risking the critical graph
rendering in a medical device app.

### 3. JDK 17 toolchain

The project targets Java 17. The default system JDK on the primary dev machine is
JDK 26, which is incompatible with the build (Lombok 1.18.42 cannot process
`@val`/`val` on JDK 26, and Gradle 8.14.3's Groovy cannot read JDK 26 class
files).

- Every module now declares `java { toolchain { languageVersion = 17 } }`
  (`app`, `libkeks`, `wear` already had it; `libglupro`, `ipluginda`,
  `localeapi` were missing it and have been fixed).
- Locally, `org.gradle.java.home` is pinned to a JDK 17 in
  `~/.gradle/gradle.properties` (machine-local, not committed).

### 4. Dependency upgrades (done)

| Dependency | Before | After |
| --- | --- | --- |
| `targetSdkVersion` | 26 | 34 |
| `appcompat` | 1.0.0 | 1.7.0 |
| `material` | 1.1.0 | 1.12.0 |
| `constraintlayout` | 1.1.3 | 2.1.4 |
| `recyclerview` | 1.0.0 | 1.3.2 |
| `preference` | 1.0.0 | 1.2.1 |
| `androidx.collection` | 1.0.0 (transitive) | 1.4.4 (via Compose) |

### 5. `androidx.collection` 1.4 compatibility — composition (done)

`androidx.collection` 1.4.x rewrote `SimpleArrayMap` in Kotlin with generic
methods (`get(K)`, `indexOfKey(K)`), while `ArrayMap` (Java) keeps the
non-generic `Map` bridge (`get(Object)`). This makes it **impossible** for a
Java subclass to override `get()` (javac reports "name clash … same erasure").

`PrefsViewString` and `PrefsViewImpl` (the transparent preference-binding maps)
used to override `get(Object)` for lazy loading. They now use **composition**
instead of inheritance:

- They extend Guava's `ForwardingMap<K, V>` (already a dependency) and implement
  `ObservableMap<K, V>`.
- Storage and change-notification are delegated to an internal
  `ObservableArrayMapNoNotify` instance.
- `get` is a plain interface method (no generic/non-generic clash) and does the
  lazy read + `putNoNotify` caching as before.
- The two `ObservableMap` callbacks forward to the delegate, so two-way binding
  (`@={prefs[...]}` / `@={sprefs[...]}`) keeps working.

This is the pattern to follow elsewhere: **prefer composition over extending
framework collections**, and avoid new `ObservableMap` + `@={...}` bindings in
favor of `ViewModel`/`StateFlow`.

### 6. `targetSdk` 34 manifest changes (done)

Bumping to `targetSdk 34` required explicit `android:exported` on 24 components
(receivers, services, activities) with intent filters. Values were chosen per
component:

- `exported="true"` for launcher, system-broadcast receivers
  (BOOT_COMPLETED, power, headset, bluetooth, package-added), widget receivers,
  NFC/USB, and companion-app receivers (Nightscout Client, LibreLink, Aidex,
  ThinJam).
- `exported="false"` for Firebase messaging (`GcmListenerSvc`,
  `MyInstanceIDListenerService`) and the internal `SendFeedBack` activity.
- `exported="true"` for system-bound services (`UiBasedCollector`
  notification-listener, `AlwaysOnDisplayService` accessibility) per the
  Android 12 guidance.

### 7. `targetSdk` 34 runtime corrections (done)

Beyond the manifest `android:exported` work, bumping to 34 surfaced several
runtime requirements that crash on startup:

- **`registerReceiver` flags** — added `RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED`
  to all context-registered receivers (companion-app receivers exported; system
  and internal receivers not). Two hidden cases were inside dead AARs:
  - **PebbleKit**: `PebbleKit.registerReceivedDataHandler`/`registerReceivedAckHandler`/
    `registerReceivedNackHandler`/`registerDataLogReceiver` call the flag-less
    `Context.registerReceiver`. `PebbleWatchSync` now registers the same PebbleKit receiver classes
    itself via `ContextCompat.registerReceiver(..., RECEIVER_EXPORTED)` (and unregisters on destroy).
  - **amazfitcommunication**: `TransporterClassic.get()` registers
    `com.huami.watch.transport.DataTransportService.Start` on the application context with the
    flag-less overload. `Amazfitservice` now passes a `ContextWrapper` (`FlaggedReceiverContext`)
    that forces `Context.RECEIVER_EXPORTED` on the two-arg `registerReceiver` (and returns itself
    from `getApplicationContext()` so the library's internal call is intercepted).
- **`PendingIntent` flags** — added `FLAG_IMMUTABLE` everywhere, including a
  `TaskStackBuilder.getPendingIntent` call the initial single-line scan missed.
- **Foreground-service types** — declared `FOREGROUND_SERVICE_CONNECTED_DEVICE`
  and `FOREGROUND_SERVICE_DATA_SYNC` permissions, and dropped the `location`
  type (a BLE app, not a GPS tracker) so services can actually reach
  `startForeground` on Android 14.
- **Bluetooth runtime permissions** — the manifest had `tools:node="remove"` on
  `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` (a targetSdk-26 leftover). Re-declared
  them (`BLUETOOTH_SCAN` with `neverForLocation`) and enabled the Android 12+
  path in `LocationHelper` (`newType` flag removed), so BLE scanning/connection
  actually works on real hardware.
- **Background foreground-service starts** — Android 12+ disallows starting a
  foreground service from the background. `CollectionServiceStarter` now routes
  background collection restarts through an exact alarm
  (`setExactAndAllowWhileIdle`) + `WakeLockTrampoline`, whose firing grants the
  temporary allow-list needed for `startForegroundService`. The in-process
  Handler path is kept only for foreground restarts (where the full
  stop-all/start-correct-collector logic is needed). The direct
  `startForegroundService` call sites are also wrapped so they log instead of
  crashing when the allow-list is absent. Non-foreground collectors (UiBased,
  a `NotificationListenerService`) are excluded from the exact-alarm restart
  path entirely, since they can never call `startForeground()` — starting one
  would still be killed by `ForegroundServiceDidNotStartInTimeException`.
  `UiBasedCollector` now also reports listener-enabled health through
  `isCollecting()`, which `MissedReadingService` reads via reflection.

### 8. Theme & color system (Phase 1)

Two-layer color model:

- **Every colour has a Material You default.** The app resolves its `ColorScheme` from the
  device theme (`isSystemInDarkTheme()`; Android 12+ dynamic / Material You, otherwise the
  brand fallback palette), then applies any **user overrides** on top.
- The full set of colours lives in one registry, `ui/theme/ThemeColor` — the Material 3 chrome
  roles *and* the data/chart colours (high/low/in-range, chart lines/backgrounds, basal, number
  wall, …). Data colours keep their legacy `ColorCache` key so legacy screens stay in sync.
- **Overrides win over Material You.** Each `ThemeColor` has an override key; absence means
  "follow Material You". `resolveXdripColorScheme()` applies the chrome overrides (inside
  `XdripTheme`), and `xdripColor()` / `LocalXdripColors` resolve data colours as
  `override ?: Material default`. The rule is implemented in `ui/theme/ThemeColor.kt`.
- A Compose **Theme editor** (`ui/settings/ThemeEditorScreen.kt`) lists every colour with a
  swatch, the Compose picker, a per-colour "use Material You default" and a global reset.

`ColorCache` is a static cache with manual `invalidateCache()` and no observers, so it
needs a change-notification bridge to be reactive in Compose: a Kotlin `StateFlow`-based
bridge (`ColorCacheBridge`) updates `LocalXdripColors` on invalidation so Compose recomposes
when a color is picked. Legacy `color_*` writes are mirrored into overrides via a
preference-change listener, and a one-time migration copies any non-default legacy colour
into an override so existing customisations survive the switch to Material You defaults.
S6 replaced the legacy colour widgets with `com.github.skydoves:colorpicker-compose` (the
shared `ColorPickerDialog`), so the `color_*` keys are now written directly by Compose.

---

## Dependency Migration Register

The full inventory of dependencies to replace or retire — Compose targets, dead AARs,
legacy frameworks, and the Wear module — lives in
[`Tech_Debt.md`](./Tech_Debt.md); the per-library **UI replacement map** (legacy lib/widget →
candidate Compose replacement → files → phase) lives in
[`Compose_Library_Replacements.md`](./Compose_Library_Replacements.md).

Highlights:

- `hellocharts` → Vico (Phase 3); `colorpicker` / `search-preference` → Compose and **removed**
  in S6 (colour picks now use `colorpicker-compose`). The chart preview inflates
  `prefs_example_chart_layout` inside an `AndroidView`; the RemoteViews/lockscreen/dream chart
  surfaces need a non-Compose drawing path (see the map).
- ActiveAndroid ORM → Room is **done** (all 28 tables + `Libre2Sensors` on Room; ActiveAndroid
  retired). The data layer is now a good foundation for the Home/chart Compose work; a future
  `Flow` pass over the DAOs will make Compose screens reactive (see `Tech_Debt.md` §5).

---

## Conventions Going Forward

- **Kotlin-first for new UI.** New screens/components are Kotlin. Java classes are
  consumed, not extended (see `Documentation/technical/Kotlin_Policy.md`).
- **State in `ViewModel`/`StateFlow`**, not `ObservableMap` + two-way binding.
- **No new `hellocharts` usage**; route new charting through the Vico migration.
- **No hardcoded colours.** Material You is the default for every colour; user overrides
  (registered in `ui/theme/ThemeColor`, resolved by `resolveXdripColorScheme` / `xdripColor` /
  `LocalXdripColors`) always win over those defaults.
- **Thin, delegate-based bridges** for anything that must still touch the legacy
  binding/collection stack (as done for `PrefsView*`).
- **Screen-by-screen feature parity** with manual + UI tests before deleting the
  legacy layout.

---

## Risks / Open Items

- **`targetSdk 34` runtime behavior** (foreground-service types, exact-alarm,
  notification permission, scoped storage) is built but not yet device-tested.
- **`hellocharts` is a dead dependency** — do not let `AndroidView` wrapping
  become permanent.
- **Home chart pan is clamped to horizontal** by `ui/chart/HorizontalLineChartView` /
  `HorizontalPreviewLineChartView` (hello-charts' `ZoomType` limits zoom only, not pan). These
  are stopgaps to delete when Phase 3 replaces the chart with Vico.
- **Data Binding remains in use** across most screens until migrated; the
  `PrefsView*` composition bridge will be removable once those bindings are gone.
- **Dynamic color fallback**: Android < 12 devices use the brand fallback scheme;
  legacy screens remain Holo-themed until migrated (a temporary visual mismatch
  between Compose and legacy surfaces).
- **R8 produces a malformed dex (`Out-of-order annotation_element name_idx`)** on
  minified builds, which ART rejects at startup (`ClassNotFoundException` for the
  application class). Root cause: R8 emits out-of-order annotation elements for
  `dalvik.annotation.MethodParameters` (the `accessFlags` element) and
  ActiveAndroid's `@Column` (`name`). A `-keep` / `-keepattributes` workaround
  does **not** help. **Fix:** upgraded to AGP 9.4.1 (newer R8) and re-enabled
  minification for `debug` and `release`. (ActiveAndroid has since been removed,
  so its `@Column` contributor no longer applies.) This also required migrating several
  deprecated AGP APIs (`applicationVariants` → `androidComponents`,
  `kotlin-android` → built-in Kotlin, `compileSdk =`, non-final `R.id`, and
  `resValues`/`wearApp` changes) — see the `AGP 9` notes.
