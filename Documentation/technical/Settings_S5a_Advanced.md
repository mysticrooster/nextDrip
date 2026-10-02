# S5a — Advanced Settings → Compose (plan & progress)

Working plan for porting `pref_advanced_settings.xml` (273 elements, 2 top-level branches) into the
Compose settings host. Annex to [`Settings_Migration.md`](./Settings_Migration.md), where the recipe,
verification passes and higher-level status board live.

**Invariant:** pref keys and types never change. **This doc is a living document** — update the
section status table and progress log as work lands.

---

## 1. Inventory

`pref_advanced_settings.xml` has two top-level entry points (both shown on the legacy root):

| Branch | key | title | Nodes | Notes |
| --- | --- | --- | --- | --- |
| Smart watch features | `smart_watch_options` | `@string/smart_watch_features` | 148 | Wear, Pebble, Amazfit, BlueJay, LeFun, MiBand, Smartwatch sensors |
| Other settings | `other_category` | `@string/other_settings` | 124 | TTS, inter-app, extra status line, calibration, Bluetooth, BlueReader, Libre2, logging, misc |

### Other settings (`other_category`)

| Section | key | type | ~leaves |
| --- | --- | --- | --- |
| Speak readings | `xdrip_speak_readings_settings` | Screen | 11 |
| Inter-app settings | `xdrip_intrer_app_settings` | Screen | 16 (+ Health Connect sub-screen) |
| Less common settings | `xdrip_less_common_settings` | Screen | groups the below |
| · Extra status line | `xdrip_extra_status_line` | Screen | 23 |
| · Advanced calibration | `xdrip_plus_calibration_settings` | Screen | 12 |
| · Bluetooth settings | `xdrip_bluetooth_adv_settings` | Screen | 14 |
| · BlueReader advanced | `xdrip_blueReader_advanced_settings` | Screen | 5 |
| · Libre2 advanced | `xdrip_libre2_advanced_settings` | Screen | 2 |
| · Extra logging | `xdrip_logging_adv_settings` | Screen | 4 |
| · Other misc options | `xdrip_other_misc_extra_screen` | Screen | 14 (+ collector-in-foreground sub-screen) |
| (top-level leaves) | `aggressive_service_restart`, `interpret_raw`, `show_data_tables`, `display_bridge_battery`, `disable_battery_warning`, `save_db_ondemand`, `retention_days_bg_reading` | — | 7 |

### Smart watch features (`smart_watch_options`)

| Section | key | ~leaves |
| --- | --- | --- |
| Android Wear | `android_wear_preferences` | 12 |
| Pebble | `pebble_preferences` | 18 |
| Amazfit | `amazfit_sync_preferences` | 9 |
| BlueJay | `bluejay_preference_screen` (+ advanced) | 23 |
| LeFun | `lefun_preferences` (+ features) | 13 |
| MiBand | `miband_preferences` (+ settings/nightmode/screens) | 45 |
| Smartwatch sensors | `smartwatch_sensors_screen` | 3 |

---

## 2. Approach

- **Screens** live in new `ui/settings/AdvancedScreens.kt` (+ `WatchScreens.kt`) using the existing
  row helpers (`SwitchPref`/`EditPref`/`ListPref`/`RingtonePref`/`SettingsSliderRow`) and
  `SettingsState` for reactive reads/writes.
- **Gating** goes through `SettingsVisibility` / `Pref` conditions (`engineering_mode`,
  `DexCollectionType`, hardware name, `DexCollectionService.getBestLimitterHardwareName()`), reusing
  the logic read from `Preferences.java`.
- **Side effects** are attached via the `onCheckedChange`/`onValueChange`/`onSelected` hooks.
- **Navigation**: add `SettingsScreen` destinations and wire into `titleFor` /
  `SettingsScreenContent`; the legacy root gets "Smart watch features" and "Other settings" entries
  (ordered as in the legacy XML).
- **Dependencies** (`android:dependency`, `disableDependentsState`) via
  `SettingsState.dependentEnabled`.
- New **value rows** only if needed (e.g. TTS sliders already covered by `SettingsSliderRow`).

---

## 3. Section order & status

Low-risk/self-contained first, watches last (hardware + dynamic `removePreference` heavy).

| # | Section | Status |
| --- | --- | --- |
| 1 | Other misc options + collector-in-foreground | **Done** |
| 2 | Extra logging | **Done** |
| 3 | Advanced calibration | **Done** |
| 4 | Bluetooth settings | **Done** |
| 5 | BlueReader advanced (hardware-gated) | **Done** |
| 6 | Libre2 advanced (hardware-gated) | **Done** |
| 7 | Other settings top-level leaves | **Done** |
| 8 | Extra status line | **Done** |
| 9 | Speak readings (TTS) | **Done** |
| 10 | Inter-app settings (+ Health Connect) | **Done** |
| 11 | Smartwatch sensors | **Done** |
| 12 | Android Wear | **Done** |
| 13 | Pebble | **Done** |
| 14 | Amazfit | **Done** |
| 15 | BlueJay (+ advanced) | **Done** |
| 16 | LeFun (+ features) | **Done** |
| 17 | MiBand (+ settings/nightmode/screens) | **Done** |

Sections 1–10 land in `ui/settings/AdvancedScreens.kt`; the "Other Settings" root category
links to Speak readings / Inter-app / Less common settings (inline, as in the legacy XML).

### Known deviations / follow-ups (sections 1–10)

- The legacy code moves `interpret_raw` and `predictive_bg` between the notifications and
  advanced screens depending on `DexcomShare` (a cross-screen `addPreference`/`removePreference`
  quirk). Both are currently shown in Less common settings / Other misc as declared in the XML.
  Revisit when the notifications/advanced parity pass runs.
- `requested_ignore_battery_optimizations_new` is treated as a plain switch (no listener was found
  in the fragment); verify on-device whether it should trigger the system battery-optimisation
  prompt.
- `allow_testing_with_dead_sensor` has a self-`dependency` in the XML (legacy quirk) and is left
  ungated.


---

## 4. Gating & side-effects inventory (to reproduce)

From `Preferences.java`:

- `engineering_mode` gates: `old_school_calibration_mode`, `nfc_test_diagnostic` (S3),
  `bugfender_appid` (only when `enable_bugfender`), MiBand screens/experimental, `share_test_key`,
  web deposit, `dex_specified_slot`.
- `external_blukon_algorithm` ⇄ `retrieve_blukon_history` enabled inversion.
- `current_calibration_plugin` → `PluggableCalibration.invalidate*`.
- `g5-battery-warning-level` → `G5BaseService.resetTransmitterBatteryStatus()` +
  `SdcardImportExport.hardReset()` (done in S3 G5 debug).
- `enable_bugfender` gating of `bugfender_appid`.
- `requested_ignore_battery_optimizations_new` → battery-optimisation prompt.
- `health_connect_enable` → `HealthGamut.init`; `health_connect_manage` → open permission manager.
- Watch sections: `MiBandEntry`/`LeFunEntry`/`BlueJayEntry` pref listeners, dynamic removal of
  `miband2_screen`/`miband3_4_screen`/`miband_graph_category`/`debug_miband4` for model/engineering,
  `update_miband_bg` click, Pebble installer intents, BlueJay "Launch BlueJay Panel".
- `interpret_raw` / `predictive_bg` moved/removed per `DexcomShare` (see data-source block).

> Full `removePreference`/`addPreference` matrix lives in `Preferences.java` ~1334–2200; each
> section implements only the conditions that touch its keys.

---

## 5. Verification mapping

Per section, run the passes from
[`Settings_Migration.md`](./Settings_Migration.md):

- **A** structure/navigation, **B** key/type round-trip, **D** conditional visibility,
  **E** side effects, **F** summaries/validation — targeted per screen.
- **K** global: full unit suite + `assembleFastDebug` each pass.

---

## 6. Progress log

- Plan created; inventory captured. Started section 1 (Other misc options).
- **Pass 1 (sections 1–10) landed** in `ui/settings/AdvancedScreens.kt`:
  - Other misc + collector-in-foreground, extra logging (`enable_bugfender` gates app id),
    advanced calibration (plugin list via `PluggableCalibration` + cache invalidation),
    Bluetooth, BlueReader (hardware-gated), Libre2 (`LibreReceiver`-gated), the Less-common
    top-level leaves, extra status line, Speak readings (TTS sliders) and Inter-app
    (+ Health Connect).
  - `SettingsVisibility.isEngineeringMode(state)` now reads through `SettingsState` so
    engineering-gated rows show/hide live; added `isBlueReader()` / `isLibreReceiver()`.
- **Host fix:** `SettingsRoot` now resets the scroll position on navigation (previously the shared
  `rememberScrollState` kept the previous screen's offset, so submenus opened scrolled and rows
  could be off-screen).
- Tests: added advanced extra-status-line toggle + calibration-plugin presence; 953 unit tests
  green; `assembleFastDebug` green.
- **Pass 2 (sections 11–17, watches) landed** in `ui/settings/WatchScreens.kt`:
  - Smartwatch sensors, Android Wear, Amazfit, LeFun (+ features), BlueJay (+ advanced),
    MiBand (+ sub-settings), Pebble. Root now has a **Smart Watch Features** category.
  - Gating: `SettingsVisibility.pebbleSyncType(state)` (reactive Pebble type) and
    `SettingsVisibility.mibandType()` (hardware model); engineering-gated MiBand debug section.
  - Side effects reproduced: Wear `WatchUpdaterService.startSelf`; Amazfit service start/stop;
    LeFun/BlueJay/MiBand call-permission prompts; MiBand enable location/read-permission and
    `update_miband_bg`; Pebble service/watchface install via the shared
    `utilitymodels.pebble.PebbleActions` (the legacy fragment now delegates to it too); BlueJay
    mutual-exclusion guards via shared `BlueJayAdapter.canUsePhoneSlot`/`canRunPhoneCollector`
    (used by both the legacy listeners and Compose). The legacy MiBand/LeFun/BlueJay
    `OnSharedPreferenceChangeListener`s are registered for the Compose host lifetime
    (`SettingsActivity`), since the legacy activity is not open.
  - New component/helper support: `SettingsInfoRow` (read-only/selectable), `maxLength` on edit
    rows, switch **veto** (`onBeforeChange`), and `onSelected` on `ListPref`.
  - Tests: new `WatchSettingsTest` (14 tests — navigation, round-trips, Wear dependency chain,
    Pebble type visibility, MiBand model gating, engineering debug, BlueJay veto, Amazfit).
    Full unit suite + `assembleFastDebug` (R8) green.
  - **Known deviations:** Pebble visibility uses a pure `syncType` mapping (initial-inflation
    behaviour) because the legacy `removePreference` matrix differs between inflation and
    on-change; the MiBand debug section is placed in the sub-settings screen (matching where the
    legacy `removePreference` looked for it) and gated on engineering mode.
- **S5a complete.** S5b (`xdrip_plus_prefs.xml`) and S6 (legacy settings retirement) have since
  landed — see `Settings_Migration.md` §3 — so no outbound work remains from this annex.
