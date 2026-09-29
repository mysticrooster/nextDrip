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
screens migrate.

| Dependency | Usage | Replace with | Track / Phase | Effort | Status |
| --- | --- | --- | --- | --- | --- |
| `hellocharts` (local AAR) | 17 files | [Vico](https://github.com/patrykandpatrick/vico) | Compose / Phase 3 | High | Not started |
| `colorpicker` AAR (`com.rarepebble.colorpicker`) | 3 files + prefs XML | Compose-native color picker | Compose / Phase 4 | Low | Not started |
| `search-preference` (local AAR) | 1 file | Compose search/settings UI | Compose / Phase 4 | Low | Not started |
| `com.github.amlcurran.showcaseview` | 13 files | Compose tooltips/coach-marks (or drop) | Compose / Phase 2–5 | Medium | Not started |
| `androidx.preference` | settings screens | Compose settings | Compose / Phase 4 | Medium | Not started |
| `androidx.recyclerview` | 8 files | `LazyColumn` / `LazyRow` | Compose / cross-cutting | Medium | Not started |
| `androidx.cardview` | legacy layouts | `Card` / `Surface` | Compose / cross-cutting | Low | Not started |
| `androidx.constraintlayout` | 9 layouts | Compose layouts | Compose / cross-cutting | Medium | Not started |
| `androidx.appcompat` / `material` (XML) | 24 / 1 files | Material 3 (retire as screens migrate) | Compose / cross-cutting | High | Not started |
| `com.journeyapps:zxing-android-embedded` | 10 files | CameraX / ML Kit barcode, or `AndroidView` wrap | Compose / Phase 5 | Medium | Not started |
| `me.tatarka.bindingcollectionadapter2` + Data Binding | 26 layouts | `ViewModel` / `StateFlow` (retire binding) | Compose / cross-cutting | High | Not started |
| `com.getpebble:pebblekit` | 7 files | remove (dead watch) | Compose / Phase 5 | Low | Not started |

---

## 2. Dead / legacy AARs to retire

Local and unmaintained AARs that should not survive the migration.

| Dependency | Notes | Recommendation | Effort | Status |
| --- | --- | --- | --- | --- |
| `thread-safe-active-android` (ActiveAndroid ORM) | 41 files (`BgReading`, `Calibration`, …) | → Room (see §5) | **High** | In scope |
| `amazfitcommunication-master` AAR | companion device | review / remove if unused | Low | Not started |
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

## 5. ActiveAndroid → Room (in scope)

The largest single modernization item. ActiveAndroid is an abandoned ORM and is the
data backbone of the app (29 `@Table` classes, 64 files, ~478 query sites).

**Detailed plan:** [`ActiveAndroid_to_Room.md`](./ActiveAndroid_to_Room.md).

- **Status (2026-09-28):** underway. Room 2.8.5 + `db/AppDatabase` + background
  `LegacyDataImporter` (no user data lost) in place. Migrated 7 of 29 tables:
  `CalibrationRequest`, `ActiveBgAlert`, `PenData`, `AlertType`, `HeartRate`,
  `PebbleMovement` (StepCounter), `TransmitterData` — façades preserved, tests green,
  verified running on-device.

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

## Ownership conventions

- **Compose track** items land in the corresponding `Compose_Migration.md` phase.
- **Framework modernization** items are scheduled independently and can be picked up
  opportunistically (e.g. the three "Low effort" items in §3 are good first tasks).
- Mark items `In scope`, `Not started`, `In progress`, or `Done`.
