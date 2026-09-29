# Settings & Secondary Views → Compose (plan)

## Overview

This document is the **plan** for the next large step of the Compose migration:
replacing the legacy **settings** subsystem (`Preferences.java` + the preference XMLs)
and the **secondary views** (the "long tail" of activities/fragments that are neither
Home nor the drawer shell) with Jetpack Compose.

It follows the strategy and conventions in
[`Compose_Migration.md`](./Compose_Migration.md) and complements the dependency register in
[`Tech_Debt.md`](./Tech_Debt.md). It is **review-gated**: the phases below are intended to be
reviewed before implementation begins.

---

## Goal & scope

**In scope**

- The main settings screen: `utils/Preferences.java` (framework `android.preference`,
  `BasePreferenceActivity`), the 8 preference XMLs, and the custom preference views.
- Secondary screens: alert editing, snooze, reminders, sensor start/stop/location,
  calibration editors, data tables, event/error logs, profile/basal/insulin editors,
  backup/import/export, language editor, help/agreement/license, misc settings-like screens.
- The settings-only support libraries (`search-preference`, `colorpicker` picker dialog).

**Out of scope (owned by other phases)**

- `Home.java` + charts (Phase 2/3) — except any shared dialog/row components it will reuse.
- The navigation drawer shell (Phase 1, done).
- The Room/`Flow` data-layer pass (`Tech_Debt.md` §5 follow-up).
- The Wear module.

**Non-negotiable invariant**

- **Preference keys and types must not change.** Settings are user data; keys
  (`"highValue"`, `"dex_collection_method"`, …) are read app-wide via `Pref`/
  `PreferenceManager`. Compose settings must read/write the *same* keys, so backups and
  existing installs keep working. New code uses `Pref` (or `PreferenceManager`) exactly as
  today; only the UI is replaced.

---

## Current state (facts)

| Item | Value |
| --- | --- |
| Main settings class | `utils/Preferences.java` — **3,223 LOC**, framework `android.preference.*` |
| Base class | `BasePreferenceActivity extends android.preference.PreferenceActivity` (deprecated) |
| Preference XMLs | **8** files, **4,448 lines**, **85** nested `<PreferenceScreen>` sub-screens, **~496** leaf preferences |
| Custom preference views | `utils/TimePreference`, `utils/ExampleChartPreferenceView`, `PrefsView*` (binding bridges) |
| Settings-only libs | `search-preference` AAR, `colorpicker` AAR (`ColorPreferenceDialog`) |
| `androidx.preference` | `1.2.1` (used for `PreferenceManager` reads; the settings **UI** is still framework) |
| Layouts | 99 total; **26** Data-Binding layouts; **18** activities use generated `*Binding` |
| Secondary activities/fragments | ~50 |
| Data layer | Room (done) — settings reads mostly go through `Pref` |

The settings UI is effectively **one very large screen**: `AllPrefsFragment` inflates all
XMLs, then programmatically removes/relabels/hides sub-screens based on prefs and hardware.

---

## Strategy

Keep the **incremental hybrid** approach:

1. **Add a Compose settings entry point alongside the legacy one.** Host the new Compose
   settings in the existing activity/navigation first (via `ComposeView` inside
   `BasePreferenceActivity`, or a new Compose host activity), so the drawer/`Preferences`
   entry keeps working. Migrate category-by-category.
2. **Build a small settings component library once**, then port screens onto it.
3. **Screen-by-screen feature parity**, verified against the legacy screen, before deleting
   the legacy layout/XML.
4. **Do not change pref keys** (see invariant).

Two sub-tracks run independently:

- **Track S — settings** (Phase 4).
- **Track V — secondary views** (Phase 5, long tail).

---

## Shared Compose settings framework (build once)

New package `ui/settings/` (Kotlin), using Phase 1 `XdripTheme` + `LocalXdripColors`.

**Components**

| Component | Replaces |
| --- | --- |
| `SettingsScaffold` (top bar, back, section list) | `PreferenceActivity` chrome |
| `SettingsCategory` (grouped section header) | `<PreferenceCategory>` |
| `SettingsSubScreenRow` (navigates to child) | nested `<PreferenceScreen>` |
| `SwitchRow` | `SwitchPreference` / `CheckBoxPreference` |
| `EditTextRow` (with summary + validation) | `EditTextPreference` |
| `ListRow` (single/multi choice) | `ListPreference` |
| `SliderRow` / `NumberRow` | `SeekBarPreference` (where used) |
| `ColorRow` (opens the Compose color picker) | `ColorPreferenceDialog` + `colorpicker` |
| `RingtoneRow` (sound picker) | `RingtonePreference` |
| `TimeRow` | `TimePreference` |
| `InfoRow` / `ActionRow` (static text / click action) | plain `Preference` with click listener |
| `SettingsSearch` | `search-preference` AAR |

**State layer**

A `SettingsViewModel` (or a small `Pref`-backed helper) exposing each preference as a
`StateFlow` and writing via `Pref`/`PreferenceManager`. This replaces the `ObservableMap` +
`@={...}` two-way Data Binding used by `PrefsView*` and the `bindPreferenceSummary*` helpers.
Conditional visibility (hardware/collection-type dependent screens) becomes ordinary Compose
`if`/state.

**Navigation**

Reuse `navigation-compose`. Settings is a graph: root categories → sub-screens. The drawer's
"Settings" entry points at the graph's start destination. Deep links (`jumpTo`) currently used
by `AllPrefsFragment` are expressed as graph routes/arguments.

**Dialogs**

Small shared composables for common dialogs (text entry, number entry, confirm, PIN, tree
selector) — see the existing `dialog_*.xml` layouts (`dialog_text_entry`, `dialog_pin_entry`,
`dialog_single_text_field`, `dialog_checkbox`, `dialog_tree_selector`, …).

---

## Track S — settings migration

### Sequencing

1. **S0 — foundation.** `ui/settings/` package + components + `SettingsViewModel` + nav graph
   + the Compose color picker + settings search. Host it from the existing entry point with a
   single **pilot category** visible and the rest still legacy. **Done (components + host,
   hand-rolled screen stack; color picker/search still to come).**
2. **S1 — pilot: `pref_general`** (31 lines, 2 screens) + `pref_license`. Smallest, proves the
   pattern end-to-end (rows, summaries, sound picker, validation). **Done** (`General` category:
   units + high/low + license row; Compose tests in `SettingsActivityTest`).
3. **S2 — `pref_notifications`** (412 lines, 12 screens) — dialogs, sounds, unitized numeric
   summaries.
4. **S3 — `pref_data_source`** (447 lines, 7 screens) — collection-method dependent visibility.
5. **S4 — `pref_data_sync`** (558 lines, 16 screens) — QR/barcode flows, cloud creds, test
   buttons.
6. **S5 — `pref_advanced_settings`** (1,633 lines, 24 screens) + `xdrip_plus_prefs`
   (1,353 lines, 23 screens) — the bulk; migrate sub-screen by sub-screen.
7. **S6 — retire legacy settings.** Delete `AllPrefsFragment`/`Preferences.java` framework code,
   `BasePreferenceActivity`, `TimePreference`, `ExampleChartPreferenceView`, the pref XMLs,
   the `search-preference` and `colorpicker` dependencies, and the `PrefsView*` settings usage.

### Custom pieces to replace

- `TimePreference` → `TimeRow` (24-hour time; used in 2 places).
- `ExampleChartPreferenceView` → a Compose preview row (used by color/chart settings).
- `ColorPreferenceDialog` + `com.rarepebble.colorpicker` → a Compose color picker
  (the `colorpicker` AAR is also used by `NumberWallPreview`, which Track V migrates).
- `search-preference` (only referenced by `Preferences.java`) → `SettingsSearch`.
- `bindPreferenceSummaryToValue*` helpers → `SettingsViewModel` state.
- Conditional `removePreference`/visibility logic → Compose state.

---

## Track V — secondary views

Group by **shape** so the same components are reused; migrate in order of similarity.

| Group | Screens (examples) | Notes |
| --- | --- | --- |
| **A. Alert/notification editors** | `EditAlertActivity`, `AlertList`, `SnoozeActivity`, `MissedReadingActivity`, `Reminders` | Form-heavy; reuse `SwitchRow`/`ListRow`/`TimeRow`. Tests exist (parity baseline). |
| **B. Sensor/calibration flows** | `StartNewSensor`, `StopSensor`, `NewSensorLocation`, `CalibrationOverride`, `DoubleCalibrationActivity`, `AddCalibration`, `CalibrationCheckInActivity` | Stepper/wizard-ish; mostly simple forms + confirmations. |
| **C. Profile/insulin/basal editors** | `ProfileEditor`, `BasalProfileEditor`, `insulin/InsulinProfileEditor` (+ `DatePickerFragment`/`TimePickerFragment`) | Custom drag/list editing; reuse `AndroidView` for the basal drag editor if needed. |
| **D. Data/table & log viewers** | `BgReadingTable`, `CalibrationDataTable`, `SensorDataTable`, `EventLogActivity`, `ErrorsActivity`, `stats/StatsActivity` (+ fragments), `SystemStatusFragment`, `MegaStatus`, `BGHistory`, `CalibrationGraph`, `LibreTrendGraph` | Lists/tables + charts; the chart ones wrap `hellocharts` via `AndroidView` until Phase 3. |
| **E. Backup/import/export & admin** | `BackupBaseActivity`/`BackupActivity`, `ImportDatabaseActivity`, `SdcardImportExport`, `DisplayQRCode`, `AndroidBarcode`, `SaveLogs`, `SendFeedBack`, `DatabaseAdmin`, `MtpConfigureActivity` | Mixed; some are debug/admin. |
| **F. Info/legal/help** | `HelpActivity`, `Agreement`, `LicenseAgreementActivity`, `HealthPrivacy`, `UsbConnectedActivity`, `UpdateActivity` | Mostly static text; quick wins. |
| **G. Misc settings-like** | `XDripDreamSettingsActivity`, `NumberWallPreview`, `TimePickerPrefActivity`, `SelectAudioDevice`, `FakeNumbers`, `ShareTest`, `WearVoiceActivity`, `InstallPebbleWatchFace`, `ThinJamActivity`, `DepositActivity`, `GluProActivity`, `NocturneConnectActivity`, `tidepool/AuthFlowIn`, `languageeditor/LanguageEditor` | Self-contained; several are small. |

Suggested Track V order: **F** (quick wins, no data) → **A/B** (forms, reuse settings
components) → **G** (misc) → **D** (tables/logs; heavy, chart-wrapping) → **C** (editors,
highest UI complexity) → **E** (admin/backup).

The `PrefsView*` binding bridge can be retired once the screens still using it
(`DoubleCalibrationActivity`, `Home`, `BackupActivity`, `DepositActivity`,
`EmergencyAssistActivity`, `NumberWallPreview`, `XDripDreamSettingsActivity`,
`DisplayQRCode`) are migrated — i.e. partly Track V, partly Home (Phase 2).

---

## What needs updating (concrete)

**New code**

- `ui/settings/` — components, `SettingsViewModel`, nav graph, search, color picker.
- Per-screen Compose files under `ui/settings/…` and `ui/<feature>/…`.

**Deleted / retired (at the end of Track S)**

- `utils/Preferences.java` (framework code), `BasePreferenceActivity.java`
- `utils/TimePreference.java`, `utils/ExampleChartPreferenceView.java`
- `res/xml/pref_*.xml`, `res/xml/xdrip_plus_prefs.xml`
- `cloud/backup/…` settings bits, `SdcardImportExport` preference glue
- Dependencies: `search-preference` AAR, `colorpicker` AAR (if no remaining users)

**Kept**

- `Pref` / `PreferenceManager` reads (keys unchanged), `PreferencesNames`, `ColorCache`, the
  `LocalXdripColors` bridge, `androidx.preference` (until no readers remain).

**Tests to update/add**

- Update `EditAlertActivityPreferencesTest`, `ErrorsActivityPreferencesTest`,
  `SnoozeActivityPreferencesTest`, `AlertListTest`, `MissedReadingActivityTest` as their
  screens move (they use `androidx.preference` reads; keys stay, so most should still pass).
- Add preference **round-trip parity** tests (set via Compose path → read via `Pref`).
- Add Compose UI/state tests per migrated screen (state → semantics), as done for the drawer.
- Keep `PrefsViewImplTest` until `PrefsView*` is retired.

**Docs**

- `Compose_Migration.md`: flip Phase 4/5 from "Planned" to the phases above; link this doc.
- `Tech_Debt.md`: track `search-preference` and `colorpicker` removal, and the settings
  framework entry.

---

## Risks & mitigations

| Risk | Mitigation |
| --- | --- |
| **Preference-key drift** breaks settings/backups | Enforce the invariant; round-trip parity tests. |
| **Conditional/hardware-dependent screens** (collection type, watch type, AndroidAPS, etc.) were expressed as runtime `removePreference` | Model as Compose state; port each condition explicitly and test both branches. |
| **Custom dialogs/summaries** (unitized numeric values, sounds, QR) | Reuse the shared dialog/row components; port the formatting helpers, don't reinvent. |
| **Scope** (85 sub-screens, ~50 activities) | Strict screen-by-screen with the legacy screen deleted only after parity; pilot first. |
| **`colorpicker`/`search-preference` AAR removal** while other screens still use them | Remove only after the last user migrates; track in `Tech_Debt.md`. |
| **Data Binding interop** during the hybrid period | Follow the established `AndroidView`/delegate bridge pattern. |

---

## Testing strategy

- **State/parity unit tests** (Robolectric, no screenshots): for each migrated screen, assert
  the Compose state derives the right values and that writes hit the correct pref keys.
- **Preference round-trip tests**: set through the new path, read via `Pref`; and vice versa.
- **Manual parity checks** on the emulator against the legacy screen (values, summaries,
  conditional visibility, dialogs, sounds).
- Existing behaviour tests (alert/snooze/missed/errors) must stay green.

---

## Definition of done

- Every settings category and sub-screen and every secondary view is Compose (or
  `AndroidView`-wrapped legacy where a dependency is intentionally retained, e.g. the basal
  drag editor / charts).
- No `android.preference` UI classes remain; `Preferences.java`/`BasePreferenceActivity` and
  the preference XMLs are deleted.
- `search-preference` and (if unused) `colorpicker` dependencies removed.
- Pref keys unchanged; backups and existing installs remain compatible.
- Full unit suite + `assembleFastDebug` (R8) green; manual parity checked on the emulator.
