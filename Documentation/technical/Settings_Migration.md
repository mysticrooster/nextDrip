# Settings → Compose migration (plan, status & verification)

Single authoritative doc for migrating the legacy **settings** subsystem
(`utils/Preferences.java` + the preference XMLs) and the **secondary views** to Compose.
Merges the former "settings plan" and "change list & verification passes" docs.

Companions: [`Compose_Migration.md`](./Compose_Migration.md) (umbrella strategy/phases),
[`Settings_S5a_Advanced.md`](./Settings_S5a_Advanced.md) (large-phase annex),
[`Tech_Debt.md`](./Tech_Debt.md) (dependency register), [`AGENTS.md`](../../AGENTS.md).

> **Non-negotiable invariant:** preference **keys and types never change**. Settings are user data,
> read app-wide via `Pref`/`PreferenceManager`; Compose must read/write the *same* keys so existing
> installs and backups keep working. Only the UI is replaced.

---

## 1. Scope

In scope: the main settings screen (`Preferences.java`, `BasePreferenceActivity`, the 7 settings
XMLs, custom preference views) and the settings-only libs (`search-preference`, `colorpicker`).
Out of scope: `Home`/charts (Phase 2/3), the drawer shell (Phase 1, done), the Room/`Flow`
follow-up, and the Wear module.

### Inventory (accurate)

| File | Screens | Categories | Leaves |
| --- | --- | --- | --- |
| `pref_general.xml` | 2 | 1 | 3 |
| `pref_license.xml` | 1 | 0 | 1 |
| `pref_notifications.xml` | 12 | 4 | 54 |
| `pref_data_source.xml` | 7 | 3 | 57 |
| `pref_data_sync.xml` | 16 | 1 | 67 |
| `pref_advanced_settings.xml` | 24 | 15 | 232 |
| `xdrip_plus_prefs.xml` | 23 | 32 | 157 |
| **Total** | **85** | **56** | **571** |

The legacy UI is effectively **one very large screen**: `AllPrefsFragment` inflates all XMLs, then
programmatically removes/relabels/hides sub-screens based on prefs and hardware. ~217
`findPreference(...)` listeners attach behaviour.

---

## 2. Architecture as built

New package `ui/settings/` (Kotlin), using the Phase 1 `XdripTheme` + `LocalXdripColors`.

### Host & navigation

- `SettingsActivity` (`ComponentActivity`) hosts everything; the drawer's Settings entry points here.
- `SettingsRoot` renders a **lightweight in-Compose screen stack**: `mutableStateListOf<SettingsScreen>`
  + `BackHandler` + a `TopAppBar` with a Material back `IconButton`. It also **resets the scroll to
  top on navigation** (one shared `rememberScrollState`).
- `titleFor(screen)` and `SettingsScreenContent(screen, onNavigate, onOpenClassic)` dispatch each
  `SettingsScreen`.
- Not-yet-migrated screens are reached via an "Classic settings" row → legacy `Preferences`.
- **Why not `navigation-compose`?** The sealed-stack is the smallest thing that works; swap to
  `navigation-compose` (already a dependency) once the graph covers all 85 sub-screens. Deep links
  (`jumpToScreen`) are still a follow-up.

### Row components (`SettingsComponents.kt`)

| Composable | Replaces |
| --- | --- |
| `SettingsCategory` | `<PreferenceCategory>` |
| `SettingsActionRow` | plain `<Preference>` / nested `<PreferenceScreen>` |
| `SettingsSwitchRow` | `SwitchPreference` / `CheckBoxPreference` |
| `SettingsEditTextRow` | `EditTextPreference` |
| `SettingsListRow` | `ListPreference` (bounded `LazyColumn` dialog) |
| `SettingsRingtoneRow` | `RingtonePreference` (system picker) |
| `SettingsTimeRow` | `TimePreference` (stores millis) |
| `SettingsColorRow` + `ColorPickerDialog` | `ColorPicker` (colorpicker AAR) |
| `SettingsSliderRow` | `SeekBarPreference` |

Rows take `modifier` (for `testTag`), `enabled` (for dependencies), and optional
`onCheckedChange`/`onValueChange`/`onSelected` (for side effects).

### State & gating

- `SettingsState` (`SettingsPrefs.kt`): a small `Pref`-backed snapshot holder (`bool/string/int/setX`)
  that makes reads reactive via a `mutableStateMapOf`. **No `SettingsViewModel`** — one isn't needed
  for row-level state; introduce one only when a screen needs cross-row derived state.
- `SettingsPrefs`: formatting/validation helpers mirroring the legacy `bindPreference*` listeners
  (numeric/unitised summary, glucose range check).
- `SettingsVisibility` (`SettingsVisibility.kt`): the legacy `removePreference` matrix as Compose
  conditions — `collectionType(state)`, `isEngineeringMode(state)`, `hasLibre`, `hasWifi`,
  `isBlueReader()`, `isLibreReceiver()`. **Gates must read through `SettingsState`** so toggling a
  master (e.g. `engineering_mode`) recomposes the screen.
- Dependencies (`android:dependency`, `disableDependentsState`) → `SettingsState.dependentEnabled`.

### Theme integration

Every colour defaults to **Material You** and any user pick overrides it (registry
`ui/theme/ThemeColor`, resolution `resolveXdripColorScheme` / `xdripColor` / `LocalXdripColors`).
Components read chrome from `MaterialTheme.colorScheme`; no component hardcodes a colour. The
**Theme editor** (`ui/settings/ThemeEditorScreen.kt`) exposes every colour with a reset. See
[`Compose_Migration.md`](./Compose_Migration.md) §8.

---

## 3. Track S — sequencing & status board

Order is deliberate: build the framework once, prove it on the smallest screen, then work
cheapest/self-contained → hardware-heavy, deleting the legacy UI only after parity.

| Step | Scope | Status | Commit |
| --- | --- | --- | --- |
| **S0** | `ui/settings/` framework: components, hand-rolled stack, colour picker, root search, app-theme data | **Done** | `0a0caf350`, `08acd9892` |
| **S1** | `pref_general` + `pref_license` (pilot) | **Done** | `0a0caf350` |
| **S2** | `pref_notifications` (dialogs, sounds, dependencies, unitised numeric) | **Done** | `08acd9892` |
| — | Material You defaults + per-colour overrides + Theme editor | **Done** | `87d4c2239`, `2f4139cfd` |
| **S3** | `pref_data_source` (collection-type gating, side effects) | **Done** | `c1d090169` |
| **S4** | `pref_data_sync` | **Done** | `27a344d08` |
| **S5a** | `pref_advanced_settings` | **Done** — "Other settings" and watches | `274fd4e6e`, watches pass |
| **S5b** | `xdrip_plus_prefs` | **Done** — Extra Settings tree + theme-editor colour parity | this pass |
| **S6** | Retire legacy settings | Todo | — |

Also landed: a host scroll-reset fix (`274fd4e6e`); long-list dialogs now scroll
(`81ad2805d`).

---

## 4. How to port a category (agent recipe)

1. **Parse the XML** to get the exact tree (keys/titles/defaults/`dependency`). Read-only snippet:

   ```bash
   python3 - <<'PY'
   import xml.etree.ElementTree as ET
   NS='{http://schemas.android.com/apk/res/android}'
   def a(e,n): return e.get(NS+n)
   def short(t): return t.split('.')[-1]
   def walk(e,d):
       k=a(e,'key'); t=a(e,'title')
       if e.tag in ('PreferenceScreen','PreferenceCategory'):
           print('  '*d+f"[{short(e.tag)}] key={k} title={t}")
           for c in e: walk(c,d+1)
       else:
           extra=' '.join(f"{n}={a(e,n)}" for n in ('defaultValue','summary','dependency','inputType','max','min','entries','entryValues') if a(e,n))
           print('  '*d+f"<{short(e.tag)}> key={k} title={t} {extra}")
   for c in ET.parse('app/src/main/res/xml/pref_XXXX.xml').getroot(): walk(c,0)
   PY
   ```
2. **Read `Preferences.java`** for the `removePreference`/`addPreference` and listener logic that
   touches those keys (the matrix lives ~L1334–2200, plus each `addPreferencesFromResource` block).
3. **Wire navigation**: add `data object` entries to `SettingsScreen` (`SettingsNavigation.kt`), a
   `titleFor` branch and a `SettingsScreenContent` branch (`SettingsScreens.kt`), the root category
   row, and a `SETTINGS_SEARCH_INDEX` entry.
4. **Build the screen** with the row helpers. Gate with `SettingsVisibility` (read via
   `SettingsState`); attach side effects via `onCheckedChange`/`onValueChange`/`onSelected`.
5. **Test** (Robolectric + Compose, see §8): a gate test (`Pref` set → row appears/absent) and a
   round-trip test (compose action → `Pref` read).
6. **Verify**: `./gradlew :app:testFastDebugUnitTest :app:assembleFastDebug` (CI uses
   `assembleProdRelease testProdReleaseUnitTest`).
7. **Update** this doc's status board + the relevant pass matrix.

---

## 5. Per-category change lists

### S1 / S2 — General, Alarms & Alerts (done)
General: units list (`mgdl`/`mmol`), high/low (numeric), license row. Notifications: the whole
`pref_notifications` tree — dependency/`disableDependentsState`, `ListPreference`s, ringtone rows,
calibration-alert clearing on toggle, unitised numeric range validation.

### S3 — Data Source (`pref_data_source.xml`) — done
- `dex_collection_method` list **with change side effects**: Follower resets
  `bridge_battery`/`parakeet_battery`, turns off `plus_follow_master`, `GcmActivity.requestBGsync()`;
  `DexcomShare` forces `calibration_notifications=false`; always
  `CollectionServiceStarter.restartCollectionServiceBackground()`.
- Collection-type gates: Web Follower, NFC (Libre), share key + barcode action, `dex_txid`,
  Medtrum, NSFollow, SHFollow, follower chime, CLFollow, G5 debug, wifi receivers.
- Sub-screens: Web Follower (proxy dependents), NFC Scan Features, NS follow download-treatments,
  G5/G6 debug (OB1 options + battery, hidden for G7) + OB1 preemptive-restart.

### S4 — Data Sync (`pref_data_sync.xml`) — done
Auto configure; Cloud upload hub (REST API + download/extra, MongoDB, InfluxDB, Dexcom share,
Tidepool (test login, latency slider, cred listeners), Web Deposit (engineering-only), NightLite,
Nocturne); Glucose meters (scan/pair, calibration dependents).

### S5a — Advanced Settings (`pref_advanced_settings.xml`) — done
See [`Settings_S5a_Advanced.md`](./Settings_S5a_Advanced.md). "Other settings" branch (TTS, inter-app +
Health Connect, extra status line, calibration, Bluetooth, BlueReader/Libre2, logging, misc) and the
**watches** branch (Wear/Pebble/Amazfit/BlueJay/LeFun/MiBand/Smartwatch sensors) are migrated.

### S5b — xDrip+ Extra Settings (`xdrip_plus_prefs.xml`) — done
The whole tree is now Compose (`ui/settings/XdripPlusScreens.kt`): **Copying** (`DisplayQRCode`,
`SdcardImportExport`), **Update** (`update_channel` engineering entry swap + title, crashlytics toast,
telemetry dependency, feedback intent), **Motion** (dependency chain, `SelectAudioDevice` intent;
mutual exclusion via the host-lifetime `ActivityRecognizedService.prefListener`), **Experimental →
Pens** (Novopen/InPen/Pendiq, masked `maxLength` fields, `inpen_enabled` BT-location + refresh,
`inpen_reset`/`numberIconTest` Home extras), **Prediction** (`I_understand` gate, profile editors,
carb-absorption validation + `Profile.reloadPreferences`/`Home.staticRefreshBGCharts`, adv-predict
decimals, EULA), **Sync** (key auto-generation + `PlusSyncService` restart, cloud listener,
remote-snooze + desert-sync sub-screens with the master-IP runtime removal, `disable_all_sync` →
`hardReset`), and **Display** (font/language incl. `hardReset`, graph display/smoothing/Y-axis,
`widget_range_lines` → `WidgetUpdateService`, accessibility, number wall incl. the colour rows moved
here and `TimePickerPrefActivity` seconds-as-String rows, number icon, `show_home_on_boot`).
**Theme parity (D2):** the theme editor is restructured to the legacy titled colour groups/order with
`use_flair_colors`, `plugin_plot_on_graph` gating for `color_secondary_glucose_value`, hex for
`color_basal_tbr` and the `ExampleChartPreferenceView` preview; number-wall colours render on the
Number Wall screen instead.

### S6 — Retire legacy settings — todo
After parity: delete `Preferences.java`/`BasePreferenceActivity`/`TimePreference`/
`ExampleChartPreferenceView` + the pref XMLs; remove `search-preference` and (if unused)
`colorpicker`; retire the `PrefsView*` settings usage; point widgets/shortcuts/deep links at the
Compose host.

---

## 6. Cross-cutting leftovers

- **Deep links**: `Preferences.jumpToScreen(key)` → a `SettingsScreen` argument on `SettingsActivity`.
- **Search scope**: `SETTINGS_SEARCH_INDEX` covers sub-screens; extend to leaf prefs.
- **Custom widgets**: `ExampleChartPreferenceView` row; multi-select / tree-selector / PIN dialogs.
- **Icons**: legacy sub-screens carry `android:icon`; add leading icons to `SettingsActionRow`.
- **Live pref-change listeners**: the legacy activity registered service/watch/collector listeners
  while open. The collection-method reactions were reproduced **explicitly** in S3; the **watch**
  listeners (`MiBandEntry`/`LeFunEntry`/`BlueJayEntry`), the **number-wall** listener
  (`LockScreenWallPaper.PrefListener`), the **motion** listener (`ActivityRecognizedService`), and
  the **cloud** listener (`use_xdrip_cloud_sync` → `Pusher.requestReconnect()` +
  `CollectionServiceStarter.restartCollectionServiceBackground()`) are now registered for the
  Compose host lifetime in `SettingsActivity` (S5a watches, S5b extras).
- **`summaryOn`/`summaryOff`**: switch state text is currently rendered as a computed `subtitle`.

---

## 7. Track V — secondary views

Migrate the legacy screens launched from settings. Group by shape; preferred order **F** info/legal
(quick) → **A/B** alert & sensor forms (reuse settings rows) → **G** misc → **D** tables/logs
(chart-wrapping) → **C** editors (highest complexity) → **E** admin/backup.

**Pass 1 — done (quick, self-contained wins).** Hosting: each legacy activity keeps its class,
manifest entry and launch intent; only its content becomes Compose (`setContent`), with a shared
Material 3 scaffold (`ui/secondary/SecondaryScaffold.kt`) replacing the action bar / nav drawer.
Migrated: `TimePickerPrefActivity` (`ui.secondary.TimePickerPrefScreen`), `LicenseAgreementActivity`,
`SelectAudioDevice`, `InsulinProfileEditor`, `MissedReadingActivity`, `NightscoutBackfillActivity`,
`DepositActivity`. Their dedicated XML layouts were deleted; `DepositActivity`/`SelectAudioDevice`
Data-Binding and `MissedReadingActivity`'s implicit on-destroy save were removed. New shared pieces:
`SecondaryScreen`, `SettingsMinutesOfDayRow` / `TimeOfDayDialog`, and the testable
`BackfillGuard`. Note `SelectAudioDevice`'s static MAC helpers and `EditAlertActivity`'s
`shortPath`/`timeFormatString` stay (used by `HeadsetStateReceiver` / alert screens).

**Pass 2 — done (trivial standalone screens).** Same in-place hosting. Migrated: `Agreement`
(first-run warning gate), `CalibrationCheckInActivity`, `CalibrationOverride`,
`DoubleCalibrationActivity`, `XDripDreamSettingsActivity`, `HealthPrivacy`, `FakeNumbers`. Layouts
deleted; `DoubleCalibrationActivity`/`XDripDreamSettingsActivity` Data Binding and
`Agreement`'s view code removed. `Agreement.prefmarker` kept public for parity.

**Pass 3 — done (Medium: small Data-Binding screens).** `MtpConfigureActivity` (keeps the
`NanoStatus` polling semantics, now Compose-driven), `DatabaseAdmin` (console + SQL actions; the
activity keeps its processors and its `console` `ObservableField`), `GluProActivity` (device list;
the shared `ViewModel` is retained and its observable list/boolean bridged into Compose). Layouts
deleted; `ViewModel`/adapter fields left in place for the service.

**Pass 4 — done (rich Medium).** `EmergencyAssistActivity` — contact picker + SMS/contacts/
location permission flows kept in the activity; its `PrefsViewImpl`, `EmergencyAssist` model and
contact `ObservableList` are bridged into Compose, and `PrefsViewStringSnapDefaults` is
reimplemented as `snapMinutesValue` with identical keys/defaults (lower/upper/inactivity snap to
60/240/1440; the lowest-alert threshold keeps the legacy no-default behaviour). `BackupActivity` —
SAF/Google Drive sign-in, the `BackupStatus` sink and the chooser/restore dialogs kept in the
activity; the `ViewModel`'s `ObservableField`s, metadata `ObservableArrayMap` and the
automatic-backup prefs are bridged into Compose. Both binding layouts and their item layouts are
deleted.

**Pass 5 — done (V4 sensor & calibration forms).** `NewSensorLocation` (radio list + Other field),
`StopSensor` (confirm-gated stop + reset calibrations; G6/G7 copy and the `resettableCals`
predicate kept in the activity), `AddCalibration` (blood glucose entry; automated-calibration
intent handling and the blood-test/calibration side effects kept in the activity, which returns
the legacy validation error), `StartNewSensor` (start action; Bluetooth location permission,
insertion date/time prompts and collector-specific start chain kept in the activity) and
`SnoozeActivity` (status/visibility derived in Compose from the alert and disabled-until prefs;
M3 slider + list dialogs replace the NumberPickers; static snooze helpers and per-type
disable/clear/remote actions kept). All five dropped `ActivityWithMenu` for `BaseAppCompatActivity`
+ `SecondaryScreen` (matching the earlier `CalibrationOverride`/`DoubleCalibrationActivity`
handling), and their layouts were deleted. Retained for the not-yet-migrated alert/reminder
screens: `SnoozeActivity.SetSnoozePickerValues` + `snooze_picker.xml` and the shared
`DatePickerFragment`/`TimePickerFragment` (retire with V6/V11).

**Remaining:** `ErrorsActivity`, `FollowerManagementActivity`, `AlertList` + `EditAlertActivity`,
`ProfileEditor`, `BasalProfileEditor`, `NumberWallPreview`, `DisplayQRCode`, `SdcardImportExport`,
`SendFeedBack`, `BTGlucoseMeterActivity`, plus the app-wide/drawer surfaces and the Data-Binding /
`NanoStatus` group (`EventLogActivity`, `NoteSearch`, `PhoneKeypadInputActivity`, `MegaStatus`,
`ThinJamActivity`). The `PrefsView*` bridge retires once its remaining users (`Home`,
`BackupActivity`, `EmergencyAssistActivity`, `NumberWallPreview`, `DisplayQRCode`) move.


---

## 8. Verification passes

Run independently; repeat per category. Legend: **Ready** = applicable now.

- **A — Structure & navigation.** Every legacy category/sub-screen exists with the same
  title/order/breadcrumb. *How:* walk the XML vs `SettingsNavigation.kt`/`SettingsScreens.kt`;
  unit test each category's reachable destinations. *Ready.*
- **B — Key & type parity.** Compose writes the exact key/type the legacy reads. *How:*
  round-trip tests + grep the key strings. *Ready.*
- **C — Defaults parity.** Untouched install behaves as before. *How:* default-resolution tests +
  the colour migration test. *Ready.*
- **D — Conditional visibility.** Visible set identical per `DexCollectionType`, engineering,
  hardware. *How:* parameterised tests over the gates. *S3/S4/S5b done; S5a partial.*
- **E — Side effects.** Changing a setting triggers the same action. *How:* test/verify the
  documented callbacks. *S3/S4/S5b done; S5a partial.*
- **F — Summaries/validation.** `bindPreference*` behaviour reproduced. *Partly ready.*
- **G — Custom widgets.** Every custom widget has a Compose equivalent + test. *Partly ready
  (`ExampleChartPreferenceView` is `AndroidView`-wrapped in the theme editor).*
- **H — Search parity.** Migrated search matches `search-preference` breadth. *Pending.*
- **I — Theming.** Material You defaults, overrides win, no literals. *Ready (`ThemeColorTest`).*
- **J — Legacy interop/retirement.** S6 artefacts deleted, no dangling users. *Pending (S6).*
- **K — Global regression.** Full suite + `assembleFastDebug` (R8) + backup/restore. *Run every pass.*

### Passes × category matrix

| Category | A | B | C | D | E | F | G | I | K |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| S1 General | ✅ | ✅ | ✅ | n/a | n/a | ✅ | ✅ | ✅ | ✅ |
| S2 Notifications | ✅ | ✅ | ✅ | n/a | ✅ | ✅ | ✅ | ✅ | ✅ |
| S3 Data Source | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| S4 Data Sync | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| S5a Other settings | ✅ | ✅ | ✅ | ✅ | partial | ✅ | ✅ | ✅ | ✅ |
| S5a Watches | ✅ | ✅ | ✅ | ✅ | partial | ✅ | ✅ | ✅ | ✅ |
| S5b xDrip+ Extra Settings | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V1 Track V quick wins | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V2 Track V trivial screens | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V3 Track V medium (DB screens) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V4 Track V rich Medium | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V5 Track V sensor/calibration forms | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

(H = search and J = retirement are app-wide and tracked above.)

---

## 9. Risks & mitigations

| Risk | Mitigation |
| --- | --- |
| **Preference-key drift** breaks settings/backups | Enforce the invariant; round-trip tests. |
| **Conditional/hardware screens** (collection type, watch model, engineering) | Model as Compose state via `SettingsVisibility`; test both branches. |
| **Scope** (85 sub-screens, ~50 activities) | Strict screen-by-screen; delete legacy only after parity. |
| **Custom dialogs/summaries** (unitised numeric, sounds, QR) | Reuse the shared rows/formatters. |
| **Data Binding interop** during the hybrid period | Established `AndroidView`/delegate pattern. |
| **`colorpicker`/`search-preference` removal** while other screens use them | Remove only after the last user migrates (`Tech_Debt.md`). |

---

## 10. Testing strategy

- **State/parity unit tests** (Robolectric + Compose, no screenshots): assert Compose writes the
  right keys and gates resolve correctly. Use `createAndroidComposeRule<SettingsActivity>` /
  `createComposeRule`; `performScrollTo` before clicking off-screen rows; dialogs are separate roots.
- **Round-trip tests**: set via Compose → read via `Pref`, and vice versa.
- **Manual parity** on-device against the legacy screen (values, summaries, conditional visibility,
  dialogs, sounds).
- Existing behaviour tests (alert/snooze/missed/errors) stay green.

---

## 11. Definition of done

- Every settings category/sub-screen and secondary view is Compose (or `AndroidView`-wrapped where
  intentionally retained, e.g. the basal drag editor/charts).
- No `android.preference` UI classes remain; `Preferences.java`/`BasePreferenceActivity` and the
  pref XMLs are deleted.
- `search-preference`/`colorpicker` removed once unused.
- Pref keys unchanged; backups compatible; full suite + R8 green; manual parity checked.
