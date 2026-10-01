# Compose library & widget replacement map

Per-library plan for replacing the legacy UI libraries, custom `View`s and `RemoteViews` surfaces
with Compose (or an intentional non-Compose path). This is the companion to
[`Tech_Debt.md`](./Tech_Debt.md) (dependency register) and
[`Compose_Migration.md`](./Compose_Migration.md) (phase plan).

Status legend: **Done** (Compose in place), **Wrapped** (legacy renderer behind `AndroidView` for
now), **Planned**, **Keep** (no Compose equivalent needed / deliberate retention).

## UI libraries

| Legacy | Renders | Candidate replacement(s) | Affected views/files | Phase / status |
| --- | --- | --- | --- | --- |
| **hellocharts** (dead local AAR) | All charts (Home, notifications, widget, lockscreen, previews) | **[Vico](https://github.com/patrykandpatrick/vico)** (`com.patrykandpatrick.vico:compose-m3`); alternatives: KoalaPlot, ComposeCharts (ehsannarmani), YCharts | `Home` chart, `BgGraphBuilder`, `ui/chart/Horizontal*LineChartView` stopgaps, `ExampleChartPreferenceView` + `prefs_example_chart_layout`, notification big-picture, `XDripDreamService`, widget `RemoteViews` | **Phase 3** (Compose surfaces); **Wrapped** for the settings preview (`ThemeEditorScreen` `AndroidView`); RemoteViews/Bitmap surfaces stay on the legacy renderer until a non-Compose drawing path exists |
| **colorpicker** AAR | `ColorPickerPreference` dialogs on the legacy colour screen | Existing Compose picker (`SettingsColorRow` + `ColorPickerDialog`, S0) | Legacy `xdrip_plus_color_settings`; the Compose theme editor replaces it | **Done** for Compose; **delete AAR in S6** once the legacy colour screen is retired |
| **search-preference** AAR | Legacy settings search | Hand-rolled Compose search (`SETTINGS_SEARCH_INDEX` + root search field) | Legacy `Preferences` screen | **Done**; **delete AAR in S6** |
| **barista** (androidTest) | Espresso test DSL | Compose test APIs (`createAndroidComposeRule`, `onNodeWithTag`) | `app/src/androidTest` | **Planned** — replace remaining barista tests before S6 |
| **zxing-android-embedded** | QR display/scan | Keep, or Compose wrappers (`qrose`, `QRKit`) | `utils/DisplayQRCode`, barcode scanner | **Keep** |
| **appauth** | OAuth via Custom Tabs | Keep (no Compose UI needed) | OAuth flows | **Keep** |
| **ns-sdk / usb-serial / mongo / influx** | Non-UI | Keep | — | **Keep** |

## Legacy activities / custom views

Material 3 Compose equivalents: `DatePicker`/`TimePicker`, `AlertDialog`, `Canvas`, and **Glance**
for app widgets. Track V (`Settings_Migration.md` §7) migrates these.

| View/activity | Replacement |
| --- | --- |
| `ProfileEditor`, `BasalProfileEditor` | Compose editors (charts/columns last) |
| `NumberWallPreview`, `NumberGraphic` | Compose screens/`Canvas` |
| `DisplayQRCode`, `SdcardImportExport` | Compose screens (or keep the QR view) |
| `SendFeedBack`, `Agreement` | Compose screens |
| `AlertList` + `EditAlertActivity` | Compose list + editor (alert forms / chart) |
| `BTGlucoseMeterActivity` | Compose screen (admin/backup group) |
| `ExampleChartPreferenceView` | `AndroidView` wrapper today (theme editor); Vico later |
| **Done — Track V pass 1** | `TimePickerPrefActivity` + `TimePickerFragment` (→ `TimeOfDayDialog`), `LicenseAgreementActivity`, `SelectAudioDevice`, `InsulinProfileEditor`, `MissedReadingActivity`, `NightscoutBackfillActivity`, `DepositActivity` — Compose content in the existing activities, dedicated layouts deleted |
| **Done — Track V pass 2** | `Agreement`, `CalibrationCheckInActivity`, `CalibrationOverride`, `DoubleCalibrationActivity`, `XDripDreamSettingsActivity`, `HealthPrivacy`, `FakeNumbers` — same in-place Compose, layouts + Data Binding removed |
| **Done — Track V pass 3 (Medium)** | `MtpConfigureActivity`, `DatabaseAdmin`, `GluProActivity` — Compose UI, `NanoStatus`/`ObservableField`/`ObservableArrayList` bridged into Compose state; layouts deleted (GluPro `item_glupro_device` + `ViewModel` adapter kept for the service) |
| **Done — Track V pass 4 (rich Medium)** | `EmergencyAssistActivity` (`PrefsViewImpl` + model `ObservableField` + contact `ObservableList` bridged; `PrefsViewStringSnapDefaults` → `snapMinutesValue`; remove-confirm dialog in Compose) and `BackupActivity` (`ViewModel` `ObservableField`s + metadata `ObservableArrayMap` + automatic-backup prefs bridged) — layouts deleted, SAF/Drive/permission flows and dialogs stay in the activities |
| **Done — Track V pass 5 (sensor & calibration forms)** | `NewSensorLocation`, `StopSensor`, `AddCalibration`, `StartNewSensor`, `SnoozeActivity` — Compose content + M3 dialogs, drawer shell dropped, layouts deleted. Retained for the not-yet-migrated alert/reminder screens: `SnoozeActivity.SetSnoozePickerValues` + `snooze_picker.xml` (EditAlert) and the shared `DatePickerFragment`/`TimePickerFragment` (EditAlert/Reminders; retire with V6/V11) |

## RemoteViews / Bitmap-only surfaces

These cannot be drawn by Compose directly and are intentionally retained:

| Surface | Why | Approach |
| --- | --- | --- |
| App widget | `RemoteViews` | Keep XML/RemoteViews; **Glance** is the long-term option |
| Lockscreen wallpaper (`LockScreenWallPaper`) | `WallpaperManager` bitmap | Keep the legacy renderer |
| `NumberGraphic` | Bitmap generation shared by widget/wallpaper | Keep until the chart renderer is replaced |
| `XDripDreamService` | Daydream `RemoteViews` | Keep |

**Chart split:** Compose surfaces (Home, settings preview) move to Vico in Phase 3; notification,
widget and lockscreen surfaces render to `Bitmap`/`RemoteViews` and need a non-Compose drawing path
(or stay on the legacy renderer until then).
