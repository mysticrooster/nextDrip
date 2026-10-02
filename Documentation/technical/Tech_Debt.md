# Tech Debt & Dependency Modernization

Companion to [`Compose_Migration.md`](./Compose_Migration.md). This register tracks
the dependencies, legacy AARs, and frameworks that should be replaced or retired as
part of the Jetpack Compose migration and the broader modernization of xDrip+.

## How to use this document

- Each item has an **owner phase** (mapping to the phases in `Compose_Migration.md`)
  or a **track** (Compose migration vs. framework modernization).
- Update the **status** column as work lands. Do not delete rows until the item is
  fully retired.
- "Effort" is a rough relative estimate, not person-days.

---

## 1. Dependency Migration Register — Compose targets

View-based / XML libraries that Compose replaces. These are retired incrementally as
screens migrate. The settings/secondary-views phases are planned in
[`Settings_Migration.md`](./Settings_Migration.md); the per-library UI replacement map is in
[`Compose_Library_Replacements.md`](./Compose_Library_Replacements.md).

| Dependency | Usage | Replace with | Track / Phase | Effort | Status |
| --- | --- | --- | --- | --- | --- |
| `hellocharts` (local AAR) | 17 files | [Vico](https://github.com/patrykandpatrick/vico) | Compose / Phase 3 | High | Not started — Home pan clamped horizontal by `ui/chart/Horizontal*LineChartView` stopgaps (delete with Vico) |
| `colorpicker` AAR (`com.rarepebble.colorpicker`) | 3 files + prefs XML | Compose-native color picker | Compose / Phase 4 | Low | **Done (S6)** — AAR, `ColorPicker`/`ColorPreferenceDialog` and the legacy colour page deleted; all picks use `com.github.skydoves:colorpicker-compose:1.1.2` |
| `search-preference` (local AAR) | 1 file | Compose search/settings UI | Compose / Phase 4 | Low | **Done (S6)** — AAR deleted with the legacy `Preferences` activity |
| `com.github.amlcurran.showcaseview` | 13 files | Compose tooltips/coach-marks (or drop) | Compose / Phase 2–5 | Medium | Not started |
| `androidx.preference` | settings screens | Compose settings | Compose / Phase 4 | Medium | Keep — `preference:1.2.1` stays for `PreferenceManager.getDefaultSharedPreferences`/`setDefaultValues`; the `android.preference` UI no longer uses it |
| `android.preference` settings UI (`Preferences.java`, `BasePreferenceActivity`, `pref_*.xml`) | main settings screen | Compose settings host | Compose / Phase 4 (S6) | High | **Done (S6)** — activity/XML/custom widgets deleted; non-UI API extracted to `SettingsSupport`, defaults to `SettingsDefaults` (fixture-tested), entry points and listeners repointed. The residual UI-class references (`MiBandEntry`, `BlueJayAdapter`, `LockScreenWallPaper`, `PluggableCalibration`) were removed in the status-refresh pass, so `android.preference` now remains only via the `PreferenceManager` data API |
| `com.github.skydoves:colorpicker-compose:1.1.2` | new (S6) | `HsvColorPicker`/`AlphaSlider`/`BrightnessSlider` in the shared `ColorPickerDialog` + NumberWallPreview | Compose / Phase 4 | — | Adopted — survives R8 (`assembleFastDebug`); pulls Kotlin 2.0.0 + Compose Multiplatform 1.6.11, which resolve to the AndroidX BOM (no duplicate classes). Revisit with the Kotlin 2.4 / BOM 2026 track |
| `androidx.recyclerview` | 8 files | `LazyColumn` / `LazyRow` | Compose / cross-cutting | Medium | Not started |
| `androidx.cardview` | legacy layouts | `Card` / `Surface` | Compose / cross-cutting | Low | Not started |
| `androidx.constraintlayout` | 9 layouts | Compose layouts | Compose / cross-cutting | Medium | Not started |
| `androidx.appcompat` / `material` (XML) | 24 / 1 files | Material 3 (retire as screens migrate) | Compose / cross-cutting | High | Not started |
| `com.journeyapps:zxing-android-embedded` | 10 files | CameraX / ML Kit barcode, or `AndroidView` wrap | Compose / Phase 5 | Medium | Not started |
| `me.tatarka.bindingcollectionadapter2` + Data Binding | 10 layouts | `ViewModel` / `StateFlow` (retire binding) | Compose / cross-cutting | High | Partial — Track V passes 1–7 deleted the `DepositActivity`/`SelectAudioDevice`/`DoubleCalibrationActivity`/`XDripDreamSettingsActivity`/`DatabaseAdmin`/`MtpConfigureActivity`/`GluProActivity`/`EmergencyAssistActivity`/`BackupActivity`/`StopSensor`/`NumberWallPreview`/`DisplayQRCode` binding layouts. Pass 7 also removed `EventLogActivity`'s binding layout + `MergeObservableList` adapter (bridged into Compose) and the remaining AAR-free secondary binding-free screens; `PrefsViewImpl` still used by `Home`, `BackupActivity`, `EmergencyAssistActivity`, `NumberWallPreview` |
| `com.getpebble:pebblekit` | 7 files | remove (dead watch) | Compose / Phase 5 | Low | Not started — settings UI migrated in S5a; AAR removal pending a product decision. Note: its `PebbleKit.register*` receivers called flag-less `Context.registerReceiver` (targetSdk 34 crash) and are now self-registered in `PebbleWatchSync` |

---

## 2. Dead / legacy AARs to retire

Local and unmaintained AARs that should not survive the migration.

| Dependency | Notes | Recommendation | Effort | Status |
| --- | --- | --- | --- | --- |
| `thread-safe-active-android` (ActiveAndroid ORM) | removed — all 28 tables on Room | migrated (see §5) | **High** | **Done** |
| `amazfitcommunication-master` AAR | companion device | review / remove if unused | Low | Not started — settings UI migrated in S5a; AAR removal pending a product decision. Note: its `TransporterClassic.get` registered a receiver flag-lessly (targetSdk 34 crash), now worked around in `Amazfitservice` via a context wrapper |
| `appauth-release` AAR | OAuth | keep (external SDK) | — | Keep |
| `ns-sdk-full-release` AAR | Nightscout SDK (follower/download) | → port AndroidAPS `core/nssdk` (see §6) | High | In scope (deferred) |

---

## 3. Legacy frameworks / languages

Modernization that is not Compose-specific but is part of the overall cleanup.

| Item | Usage | Recommendation | Effort | Status |
| --- | --- | --- | --- | --- |
| RxJava 1 (`io.reactivex:rxjava:1.3.3`) | 6 files | → coroutines (already a dependency) | Low | **Done** (→ `java.util.function.Consumer`) |
| joda-time (`net.danlew:android.joda`) | 1 file | → `java.time` (minSdk 26) | Low | **Done** (own code migrated; joda-time kept — Nightscout SDK, incl. the planned AndroidAPS port, uses it) |
| `com.evernote:android-job` | 3 files | → WorkManager (already present) | Low | **Done** |
| Dagger `2.25.4` | DI | → Hilt (or modern Dagger) | Medium | Not started |
| Lombok | pervasive | → Kotlin data classes (long-term, optional) | **High** | Not started |
| `android.preference.PreferenceManager` | `ColorCache` defaults | → `androidx.preference` | Low | **Reverted** (full `android.preference` → `androidx.preference` is a larger effort, tied to the Phase 4 settings screens) |

---

## 4. Wear module (separate effort)

The `wear` module is its own legacy surface and should be planned independently.

Note (AGP 9 upgrade): the `wear` module no longer builds under AGP 9.4.1 — its old
`androidx.vectordrawable` 1.0.0 transitive dependency uses a duplicate `namespace`
that AGP 9 now rejects. The module was partially migrated (`compileSdk =`,
`androidComponents`, `resValues`, `proguard-android-optimize.txt`) but still needs
its dependency stack modernized before it compiles.

| Dependency | Notes | Recommendation | Effort | Status |
| --- | --- | --- | --- | --- |
| `com.google.android.support:wearable:2.5.0` | old support lib | → Wear Compose / `androidx.wear` | High | Not started |
| `com.google.android.gms:play-services-wearable:10.2.1` | very old | → current play-services-wearable | Medium | Not started |
| `ustwo-clockwise-debug` AAR | watch face | review / remove | Low | Not started |
| `wearpreferenceactivity-0.5.0` AAR | settings | → Wear Compose settings | Medium | Not started |

---

## 5. ActiveAndroid → Room (done)

The largest single modernization item. ActiveAndroid was an abandoned ORM and the data
backbone of the app (29 `@Table` classes, 64 files, ~478 query sites). It is now fully
replaced by Room.

**Detailed plan:** [`ActiveAndroid_to_Room.md`](./ActiveAndroid_to_Room.md).

- **Status (2026-09-28):** **complete.** All 28 tables (plus `Libre2Sensors` as a
  `@DatabaseView`) now use Room behind their existing façades, and ActiveAndroid is fully
  retired (AAR, `initialize()`, ContentProvider, and `@Table`/`Model` usage removed).
  A generation-based `LegacyDataImporter` copies legacy rows out of `DexDrip.db` (with
  `NOT NULL` coercion so nullable legacy columns are not dropped) and can be re-run to
  backfill rows; `Backup` includes `xdrip-room.db`. Façades preserved, tests green.

- **Why now:** the Compose migration will consume `BgReading`/`Calibration`/`Treatment`
  data heavily (Home, charts, stats). A clean, observable data layer (`Room` +
  `Flow`/`StateFlow`) makes Compose screens far simpler than querying the ActiveAndroid
  static API.
- **Approach:** migrate one model at a time, keeping the public accessor methods
  (`BgReading.last()`, `Calibration.latestValid(n)`, etc.) as a stable façade so
  callers do not change en masse. Introduce Room behind the existing façade first,
  then remove ActiveAndroid.
- **Risk:** data integrity is critical (medical data). Add parity tests per model
  (existing `*Test` classes are the baseline) before switching any model's storage.
- **Track:** framework modernization, parallel to the Compose phases. Own its own
  backlog; recommended *before* the deep Home slices.
- **Follow-ups (deferred, tracked in `ActiveAndroid_to_Room.md`):** drop
  `allowMainThreadQueries` (expose `Flow`/`suspend` DAOs + run writes off the main thread);
  optionally introduce real Room `@ForeignKey`/`@Relation` (currently transient objects + id
  columns). Neither blocks the Compose work.

---

## 6. Nightscout SDK → port from AndroidAPS (in scope, deferred)

The follower/download path uses `ns-sdk-full-release.aar`, an unmaintained
`info.nightscout.sdk` build. [AndroidAPS](https://github.com/nightscout/AndroidAPS)
maintains a modernized fork — `core/nssdk` — as a self-contained, Retrofit-based
module.

- **What we get:** Nightscout **v3** API coverage (`status`, `lastModified`,
  `entries`, `treatments`, `devicestatus`, `food`, `profile`), **Access Token**
  auth (`NSAuthInterceptor` + `NightscoutAuthRefreshService`), and a clean
  local-model / remote-model / mapper separation.
- **Stack:** Retrofit 2 + OkHttp + Gson + Kotlin coroutines (optionally RxJava 3).
  xDrip already ships Retrofit 2.9.0, OkHttp 5.3.2, Gson, and Kotlin coroutines,
  so only the API definition, models, mappers, and auth flow need porting.
- **Approach:** port `NightscoutRemoteService`, the `remotemodel`/`localmodel`/
  `mapper` classes, and the auth flow into a new `nightscout` package, replacing
  `ns-sdk-full-release.aar`. Start with the follower read paths (`entries`,
  `treatments`, `devicestatus`, `status`, `lastModified`), then add write paths
  if needed. Note xDrip already has a *separate* uploader
  (`com.github.nightscout:android-uploader`).
- **License:** both projects are GPLv3, so reuse is compatible.
- **joda-time note:** AndroidAPS's SDK also uses joda-time, so joda-time stays on
  the classpath regardless of this port (intentionally retained — see §3).
- **Track:** framework modernization, parallel to the Compose phases. Deferred;
  own backlog.

---

## 7. targetSdk 34 runtime-crash audit (peripherals & bundled libraries)

Audit (2026-09-29) of every bundled AAR/jar, the resolved Maven peripheral stack, the
library modules, and the app's own Bluetooth/watch/CGM code for APIs that crash on
modern `targetSdk` (flag-less dynamic `registerReceiver`; `PendingIntent` without
`FLAG_IMMUTABLE`/`FLAG_MUTABLE`; foreground services without a `foregroundServiceType`).

**Fixed in-repo**

| Item | Finding | Fix |
| --- | --- | --- |
| `com.getpebble:pebblekit` | `PebbleKit.register*` used flag-less `Context.registerReceiver` | `PebbleWatchSync` self-registers the same receivers via `ContextCompat.registerReceiver(..., RECEIVER_EXPORTED)` |
| `amazfitcommunication` AAR | `TransporterClassic.get` used flag-less 2-arg `registerReceiver` | `Amazfitservice` passes a `FlaggedReceiverContext` wrapper forcing `RECEIVER_EXPORTED` |
| FGS types | `ExternalStatusService`, `WifiCollectionService`, `G5CollectionService`, `DexShareCollectionService`, `WebFollowService` had no `android:foregroundServiceType` (Android 14 `MissingForegroundServiceTypeException`) | types added (`dataSync` / `connectedDevice`) in `AndroidManifest.xml`; runtime calls already pass `FOREGROUND_SERVICE_TYPE_MANIFEST` or the manifest type applies |

**Clean (verified, no action):** `appauth`, `barista`, `hellocharts`, `ns-sdk-full`,
`influxdb-java`, `mongo-java-driver`, `usb-serial-for-android`, `xdrip-cloud`; library modules
`:libglupro`, `:libkeks`, `:ipluginda`, `:localeapi`; app `PendingIntent` call sites (all flagged);
Nordic BLE / RxAndroidBle / zxing / Joda `registerReceiver` calls (protected broadcasts); Sentry
system-event breadcrumbs (integration disabled + `catch(Throwable)`). (`colorpicker` and
`search-preference` were also clean but have since been removed in S6.)

**Open third-party risk — Play Services 15.x `PendingIntent` flags** (not fixable
without a GMS upgrade; reachable on error-resolution paths, notably on devices with
missing/outdated Play Services):

| Artifact | Class / method | Pattern |
| --- | --- | --- |
| `play-services-wearable:15.0.0` | `com.google.android.gms.wearable.internal.zzhg.connect` | `PendingIntent.getActivity(..., 0)` — no mutable/immutable (narrow: China Wear app branch) |
| `play-services-base`/`-basement:15.0.1` | `GoogleApiAvailabilityLight.getErrorResolutionPendingIntent`, `GoogleApiActivity.zza`, `GoogleApiManager`, `zzr.zzad` | `getActivity(..., FLAG_UPDATE_CURRENT)` — no mutable/immutable |

Recommendation: upgrade the GMS stack (`play-services-base`/`-basement` ≥ 18.x,
`play-services-wearable` ≥ 18.x). This is a broader migration because the app pins
`play-services-auth`/`-location`/`firebase-messaging` at 15.0.0 and newer majors change
APIs (e.g. `FirebaseInstanceId`); schedule it as its own task rather than a drive-by bump.

---

## 8. Follow-ups unlocked by S6

- **Dependency modernization track:** Kotlin 2.4, Compose BOM 2026.x, Material3 1.4,
  `compileSdk` 36 and the matching activity/navigation bumps. Once that lands, a newer
  `colorpicker-compose` can replace the pinned `1.1.2` (which is built against Kotlin 2.0.0 /
  Compose Multiplatform 1.6.11). Tracked separately, not part of S6.
- **Stale settings tests — reconciled:** the IA redesign (`6b426510b`) had left three red tests
  unrelated to S6: `SettingsActivityTest`'s `setting_data_source`/`setting_libre_device` tag
  expectations, `SettingsIaTest`'s `setting_reminders`-under-Alarms expectation, and
  `CollectionMethodArraysTest`'s `DexCollectionMethodValues` count. All three now match the current
  navigation/arrays — Devices inlines `DataSourceScreen` (no intermediate `setting_data_source`
  row), reminders/emergency live under General, and the collection arrays are the consolidated
  16-entry pair.
- **Leaf-pref search (pass H remainder):** destination-level search is done, but indexing individual
  leaf preferences and jump-to-row still needs a pref-key/title catalog.

---

## Ownership conventions

- **Compose track** items land in the corresponding `Compose_Migration.md` phase.
- **Framework modernization** items are scheduled independently and can be picked up
  opportunistically (e.g. the three "Low effort" items in §3 are good first tasks).
- Mark items `In scope`, `Not started`, `In progress`, or `Done`.
