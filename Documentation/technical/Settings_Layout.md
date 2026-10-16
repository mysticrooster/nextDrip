# Settings Layout

This document describes the re-organised settings information architecture. The
settings are split into nine per-category XML files under
`app/src/main/res/xml/`, each with a top-level `PreferenceScreen`:

| File | Root key | Title | Icon | Description |
|---|---|---|---|---|
| `pref_cat_general.xml` | `settings_cat_general` | General | `ic_ruler_grey600_48dp` | Glucose units, reminders, language and system notifications |
| `pref_cat_alarms.xml` | `settings_cat_alarms` | Alarms and Alerts | `ic_add_alert_grey600_48dp` | Glucose, calibration and sensor alerts and how they sound |
| `pref_cat_devices.xml` | `settings_cat_devices` | Data Sources and Devices | `ic_bluetooth_settings_grey600_48dp` | How xDrip collects data and the hardware it talks to |
| `pref_cat_data.xml` | `settings_cat_data` | Your Data | `ic_cloud_upload_grey600_48dp` | Uploads, sync, backups and data tables |
| `pref_cat_appearance.xml` | `settings_cat_appearance` | Appearance | `ic_palette_grey600_48dp` | Colours, display, graphs, always-on display and the home screen |
| `pref_cat_profile.xml` | `settings_cat_profile` | Profile | `ic_mode_edit_grey_600_48dp` | Insulin, carb ratios and the prediction model |
| `pref_cat_accessibility.xml` | `settings_cat_accessibility` | Accessibility | `ic_speak_reading_grey600_48dp` | Spoken readings and alerts |
| `pref_cat_advanced.xml` | `settings_cat_advanced` | Advanced | `ic_wrench_grey600_48dp` | Bluetooth, logging, inter-app, motion and calibration |
| `pref_cat_about.xml` | `settings_cat_about` | About | `ic_google_circles_communities_grey600_48dp` | Version information, updates, feedback and licence |

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
- `plus_show_reminders` (Enable Reminder features)
- ⇒ Reminders (gated by `plus_show_reminders`, not a newbie) · ⇒ Emergency Messages
- → **Notifications**: `high_priority_notifications`, `public_notifications`,
  `ongoing_notification_aodchipstyle`, `compact_persistent_notification`,
  `use_proper_ongoing`
- → **Motion tracking / Vehicle mode** (`xdrip_plus_motion_settings`, if data)
- → **Language**: `force_english`, `forced_language`

### Alarms and Alerts (`pref_cat_alarms`)
- ⇒ Glucose Level Alerts List (`AlertList`)
- → **Glucose Alerts Settings** (`bg_alert_profile`, `alert_audio_focus`, the
  boolean alert preferences, → Suppress Alerts if missed readings, → Ascending
  Volume)
- → **Persistent High Alert** · → **Forecasted Low Alert** · → **Sensor Expiry**
- → **Calibration Alerts** · ⇒ Missed reading Alert · → **Other Alerts**

### Data Sources and Devices (`pref_cat_devices`)
- Data source rows (`collection_category`): `dex_collection_method` and the
  per-collector rows/screens (Web Follow, NFC, G5, Dexbridge, Medtrum, NS
  Follow, Share Follow, Follower, CareLink Follow, wifi receivers)
- → **Advanced Libre options** (`libre_options_screen`, filtered per method)
- → **Glucose Meters** (`bluetooth_meters`) · → **Insulin Pens** (`pens_screen`)
- → **Smart Watch Features** (`smart_watch_options`)
- → **blueReader Settings** (shown only for BlueReader)

### Your Data (`pref_cat_data`)
- → **Cloud Upload** (`cloud_data_sync`, first entry): REST-API, MongoDB,
  InfluxDB, Dex Share, Tidepool, Web Deposit, NightLite, Nocturne
- → **Copying Settings** (`xdrip_plus_move_settings`) with → **Auto configure**
  (`auto_config_top`: camera / image file)
- → **Backups** (`backups_screen`): cloud backup, share QR, export/import DB,
  export CSV, load/save settings
- ⇒ BG data table · ⇒ Calibration data table (if `show_data_tables`)
- → **xDrip sync** (en:IU) with → Remote Snoozing and → Desert Sync

### Appearance (`pref_cat_appearance`)
- → **Colours** (legacy colour screen) · → **Display** (→ Font + display rows)
- → **Graph Settings** · ⇒ Show Libre trend (if a Libre block exists) ·
  → **Always On Display** (`xdrip_plus_accessibility`) · → **Extra Status Line** ·
  → **Number wall** · → **Number Icon** · → **Home screen** (`home-shelf-*`)

### Profile (`pref_cat_profile`, all rows en:IU)
- → Multiple Insulin Types · ⇒ Carb Ratio · ⇒ Insulin Sensitivity ·
  ⇒ Basal Profile Editor · `profile_carb_absorption_default` ·
  `simulations_enabled` · `predict_use_momentum` · → Low level prediction values

### Accessibility (`pref_cat_accessibility`)
- → **Speak Readings**

### Advanced (`pref_cat_advanced`)
- → **Bluetooth Settings** · ⇒ Events Log · → **Extra Logging** ·
  → **Inter-app** (+ Health Connect) · → **Other misc. options** ·
  → **Advanced Calibration**

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
