# Settings conversion — change list & verification passes

This document is the **working checklist** for porting the remaining legacy settings
(`android.preference` XML + `utils/Preferences.java`) to the Compose host, and the
**verification passes** used to prove each port is faithful. It complements the plan in
[`Settings_And_Secondary_Views_Compose.md`](./Settings_And_Secondary_Views_Compose.md) and the
strategy in [`Compose_Migration.md`](./Compose_Migration.md).

---

## 1. Scope

Everything the legacy settings screen does, split into the plan's tracks:

| Track | Category | Source | Sub-screens | ~Leaves | Status |
| --- | --- | --- | --- | --- | --- |
| S1 | General + License | `pref_general.xml`, `pref_license.xml` | 3 | 4 | **Done** |
| S2 | Alarms & Alerts | `pref_notifications.xml` | 12 | 54 | **Done** |
| S3 | Data Source | `pref_data_source.xml` | 7 | 59 | **Done** |
| S4 | Data Sync | `pref_data_sync.xml` | 16 | 67 | **Done** |
| S5a | Advanced Settings | `pref_advanced_settings.xml` | 24 | 232 | Todo |
| S5b | xDrip+ Options | `xdrip_plus_prefs.xml` | 23 | 157 | Todo |
| S6 | Retire legacy settings | `Preferences.java`, XMLs, libs | — | — | Todo |
| — | Theme editor (new) | — | 1 | 36 roles + 34 data | **Done** |

Already in Compose: **General** (units, high/low, license), **Alarms & Alerts** (+ all sub-screens
incl. suppress/channels/ascending/persistent-high/forecast-low/sensor-expiry/calibration/other),
**Data Source** (+ web-follower/NFC/NS-follow/G5-debug sub-screens), **Data Sync** (+ REST/Mongo/
Influx/dexcom-share/Tidepool/web-deposit/NightLite/Nocturne/meters), **Theme editor**, and a
**root search** over migrated destinations. Everything else still opens the legacy activity via
"Classic settings".

> All work preserves the **non-negotiable invariant**: pref **keys and types never change**
> (read app-wide via `Pref`), so existing installs and backups keep working.

---

## 2. What needs to change

### 2.1 Cross-cutting changes (shared building blocks)

These are needed by more than one category and should be done once, early in S3.

1. **Visibility model.** Replace the legacy `removePreference(...)` calls with Compose `if`
   conditions. Most are driven by:
   - `DexCollectionType` — `getType(dex_collection_method)`, `hasLibre(...)`, `hasWifi()`,
     `getBestCollectorHardwareName()`, `getDexCollectionType()`.
   - `engineering_mode`, `Build.VERSION.SDK_INT`, `Home.get_master()`,
     `Experience.gotData()`, `WholeHouse.isRpi()`, `enable_bugfender`.
   Implement a `SettingsVisibility` helper (reads through `SettingsState` so toggling a master
   recomposes) rather than ad-hoc `Pref` reads.
2. **Side effects on change.** The legacy screen attaches ~217 `findPreference(...)` listeners.
   The important ones must be reproduced as `onValueChange`/`onSelected` callbacks:
   service restarts (`CollectionServiceStarter.restartCollectionServiceBackground()`),
   follower resets (`ShareFollowService`, `NightscoutFollow`, `TidepoolUploader`,
   `PluggableCalibration.invalidate*`), `NFCReaderX.handleHomeScreenScanPreference`,
   alert clearing (`UserNotification.lastCalibrationAlert().delete()`), chart/widget refresh
   (`Home.staticRefreshBGCharts`, `WidgetUpdateService.staticRefreshWidgets`,
   `Notifications.staticUpdateNotification`), and session resets on credential edits.
3. **Master→dependent changes.** `android:dependency`/`disableDependentsState` → `SettingsState.dependentEnabled(...)`
   (already built). Sweep for `summaryOn`/`summaryOff` (switch state text) — currently not rendered.
4. **Summaries & validation.** Port the remaining `bindPreference*` behaviours:
   value-as-summary (`sBindPreferenceSummaryToValue`), numeric-only, unitised numeric with
   range rejection (`SettingsPrefs.isValidGlucoseInput` + toast), title-append
   (`update_channel`, MiBand MAC), and the two-way `secondsSince(1970)` conversion for
   `ProfileEditor`/`Tidepool`.
5. **Custom widgets.** Build the remaining Compose equivalents:
   | Legacy | Used by | Compose |
   | --- | --- | --- |
   | `TimePreference` | `pref_advanced_settings` (×2) | `SettingsTimeRow` (**done**) |
   | `ExampleChartPreferenceView` | `xdrip_plus_prefs` (×1) | chart-preview row (todo) |
   | `ColorPicker` (colorpicker AAR) | 34 colours | `SettingsColorRow` + theme editor (**done**) |
   | `RingtonePreference` | notifications, others | `SettingsRingtoneRow` (**done**) |
   | Multi-select / tree selector / PIN dialogs | advanced, data sync | new shared composables (todo) |
6. **Deep links.** Legacy `Preferences.jumpToScreen(key)` (used by widgets/shortcuts) → a
   `SettingsScreen` route argument on `SettingsActivity` (todo).
7. **Search scope.** Extend `SETTINGS_SEARCH_INDEX` to cover leaf preferences (not just
   sub-screens) so migrated search matches the legacy `search-preference` breadth.
8. **Global pref-change listeners.** The legacy activity registers service/watch/collector
   listeners while open (see `Preferences.onResume`). The Compose host must register the same
   set before S4, or pref changes made in Compose won't trigger the live reactions.
9. **Icons.** Legacy sub-screens carry `android:icon`; the Compose index doesn't render them.
   Add leading icons to `SettingsActionRow` for parity.

### 2.2 S3 — Data Source (`pref_data_source.xml`, 7 sub-screens)

Change list:
- Category **Data Source Settings**: `dex_collection_method` (List; entries
  `DexCollectionMethods` / values `DexCollectionMethodValues`) **with its change side effects**
  (Follower resets `bridge_battery`/`parakeet_battery`, turns off `plus_follow_master`,
  `GcmActivity.requestBGsync()`; `DexcomShare` forces `calibration_notifications=false`;
  always `restartCollectionServiceBackground()`).
- Sub-screen **Web Follower Settings** (`xdrip_plus_web_follow_settings`): visible only for
  `WebFollow`; prefs `webfollow_master_domain`, `webfollow_username`, `webfollow_password`,
  `webfollow_use_proxy` (+ dependents `webfollow_proxy_*`, `webfollow_proxy_type_http` with
  `summaryOn`/`summaryOff`).
- Sub-screen **NFC Scan Features** (`xdrip_plus_nfc_settings`): visible only when
  `hasLibre(collectionType)`; `use_nfc_scan` (with `NFCReaderX` side effect), 
  `libre2_enable_bluetooth_streaming`, `libre_filter_length`, `nfc_show_age`,
  `nfc_expiry_days`, `nfc_scan_homescreen`, `nfc_scan_vibrate`, `nfc_scan_beep`
  (engineering only), `use_nfc_multiblock`, `use_nfc_any_tag`, `nfc_test_diagnostic`
  (engineering only).
- `share_key` + action `scan_share2_barcode`: only `DexcomShare`; clicking `share_key`
  clears `dexcom_share_session_id`.
- `dex_txid`: only `DexbridgeWixel`, `WifiDexBridgeWixel`, `DexcomG5`.
- `medtrum_use_native`, `medtrum_a_hex`: only `Medtrum`.
- NSFollow group: `nsfollow_url`, sub-screen `nsfollow_download_treatments_screen`
  (+ `nsfollow_download_treatments`, dependent `cloud_storage_api_skip_download_from_xdrip`),
  `nsfollow_sample_period_in_minutes`, `nsfollow_lag`; `nsfollow_url` change resets
  `NightscoutFollow.resetInstance()`.
- SHFollow group: `shfollow_user`, `shfollow_pass`, `dex_share_us_acct` (change resets
  `ShareFollowService` + restarts collector).
- `follower_chime`: only `Follower`.
- CLFollow group (only `CLFollow`): `clfollow_country`, `clfollow_patient`,
  `clfollow_login` (action), `clfollow_grace_period`, `clfollow_missed_poll_interval`,
  `clfollow_download_finger_bgs/boluses/meals/notifications`.
- Sub-screen **G5/G6/Dex1 Debug Settings** (`xdrip_plus_g5_extra_settings`): only
  `DexcomG5`; categories **ob1_options** (`ob1_g5_use_transmitter_alg` + dependents,
  `ob1_g5_restart_sensor`, sub-screen `collection_preemptive_restart` + its children,
  `ob1_g5_use_insufficiently_calibrated`, `ob1_minimize_scanning` + `ob1_avoid_scanning`,
  `ob1_g5_allow_resetbond`, `ob1_special_pairing_workaround`, `dex_specified_slot`
  [engineering]) and **dex_battery_category** (`g5-battery-warning-level`, hidden for G7/One+).
- `wifi_recievers_addresses`: visible when `hasWifi()` **or** the value is already non-blank.

### 2.3 S4 — Data Sync (`pref_data_sync.xml`, 16 sub-screens)

Todo. Expected work: cloud storage (MongoDB/REST/Web deposit), xDrip+ Sync, Nightscout,
Dexcom share upload screens (`dexcom_server_upload_screen`, with `share_test_key`/`share_key`
hidden), Tidepool (login/creds with listeners), QR/barcode flows, data tables, plus
engineering/master gates (`cloud_storage_web_deposit`, `desert_sync_master_ip`).

### 2.4 S5a — Advanced Settings (`pref_advanced_settings.xml`, 24 screens, ~232 leaves)

Todo. Contains the bulk: display/colours, calibration (`current_calibration_plugin`,
`old_school_calibration_mode`), alerts list, sensors, motion (`motionScreen` gated by
`Experience.gotData()`), update channel (title-append), language, logging
(`enable_bugfender`), `BlueReader` hardware gate, NFC expiry listeners, Pebble/MiBand/watch
sections (many `removePreference` + dynamic `addPreference`), etc.

### 2.5 S5b — xDrip+ Options (`xdrip_plus_prefs.xml`, 23 screens, ~157 leaves)

Todo. Includes the 34 `ColorPicker` colours (**now served by the Theme editor**),
`ExampleChartPreferenceView`, number-wall preview, and many display/graph options.

### 2.6 S6 — Retire legacy settings

Only after S1–S5 reach parity:
- Delete `utils/Preferences.java` framework code, `BasePreferenceActivity`,
  `utils/TimePreference`, `utils/ExampleChartPreferenceView`, the `pref_*.xml` /
  `xdrip_plus_prefs.xml` files.
- Remove the `search-preference` and (if unused) `colorpicker` dependencies.
- Retire the `PrefsView*` settings usage once its non-settings users are migrated too.
- Point every external entry point (drawer already done, widgets, shortcuts, deep links) at
  the Compose host.

---

## 3. Verification passes

Run these as separate passes; each is independent and can be repeated per category.
Status legend: **Ready** = do it now for migrated screens; **Pending** = blocked on S3/S4/S5.

### Pass A — Structure & navigation parity · *Ready (migrated categories)*
- **Goal:** every legacy category/sub-screen exists in Compose with the same title/order/breadcrumb.
- **Check:** walk each XML and compare against `SettingsNavigation.kt` + `SettingsScreens.kt`;
  assert Back stack matches nested `PreferenceScreen` nesting.
- **How:** manual side-by-side on emulator + a unit test per category asserting the expected
  destinations are reachable (as `SettingsActivityTest` does for notifications).
- **Exit:** no migrated screen missing/renamed; search index covers every migrated destination.

### Pass B — Preference key & type parity · *Ready*
- **Goal:** Compose writes the exact key *and* type the legacy code reads.
- **Check:** for every migrated row, confirm key string + value type (bool/string/int/long)
  against the XML `android:key`/`android:defaultValue` and `Pref.get*` call.
- **How:** round-trip tests (`set via Compose → read via Pref`, and vice-versa), plus a grep
  audit that no key string changed. See `SettingsActivityTest`, `ThemeColorTest`.
- **Exit:** round-trip green; no orphaned/renamed keys.

### Pass C — Defaults parity · *Ready*
- **Goal:** an untouched install behaves as before.
- **Check:** compare defaults (XML `android:defaultValue`, `SettingsState.string(key, default)`,
  `ThemeColor` Material defaults, `legacyColorDefaults`) and the one-time migration.
- **How:** unit tests asserting the default resolved when the key is absent; migration test
  (`ThemeColorTest.migrationCopiesNonDefaultLegacyColorsOnly`).
- **Exit:** defaults match, migration preserves prior customisations.

### Pass D — Conditional visibility & gating · *Pending (S3+)*
- **Goal:** categories/prefs appear exactly as before for each `DexCollectionType`, engineering
  mode, SDK level, `Home.get_master()`, `Experience.gotData()`, hardware (BlueReader/G7/One+).
- **Check:** the `removePreference`/`addPreference` matrix in `Preferences.java` (§2.1 note) is
  reproduced with `if` conditions via a `SettingsVisibility` helper.
- **How:** parameterised unit tests over the enum values
  (`for type in DexCollectionType.entries → assert visible set`); manual spot-checks.
- **Exit:** visible set identical for every collection type and gate.

### Pass E — Side-effect / behaviour parity · *Pending (S3+)*
- **Goal:** changing a setting triggers the same action as legacy.
- **Check:** the ~217 listeners; at minimum service restarts, follower/session resets, NFC
  handler, alert deletion, chart/widget refresh, session-id clearing.
- **How:** unit-test the callbacks with mocks/Robolectric where feasible; otherwise manual
  on-device checks (e.g., switch collection method → collector restarts).
- **Exit:** documented list of side effects, each verified once.

### Pass F — Summaries, formatting & validation · *Pending (partly Ready)*
- **Goal:** row supporting text and validation match legacy (`bindPreference*`).
- **Check:** value-as-summary, numeric-only, unitised glucose range (+toast), title-append,
  `summaryOn`/`summaryOff` switches, `secondsSince(1970)` conversions.
- **How:** unit tests for formatters/validators; manual checks for toasts/titles.
- **Exit:** each summary/validation has a test or a manual sign-off.

### Pass G — Custom widgets · *Partly Ready*
- **Goal:** colour/sound/time/chart-preview/dialogs match legacy.
- **Check:** `SettingsColorRow`, `SettingsRingtoneRow`, `SettingsTimeRow` (done);
  `ExampleChartPreferenceView`, multi-select, tree-selector, PIN dialogs (todo).
- **How:** component tests (`SettingsComponentsTest`) + manual parity.
- **Exit:** every custom widget has a Compose equivalent and a test.

### Pass H — Search parity · *Pending*
- **Goal:** search finds the same settings as `search-preference`.
- **Check:** `SETTINGS_SEARCH_INDEX` covers leaf prefs, not just sub-screens.
- **How:** unit tests over the index; manual query spot-checks.
- **Exit:** representative legacy search terms all resolve to the right destination.

### Pass I — Theming (Material You + overrides) · *Ready*
- **Goal:** every component uses Material You defaults with user overrides winning.
- **Check:** no hardcoded colours; `ThemeColor` registry covers used colours; `xdripColor`
  resolves `override ?: Material default`.
- **How:** `ThemeColorTest`, `SettingsComponentsTest`, grep for colour literals.
- **Exit:** theme editor round-trips; no literal colours outside `theme/Color.kt`.

### Pass J — Legacy interop & retirement · *Pending (S6)*
- **Goal:** nothing reads the legacy XML/`Preferences.java` once migrated; no dangling keys.
- **Check:** delete artefacts in §2.6; confirm `PrefsView*`/`colorpicker`/`search-preference`
  have no remaining users.
- **How:** build + grep audit; delete then run full suite.
- **Exit:** legacy settings classes/XML/libs gone; suite + R8 green.

### Pass K — Global regression & safety · *Ready (run every pass)*
- **Goal:** no regression in the rest of the app.
- **Check:** full unit suite; `assembleFastDebug` (R8); backup/restore still includes all prefs;
  global pref-change listeners still fire.
- **How:** `./gradlew :app:testFastDebugUnitTest :app:assembleFastDebug`; manual backup/restore.
- **Exit:** suite green, R8 green, no pref-key drift.

---

## 4. Checklist tracking

- [x] Cross-cutting blocks (§2.1 items 1–9) — visibility model, dependency, summaries/validation,
      colour/sound/time/slider rows, side effects for migrated categories done; deep links,
      leaf-level search, chart-preview row, global listeners, icons remain.
- [x] S3 Data Source
- [x] S4 Data Sync
- [ ] S5a Advanced Settings
- [ ] S5b xDrip+ Options
- [ ] S6 Retire legacy
- [ ] Passes A–K green per migrated category
