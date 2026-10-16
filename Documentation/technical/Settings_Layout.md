# Settings Layout

This document describes the re-organised settings information architecture. The
settings are split into nine per-category XML files under
`app/src/main/res/xml/`, each with a top-level `PreferenceScreen`:

| File | Root key | Title | Description |
|---|---|---|---|
| `pref_cat_alarms.xml` | `settings_cat_alarms` | Alarms and Alerts | Glucose, calibration and sensor alerts and how they sound |
| `pref_cat_devices.xml` | `settings_cat_devices` | Devices | How xDrip collects data and the hardware it talks to |
| `pref_cat_data.xml` | `settings_cat_data` | Your Data | Uploads, sync, backups and data tables |
| `pref_cat_general.xml` | `settings_cat_general` | General | Glucose units, reminders and system notifications |
| `pref_cat_profile.xml` | `settings_cat_profile` | Profile | Insulin, carb ratios and the prediction model |
| `pref_cat_appearance.xml` | `settings_cat_appearance` | Appearance | Colours, display, graphs and the home screen |
| `pref_cat_accessibility.xml` | `settings_cat_accessibility` | Accessibility | Speak readings and always-on display |
| `pref_cat_advanced.xml` | `settings_cat_advanced` | Advanced | Bluetooth, logging, inter-app, motion and calibration |
| `pref_cat_about.xml` | `settings_cat_about` | About | Version information, updates, feedback and licence |

`AllPrefsFragment.onCreate` (`utils/Preferences.java`) adds all nine in that
order. `xdrip_plus_defaults.xml` is still loaded only for its default values.

The reorganisation moves rows between files but **does not change any
preference key, type or default**. `SettingsStructureTest` pins the baseline in
`app/src/test/resources/settings_structure_fixture.tsv`.

## Legend

- `[S]` switch · `[L]` list · `[E]` edit text · `[N]` slider (Int) · `[R]` ringtone · `[C]` colour
- `→` sub-screen · `⇒` launches an activity or action
- `en:X` enabled when X · `if X` hidden unless X
- `ENG` = `engineering_mode` · `IU` = `I_understand`

## Tree

### General (`pref_cat_general`)
- → **Glucose Units**: `units`, `highValue`, `lowValue`
- ⇒ Reminders (gated by `plus_show_reminders`) · ⇒ Emergency Messages
- → **Notifications**: `high_priority_notifications`, `public_notifications`,
  `ongoing_notification_aodchipstyle`, `compact_persistent_notification`,
  `use_proper_ongoing`

### Alarms and Alerts (`pref_cat_alarms`)
- ⇒ Glucose Level Alerts List (`AlertList`)
- → **Glucose Alerts Settings** (`bg_alert_profile`, `alert_audio_focus`, the
  boolean alert preferences, → Suppress Alerts if missed readings, → Ascending
  Volume)
- → **Persistent High Alert** · → **Forecasted Low Alert** · → **Sensor Expiry**
- → **Calibration Alerts** · ⇒ Missed reading Alert · → **Other Alerts**

### Devices (`pref_cat_devices`)
- Data source rows (`collection_category`): `dex_collection_method` and the
  per-collector rows/screens (Web Follow, NFC, G5, Dexbridge, Medtrum, NS
  Follow, Share Follow, Follower, CareLink Follow, wifi receivers)
- → **Advanced Libre options** (`libre_options_screen`, filtered per method)
- → **Glucose Meters** (`bluetooth_meters`) · → **Insulin Pens** (`pens_screen`)
- → **Smart Watch Features** (`smart_watch_options`)
- → **blueReader Settings** (shown only for BlueReader)

### Your Data (`pref_cat_data`)
- → **Cloud sync** (auto configure + cloud upload: REST-API, MongoDB,
  InfluxDB, Dex Share, Tidepool, Web Deposit, NightLite, Nocturne)
- → **Copying Settings** (`xdrip_plus_move_settings`)
- → **Backups** (`backups_screen`): cloud backup, share QR, export/import DB,
  export CSV, load/save settings
- ⇒ BG data table · ⇒ Calibration data table (if `show_data_tables`)
- → **xDrip sync** (en:IU) with → Remote Snoozing and → Desert Sync

### Profile (`pref_cat_profile`, all rows en:IU)
- → Multiple Insulin Types · ⇒ Carb Ratio · ⇒ Insulin Sensitivity ·
  ⇒ Basal Profile Editor · `profile_carb_absorption_default` ·
  `simulations_enabled` · `predict_use_momentum` · → Low level prediction values

### Appearance (`pref_cat_appearance`)
- → **Colours** (legacy colour screen) · → **Display** (→ Font + display rows)
- → **Language** · → **Graph Settings** · ⇒ Show Libre trend (if a Libre block
  exists) · → **Extra Status Line** · → **Number wall** · → **Number Icon** ·
  → **Home screen** (`home-shelf-*`)

### Accessibility (`pref_cat_accessibility`)
- → **Speak Readings** · → **Always On Display**

### Advanced (`pref_cat_advanced`)
- → **Bluetooth Settings** · ⇒ Events Log · → **Extra Logging** ·
  → **Inter-app** (+ Health Connect) · → **Other misc. options** ·
  → **Advanced Calibration** · → **Motion tracking**

### About (`pref_cat_about`)
- → **Version information** · ⇒ Check for update · → **Update settings** ·
  ⇒ Send Feedback · ⇒ Crowd-sourced translation · ⇒ EULA

## Visibility

Row visibility is applied in `AllPrefsFragment.onCreate` with the
`removeFromParent(key)` helper, which walks the live preference tree from the
root. Use it rather than a targeted `removePreference`, because a preference may
have moved to a different parent while its visibility rule did not.

## Not present

The Home overflow menu has been removed (its actions are now settings rows).
Help, Delete All BG readings, the debugging test feature and
"Send BG readings to backfill" stay unreachable, as before.
