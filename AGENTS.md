# AGENTS.md — working guide for xDrip+

Practical orientation for agents (and humans) working in this repository. Read the relevant
`Documentation/technical/` doc before starting a migration task; this file is the map.

## What this is

xDrip+ is an Android CGM (continuous glucose monitoring) app. Historically Java + XML/Data
Binding; new UI is written in **Kotlin + Jetpack Compose**. A large, incremental migration is in
progress (UI → Compose, ORM → Room). Medical data integrity and backwards compatibility matter.

## Build & tooling

- Gradle **9.7.1**, AGP **9.4.1**, JDK **17** toolchain (the system JDK may differ; the local
  `~/.gradle/gradle.properties` pins a JDK 17, not committed). Note the wrapper itself needs
  `JAVA_HOME` (or `java` on `PATH`) to launch — `org.gradle.java.home` only pins the daemon after
  start. Add `JAVA_HOME=<jdk17> ./gradlew …` if `java` is not on `PATH`.
- `compileSdk = targetSdk = 34`, `minSdk = 26`, `namespace`/`applicationId` = `com.eveningoutpost.dexdrip`.
- Flavors `fast` (dev) and `prod`; build types `debug`/`release`; **R8 minification is enabled for
  debug and release**.
- Modules: `:app` (main), `:libkeks`, `:libglupro`, `:ipluginda`, `:localeapi`, `:wear` (legacy —
  currently does not build under AGP 9).

Commands:

```bash
# fast local loop (frontend/unit tests)
./gradlew :app:testFastDebugUnitTest
./gradlew :app:assembleFastDebug          # R8-verified build

# single test class
./gradlew :app:testFastDebugUnitTest --tests "com.eveningoutpost.dexdrip.ui.settings.SettingsActivityTest"

# CI-equivalent (.github/workflows/unit_test.yml)
./gradlew assembleProdRelease testProdReleaseUnitTest
```

Always run the unit suite + `assembleFastDebug` before committing. There is no ktlint/detekt/spotless.

## Where things live (`app/src/main/java/com/eveningoutpost/dexdrip/`)

- `models/` — data models and their **stable façades** (`BgReading`, `Calibration`, `Treatment`, …),
  now Room-backed.
- `db/` — Room: `AppDatabase`, `dao/`, `Migrations`, `LegacyDataImporter`.
- `ui/` — Compose + UI: `theme/` (`XdripTheme`, `ThemeColor`), `drawer/`, `home/`, `settings/`,
  `chart/`, plus legacy activities.
- `services/` — collectors and foreground services; `cgm/` — CGM integrations; `watch/` — watch
  integrations; `stats/`, `cloud/`, `profileeditor/`, `insulin/`, `tables/`, `utils/`.
- Key entry points: `xdrip.java` (Application), `Home.java` (main screen), `utils/Pref`
  (prefs), `utils/Preferences.java` (legacy settings).

## Tech stack & direction

- **UI:** Compose (BOM `2024.09.03`) + Material 3. Legacy XML + Data Binding still on most screens
  (hybrid migration).
- **Theme:** `XdripTheme`; every colour defaults to **Material You** and user picks override it
  (registry `ui/theme/ThemeColor`). Never hardcode colours.
- **Data:** Room (`2.8.5`); ActiveAndroid is retired. Legacy DB file is `DexDrip.db`.
- **Settings:** Compose host in `ui/settings/`; the legacy `android.preference` UI remains for
  unmigrated screens.
- **Charts:** `hellocharts` (dead local AAR) → Vico planned.
- **DI:** Dagger (`2.25.4`); Hilt planned. Lombok is pervasive in Java.

## Conventions & invariants

- **Preference keys and types never change** — settings are user data; backups/installs must keep
  working. Reads go through `Pref`/`PreferenceManager`.
- **Kotlin-first for new UI**; do not convert existing Java to Kotlin (`Documentation/technical/Kotlin_Policy.md`).
- **No new `hellocharts`**; **no new `ObservableMap`/Data Binding** — use `ViewModel`/state.
- **Screen-by-screen parity** before deleting any legacy layout/XML.
- Migrate/extend in small commits; keep the suite green.

## Testing

- Robolectric-based; base class `RobolectricTestWithConfig`, application `TestingApplication`;
  `@Config(sdk = [BuildConfig.targetSDK], application = TestingApplication::class)`.
- Compose tests: `createAndroidComposeRule<SettingsActivity>()` / `createComposeRule()`. Use
  `performScrollTo()` before clicking off-screen rows; **dialogs are separate compose roots**; keep
  `testTag`s on rows.
- No screenshot tests — assert state/keys/semantics.
- Caveat: `Pref` caches a static `SharedPreferences`; across Robolectric test methods the cache can
  point at a stale instance — prefer fresh reads or reset where it matters.

## Migration docs (map)

| Doc | Type | Use it for |
| --- | --- | --- |
| [`Compose_Migration.md`](Documentation/technical/Compose_Migration.md) | umbrella | Strategy, **actual order & why**, phase board, design decisions, conventions, risks |
| [`Settings_Migration.md`](Documentation/technical/Settings_Migration.md) | living plan | Settings architecture, status board (S0–S6), per-category change lists, **how to port a category**, verification passes A–K |
| [`Settings_S5a_Advanced.md`](Documentation/technical/Settings_S5a_Advanced.md) | living annex | The large `pref_advanced_settings` phase, section by section |
| [`ActiveAndroid_to_Room.md`](Documentation/technical/ActiveAndroid_to_Room.md) | record | Room migration why/how/order (complete) + deferred follow-ups |
| [`Compose_Library_Replacements.md`](Documentation/technical/Compose_Library_Replacements.md) | map | Legacy UI libs/widgets → Compose replacement (hellocharts→Vico, pickers/search, RemoteViews surfaces) |
| [`Tech_Debt.md`](Documentation/technical/Tech_Debt.md) | register | Dependencies/AARs/frameworks to replace/retire, with status |
| [`Kotlin_Policy.md`](Documentation/technical/Kotlin_Policy.md) | policy | Java/Kotlin interop rules |
| [`iLet_Pump.md`](Documentation/technical/iLet_Pump.md) | feature | iLet (Beta Bionics) read-only pump driver: protocol, mapping, safety exclusions, credential handling |

Other docs (`BlueJay_Tasker.md`, `Incoming_Glucose_Broadcast.md`, `Local_Web_Services.md`) cover
specific features.

## Current focus

- **Compose Phase 4 — settings.** S0–S5b done: the whole **Advanced** category
  (`pref_advanced_settings`, incl. watches) and the **xDrip+ Extra Settings** tree
  (`xdrip_plus_prefs`: display/graph/number-wall/accessibility, copying, update, motion, pens,
  prediction, sync) are migrated, plus theme-editor colour-group parity with the legacy screen.
  **Track V pass 1** (settings sub-menu quick wins), **pass 2** (trivial screens: `Agreement`,
  calibration check-in/override, double calibration, daydream settings, health privacy, fake
  numbers), **pass 3** (Medium small Data-Binding screens: `MtpConfigureActivity`,
  `DatabaseAdmin`, `GluProActivity`), **pass 4** (rich Medium: `EmergencyAssistActivity`,
  `BackupActivity`), **pass 5** (sensor/calibration forms: `NewSensorLocation`, `StopSensor`,
  `AddCalibration`, `StartNewSensor`, `SnoozeActivity`) and **pass 6** (admin quick wins:
  `SaveLogs`, `NumberWallPreview`, `DisplayQRCode`, `SendFeedBack`) are done as in-place Compose
  (`ui/secondary/`); remaining secondary views (alert/editor/table/admin) are deferred.
  Next: **S6** retire the legacy settings subsystem (deferred until on-device testing). Track V
  (secondary views) continues.
- **Phase 2 (Home)** is paused (header rendering reverted pending redesign); **Phase 3 (charts →
  Vico)** is the unblocker.
- See `Settings_Migration.md` for the exact status board and `Compose_Library_Replacements.md`
  for the legacy-library/widget replacement map.

## Gotchas learned so far

- **Settings:** `SettingsState` is a reactive `Pref` snapshot; **gates must read through it**
  (`SettingsVisibility.isEngineeringMode(state)`) to recompose. The host resets scroll on
  navigation. Long `ListPreference` dialogs need a **bounded `LazyColumn`**. `SettingsTimeRow`
  stores millis. Legacy-colour picks are mirrored into overrides + a one-time migration runs in
  `IdempotentMigrations`.
- **Room:** `AppDatabase.getInstance()` blocks on `LegacyDataImporter.awaitImportComplete()`;
  `getInstanceWithoutImportWait()` is the non-gating path (e.g. `UserError`).
- **Chart:** Home pan is clamped horizontal by `ui/chart/Horizontal*LineChartView` stopgaps —
  delete when Vico replaces the chart.
- **targetSdk 34 runtime traps:** dead AARs register receivers flag-lessly (PebbleKit,
  amazfitcommunication) — self-register their receiver classes with
  `ContextCompat.registerReceiver(…, RECEIVER_EXPORTED)`, or pass a `ContextWrapper` that forces the
  flag; every foreground service must declare `android:foregroundServiceType` (`Tech_Debt.md` §7 has
  the audit and the remaining Play Services `PendingIntent` debt).
