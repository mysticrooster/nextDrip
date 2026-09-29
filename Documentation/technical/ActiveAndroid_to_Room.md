# ActiveAndroid → Room Migration

## Overview

The data layer is being migrated from the abandoned **ActiveAndroid** ORM to
**Room** (AndroidX's modern persistence library). This is the largest single
modernization item in the project and is a **prerequisite** for the Home/chart
Compose work, which needs a clean, observable data source.

This document records *why*, *what's involved*, and *how* — so the work can be
planned, sequenced, and tracked independently of the Compose UI phases.

---

## Why

1. **ActiveAndroid is abandoned.** The last upstream release predates 2017; xDrip
   ships a private fork (`thread-safe-active-android-3.1.1` AAR) that is itself
   unmaintained.
2. **No observability.** ActiveAndroid's static, synchronous query API has no
   `Flow`/`LiveData`. Room exposes `Flow`, so the Home dashboard, charts, and
   stats can react to data changes instead of polling.
3. **Synchronous, scattered access.** ~478 query sites across 64 files use
   `new Select()` / `new Update()` / `new Delete()` / `.save()` inline. Room
   centralizes this in DAOs.
4. **It blocks clean Compose.** `Home.java`/`BgGraphBuilder` consume
   `BgReading`/`Calibration`/`Treatment` constantly; a Room + `Flow` layer makes
   the Compose slices dramatically simpler.
5. **It is a dead AAR** in `Tech_Debt.md` §2 — it must be retired for the
   dependency stack to be modernized.

---

## Current state (survey)

| Aspect | Value |
| --- | --- |
| ORM | `thread-safe-active-android-3.1.1` (local AAR) |
| Table (`@Table`) classes | **29** |
| Files using ActiveAndroid | **64** |
| Static query sites (`Select`/`Update`/`Delete`/`Insert`/`save`) | **~478** |
| Initialization | `com.activeandroid.content.ContentProvider` (manifest) + `ActiveAndroid.initialize()` in `JoH` |
| Database name | ActiveAndroid default (`Application.db`) — confirm at migration time |
| Serialization | Models are also Gson `@Expose`d (JSON ↔ DB model is entangled) |
| Foreign keys | `Model` fields with `onDelete = CASCADE` (e.g. `BgReading.sensor`, `.calibration`) |
| Room dependency | **Present** (`androidx.room:room-runtime`/`room-ktx`/`room-compiler` 2.8.5) |
| Migrated tables | **7** — `CalibrationRequest`, `ActiveBgAlert`, `PenData`, `AlertType`, `HeartRate`, `PebbleMovement`, `TransmitterData` |
| Existing tests | `CalibrationTest`, `TreatmentsTest`, `SensorTest`, … (parity baseline) |

### Package structure

Entities stay in `com.eveningoutpost.dexdrip.models` (they *are* the domain objects:
515 files import `models.*` and 915 direct public-field reads mean moving them would
force a mass rename plus a mapping layer — defeating façade-first). The new Room
plumbing lives in a separate `db` package:

```
com.eveningoutpost.dexdrip
├── models/                # unchanged package — entities + static façades
│   ├── CalibrationRequest.java   @Entity + façade   (migrated)
│   ├── ActiveBgAlert.java        @Entity + façade   (migrated)
│   └── …                         each migrates ActiveAndroid → Room in place
└── db/                    # new Room layer (permanent)
    ├── AppDatabase.java   @Database + singleton
    ├── LegacyDataImporter.java  one-time copy from Application.db
    ├── dao/               one DAO per migrated entity
    ├── Converters.java    @TypeConverter (as needed)
    └── Migrations.java    Migration objects (as needed)
```

### Model inventory

| Category | Classes |
| --- | --- |
| Core glucose data | `BgReading`, `Calibration`, `Sensor`, `Treatments`, `BloodTest` |
| Health / activity | `HeartRate`, `StepCounter`, `TransmitterData`, `ShareGlucose`, `PenData`, `Prediction`, `APStatus`, `DesertSync`, `Accuracy`, `UserError`, `UserNotification`, `LibreBlock`, `LibreData`, `Libre2RawValue`, `Libre2Sensor` |
| Alerts | `AlertType`, `ActiveBgAlert` |
| Devices | `ActiveBluetoothDevice` |
| Upload/sync queues | `BgSendQueue`, `CalibrationRequest`, `CalibrationSendQueue`, `SensorSendQueue`, `UploaderQueue` |
| Reminders | `Reminder` |

---

## How

### Guiding principles

1. **Façade-first.** Every model exposes public *static* accessor methods
   (`BgReading.last()`, `Calibration.latestValid(n)`, `BgReading.last30Minutes()`,
   …). Keep those signatures stable; only re-point their internals at the new
   DAO. Callers (64 files) do not change.
2. **Incremental, model-by-model.** Migrate one table at a time. Each model is a
   self-contained, individually committable slice with a parity test.
3. **Data-safe.** Match the existing ActiveAndroid schema (table/column names and
   types) exactly so Room opens the existing SQLite file without data loss. A
   destructive migration is **not** acceptable (medical data).
4. **Side-by-side during transition.** Room and ActiveAndroid coexist until the
   last table is migrated; each table moves over independently.

### Technical steps (per model)

1. **Entity conversion** — `@Table(name = "X", id = …)` → `@Entity(tableName = "X")`;
   `@Column(name, index, onDelete)` → `@ColumnInfo` + `@Index` + `@ForeignKey`;
   the `Model.id` becomes `@PrimaryKey`.
2. **DAO** — replace `new Select()…`, `new Update()`, `new Delete()`,
   `new Insert()`, `.save()` with `@Query` / `@Update` / `@Delete` / `@Insert`
   methods.
3. **Façade** — re-implement the model's static accessors on top of the DAO.
4. **Type converters** — `@TypeConverter` for `Enum`, serialized `List<Model>`,
   `Date`, etc. that ActiveAndroid serialized automatically.
5. **Gson `@Expose` coexistence** — Room entities can remain Gson-serializable
   (`@SerializedName`); confirm the JSON output is unchanged where it is used
   over the wire.
6. **Parity test** — assert the Room result matches the ActiveAndroid result for
   the same inputs before switching.
7. **Retire legacy schema shims.** Many models carry a manual `fixUpTable()` /
   `updateDB()` (raw `CREATE TABLE`/`ALTER TABLE` strings via `PlusModel.fixUpTable`
   or `SQLiteUtils.execSql`), invoked from `IdempotentMigrations.performAll()`. Room
   owns the schema now, so when a model moves: delete its `fixUpTable()`/`schema`
   array and remove its line from `IdempotentMigrations`. (`CalibrationRequest` had
   none, `ActiveBgAlert`'s was self-contained; `PenData`, `APStatus`, `Prediction`,
   `DesertSync`, `Libre*`, `AlertType`, `UserNotification` are wired into
   `IdempotentMigrations`.)
8. **Foreign keys block dependents.** Models with a `@Column` field typed as another
   model hold a real FK. The spine is `Sensor ← Calibration ← BgReading`, with
   `SensorSendQueue→Sensor`, `CalibrationSendQueue→Calibration`, `BgSendQueue→BgReading`.
   A dependent cannot move before its target (or must temporarily store the FK as a
   raw id column and load the object through the target's façade).
9. **Watch for raw SQL outside the façades.** Some callers bypass the model and hit
   the table directly via `Cache.openDatabase().rawQuery(...)` against ActiveAndroid's
   database (e.g. `StatsResult.getTotal_steps()` reads `PebbleMovement`). Those break
   when the table moves to Room and must be re-pointed at a DAO query.
10. **Table name ≠ class name.** Use the `@Table(name=…)` value, not the class name,
    in `MIGRATED_TABLES` (e.g. `StepCounter` → `PebbleMovement`).
11. **`instanceof Model` / `getId()` assumptions.** `UploaderQueue.newEntry(…, Model)`
    does `obj instanceof <ModelType>` and `obj.getId()` for several models. A migrated
    model is no longer a `Model`, so its `instanceof` branch must be removed (or the
    helper refactored to take `Object`). Callers that pass a migrated model as `Model`
    must be re-pointed too.
12. **Tests that clear tables.** Some tests reset state with
    `new Delete().from(<Model>.class).execute()` (ActiveAndroid). For migrated tables
    this no longer compiles — expose a `deleteAll()` façade (DAO-backed) and call that.

### Database & migration (the hard part)

- A `@Database` (`com.eveningoutpost.dexdrip.db.AppDatabase`) lists the migrated
  entities and is exposed as a process-wide singleton via
  `AppDatabase.getInstance(context)`; DAOs live in `com.eveningoutpost.dexdrip.db.dao`.
- **Transition strategy:** during the side-by-side period Room opens its **own**
  database file (`xdrip-room.db`), *not* ActiveAndroid's `Application.db`. This
  avoids Room trying to manage a file whose migrated tables already exist (created
  by ActiveAndroid) and whose other tables Room does not know about — opening the
  shared file would fail schema validation.
- **Data preservation (`LegacyDataImporter`).** Because Room starts empty, a
  one-time importer copies the legacy rows across the first time the app runs after
  a table has moved. It is kicked off from `IdempotentMigrations.performAll()`.
  - Mechanism: `ATTACH DATABASE <Application.db> AS legacy`, then one
    `INSERT OR IGNORE INTO main.T (<shared cols>) SELECT <shared cols> FROM legacy.T`
    per table, over the **intersection** of the two tables' columns. This is immune
    to column-order differences and to legacy-only/room-only columns, and is atomic
    per table (one statement). No per-row Java.
  - **Threading:** `importAll()` runs the copy on a dedicated background thread
    (`legacy-data-import`) so it never blocks app startup. `AppDatabase.getInstance()`
    first calls `LegacyDataImporter.awaitImportComplete()`, so any migrated façade is
    gated behind the copy and can never read — then race — a table mid-import. The
    await is bounded (30s) so a stuck copy cannot hang the app forever. The importer
    itself uses the non-gating `AppDatabase.buildInstance()` so it never waits on its
    own import.
  - Guards: copies only when the Room table is empty *and* the legacy table has rows;
    marks a per-table `PersistentStore` flag (`room_legacy_import_done_<table>`) so
    later deletions never re-import; `INSERT OR IGNORE` avoids PK clashes.
  - Adding an entity is a two-step chore: register it in `@Database` **and** add its
    table name to `LegacyDataImporter.MIGRATED_TABLES`. A unit test
    (`AppDatabaseImportListTest`) fails if the two drift apart.
  - After the last table has moved and ActiveAndroid is removed, the importer and its
    flags can be deleted (the copy has already happened on every device).
- Table/column names still match ActiveAndroid exactly (`@Entity(tableName = …)`,
  `@ColumnInfo(name = …)`), so the two schemas stay aligned and an eventual
  switch-over/copy is mechanical.
- Remove the ActiveAndroid `ContentProvider` and `initialize()` call only once
  the last table has moved.
- `@Database(exportSchema = false)` for now; schema export should be enabled when
  the first real `Migration` is needed.

### Threading

ActiveAndroid performs synchronous DB I/O across many threads. Room requires
explicit threading:

- New queries become `suspend`/`Flow` DAO methods (run on IO dispatchers).
- Legacy synchronous call sites (inside the façades) can temporarily use
  `allowMainThreadQueries` + `runBlocking`/background threads, then be migrated
  to `Flow` over time.

---

## Sequencing (suggested order)

1. ~~**Add Room** (runtime, ktx, compiler) + an empty `@Database` skeleton
   side-by-side with ActiveAndroid.~~ **Done.**
2. ~~**Pilot one leaf model**: establish the full entity → DAO → façade →
   data-safe migration → parity-test pattern.~~ **Done** — `CalibrationRequest`
   (chosen over `UploaderQueue`, whose manual raw-SQL `fixUpTable()` schema made
   it a poor first candidate).
3. ~~**Migrate the remaining queues**~~ **Re-ordered.** The queues have FKs into the
   core glucose models (`SensorSendQueue→Sensor`, `CalibrationSendQueue→Calibration`,
   `BgSendQueue→BgReading`) and `UploaderQueue` does class-based ActiveAndroid
   reflection (`getLegacyCount(X.class, …)`), so they cannot move first.
4. **Migrate the FK-free leaf models** (no `@Column` model reference, no external
   `X.class` use): `ActiveBgAlert` ✔, then `AlertType`, `PenData`, `Reminder`,
   `ShareGlucose`, `HeartRate`, `StepCounter`, `TransmitterData`, `UserError`,
   `ActiveBluetoothDevice` (2 external direct `Select`s to fold into the façade),
   `BloodTest`, `Treatments`, `Libre*`, `Accuracy`, `APStatus`, `DesertSync`,
   `Prediction`, `UserNotification`, `LibreBlock`, `LibreData`.
5. **Migrate the FK spine** `Sensor → Calibration → BgReading` — the ones Home/charts
   consume. **Requires the one-time data-copy from `Application.db` (see above).**
6. **Migrate the FK dependents** — `SensorSendQueue`, `CalibrationSendQueue`,
   `BgSendQueue`, `UploaderQueue` (the latter also needs `getLegacyCount` reflection
   removed).
7. **Remove ActiveAndroid** (AAR, ContentProvider, `initialize()`).

Each step is independently shippable; the app keeps working throughout because
unmigrated models still use ActiveAndroid and migrated models keep their façade.

### Progress log

- **2026-09-28 — Room foundation + `CalibrationRequest` pilot.**
  - Added `androidx.room:room-runtime/room-ktx/room-compiler:2.8.5` to
    `app/build.gradle` (Java `annotationProcessor`, not KSP/kapt).
  - Added `com.eveningoutpost.dexdrip.db.AppDatabase` (owns `xdrip-room.db`,
    `allowMainThreadQueries()` for the synchronous-façade transition) and
    `CalibrationRequestDao`.
  - Converted `CalibrationRequest` from `extends Model` (`@Table`/`@Column`) to a
    Room `@Entity`, re-pointing `createRange`/`createOffset`/`clearAll`/
    `shouldRequestCalibration` at the DAO. All external callers use those static
    methods, so no caller changes were needed.
  - Added `CalibrationRequestTest` (in-memory Room DB, 5 cases) — all green, and
    the full `testFastDebugUnitTest` suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — Structure settled; `ActiveBgAlert` migrated (2/29).**
  - Moved the plumbing `data/` → `db/` with DAOs under `db/dao/`; entities stay in
    `models/`. Added a `setInstanceForTesting()` hook to `AppDatabase`.
  - Converted `ActiveBgAlert` to a Room `@Entity` + `ActiveBgAlertDao` (`@Upsert`
    mirrors ActiveAndroid `save()`; `@Delete`). Dropped its private `fixUpTable()`
    (Room owns the schema). Added `ActiveBgAlertTest` (5 cases), all green.
  - Audit findings recorded above: the queue models are FK-blocked (sequencing
    re-ordered in step 3–6), and the legacy `fixUpTable()`/`updateDB()` shims wired
    into `IdempotentMigrations` must be removed per model.
- **2026-09-28 — Data preservation (`LegacyDataImporter`).**
  - Added `db/LegacyDataImporter`: one-time `ATTACH` + `INSERT ... SELECT` copy of
    migrated tables from `Application.db` into `xdrip-room.db`, guarded per table by
    a `PersistentStore` flag; invoked from `IdempotentMigrations.performAll()`.
  - Runs on a background thread; `AppDatabase.getInstance()` gates on
    `awaitImportComplete()` (bounded) so façades cannot race the copy.
  - Added `db/dao/MetaDao` + `AppDatabaseImportListTest` so a newly registered
    entity cannot be forgotten in `MIGRATED_TABLES`.
  - Added `LegacyDataImporterTest` (real SQLite via `@SQLiteMode(NATIVE)`: import,
    background+await, idempotency, no-overwrite, missing-legacy-db,
    legacy-only-column) — all green.
  - Full `testFastDebugUnitTest` + `assembleFastDebug` (R8) pass.
- **2026-09-28 — FK-free leaves, batch 1 (6/29 total).**
  - Migrated `PenData` (BLOB `byte[]`, unique indexes, external instance `save()`
    preserved), `AlertType` (persistent user config; dropped `fixUpTable`),
    `HeartRate` and `StepCounter` (table `PebbleMovement`; kept the instance
    `saveit()` used by Gson/watch callers).
  - Removed the now-dead `PenData.updateDB()` and `AlertType.fixUpTable()` calls
    from `IdempotentMigrations`.
  - Re-pointed the one raw-SQL caller of a migrated table
    (`StatsResult.getTotal_steps`) at a new `StepCounterDao.totalStepsBetween`.
  - Added tests `PenDataTest`, `AlertTypeTest`, `HeartRateTest`, `StepCounterTest`;
    full suite (888 tests) + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `TransmitterData` (7/29) + verified on-device.**
  - Migrated `TransmitterData` (indexed `timestamp`/`uuid`; kept an instance `save()`
    because `WatchUpdaterService` Gson-loads then saves it).
  - Removed the dead `instanceof TransmitterData` branch from
    `UploaderQueue.newEntry` (a migrated model is no longer a `Model`).
  - Added `TransmitterData.deleteAll()` and updated `BlueReaderTest`'s reset (it
    previously cleared the table with an ActiveAndroid `Delete`).
  - Added `TransmitterDataTest`. Full suite + `assembleFastDebug` (R8) pass, and the
    app has been run on a device with no crashes (migrated façades logging normally).

---

## Risks

- **Data loss** — the top risk. Mitigation: exact schema match + parity tests +
  a validated migration against a real device DB.
- **Threading regressions** — synchronous → coroutine shifts can introduce
  races/deadlocks. Mitigation: keep the façade's call surface synchronous first.
- **Foreign keys + `@Expose` entanglement** on the core models — requires care to
  keep JSON payloads unchanged.
- **Scope creep** — 29 tables is a lot; resist "cleaning up" the schema while
  migrating (schema changes should be a separate, deliberate migration).

---

## Effort & sequencing framework (for timelines)

- **Effort:** the largest single item in the modernization — multiple weeks,
  delivered as small per-model commits.
- **Track:** framework modernization, parallel to (and a prerequisite for the
  deep parts of) the Compose Home phase.
- **Metrics for planning:** 29 entities · 64 files · ~478 query sites. FK-free leaf
  models (~20) are Low/Medium effort; the FK spine (`Sensor`/`Calibration`/`BgReading`)
  is High-effort (data copy + FK + `@Expose`); the queue models are dependent on the
  spine, not the easy pilots originally assumed.
