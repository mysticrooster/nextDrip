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

## Starting survey (pre-migration)

The values below record the state **before** the migration began. The migration is now
complete (see the Sequencing and Progress log below): ActiveAndroid is gone and all 28 tables
(plus `Libre2Sensors`) are on Room.

| Aspect | Value (at survey time) |
| --- | --- |
| ORM | `thread-safe-active-android-3.1.1` (local AAR) — **now retired** |
| Table (`@Table`) classes | 29 — **now 0** |
| Files using ActiveAndroid | 64 — **now 0** |
| Static query sites (`Select`/`Update`/`Delete`/`Insert`/`save`) | ~478 — **now 0** |
| Initialization | `com.activeandroid.content.ContentProvider` (manifest) + `ActiveAndroid.initialize()` in `JoH` — **removed** |
| Database name | **`DexDrip.db`** (manifest `AA_DB_NAME`), not ActiveAndroid's `Application.db` default |
| Serialization | Models are also Gson `@Expose`d (JSON ↔ DB model is entangled) |
| Foreign keys | `Model` fields with `onDelete = CASCADE` (e.g. `BgReading.sensor`, `.calibration`) — now transient objects + id columns |
| Room dependency | **Present** (`androidx.room:room-runtime`/`room-ktx`/`room-compiler` 2.8.5) |
| Migrated tables | **28 of 28** + `Libre2Sensors` as a `@DatabaseView` |
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
13. **Bump `@Database` version whenever the entity set changes.** Room persists a schema
    identity hash; adding/removing an entity **without** bumping the version makes Room
    throw `IllegalStateException: Room cannot verify the data integrity… forgot to update
    the version number` on the next open. This crashed on-device when a later build added
    entities to an existing `xdrip-room.db`. Because the Room DB is only ever a copy of the
    legacy data during the transition, `AppDatabase` uses
    `fallbackToDestructiveMigration(true)` plus an `onDestructiveMigration` callback that
    calls `LegacyDataImporter.clearImportFlags()`, so a recreated DB is re-filled from
    `Application.db` on the next import. As a belt-and-braces measure `AppDatabase.open()`
    also force-opens on first use and, if Room still refuses (same-version hash mismatch),
    deletes and rebuilds the file — so a schema change can never crash the app. (The
    released build will define the full schema as a single version, so real users never hit
    this.)

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
   - Source: the legacy database is **`DexDrip.db`** (the manifest's `AA_DB_NAME`), not
     ActiveAndroid's `Application.db` default.
   - Mechanism: `ATTACH DATABASE <DexDrip.db> AS legacy`, then one
     `INSERT OR IGNORE INTO main.T (<cols>) SELECT <exprs> FROM legacy.T WHERE _id NOT IN
     (SELECT _id FROM main.T)` per table, over the **intersection** of the two tables' columns.
     Immune to column-order differences and legacy-only/room-only columns; atomic per table.
   - **`NOT NULL` coercion:** Room makes primitive columns `NOT NULL` while the legacy schema is
     nullable (e.g. columns added later via `ALTER TABLE`). Any shared column that is `NOT NULL`
     in Room is copied through `COALESCE(col, 0)`, otherwise `INSERT OR IGNORE` would silently
     drop those rows (this caused old readings to go missing).
   - **Idempotent re-runs:** rows are only copied when their `_id` is absent, so the copy can be
     re-run to backfill rows without duplicating tables that lack a unique constraint.
   - **Import generation:** each table stores the `CURRENT_IMPORT_GENERATION` it was imported at.
     Bumping `CURRENT_IMPORT_GENERATION` re-runs the copy (idempotently) for every table — this is
     how a fix to the copy logic recovers rows a previous version dropped. Adding a new table to
     `MIGRATED_TABLES` imports it automatically (its stored generation starts at 0).
   - **Threading:** `importAll()` runs the copy on a dedicated background thread
     (`legacy-data-import`) so it never blocks app startup. `AppDatabase.getInstance()`
     first calls `LegacyDataImporter.awaitImportComplete()`, so any migrated façade is
     gated behind the copy and can never read — then race — a table mid-import. The
     await is bounded (30s) so a stuck copy cannot hang the app forever. Best-effort logging
     (`UserError`) uses the non-gating `AppDatabase.getInstanceWithoutImportWait()`.
   - Adding an entity is a two-step chore: register it in `@Database` **and** add its
     table name to `LegacyDataImporter.MIGRATED_TABLES`. A unit test
     (`AppDatabaseImportListTest`) fails if the two drift apart.
   - After ActiveAndroid is fully retired, the importer and its generations can be deleted
     (the copy has already happened on every device).
- Table/column names still match ActiveAndroid exactly (`@Entity(tableName = …)`,
  `@ColumnInfo(name = …)`), so the two schemas stay aligned and any later copy is mechanical.
- `@Database(exportSchema = true)`: the schema is exported to `app/schemas/` (via the
  `room.schemaLocation` annotation-processor argument). Every schema change bumps the version and
  adds a `Migration` to `db/Migrations.ALL`; Room validates the migrated schema against the export.
  The dev-only `fallbackToDestructiveMigration` + self-heal were removed so upgrades never wipe data.

### Threading

ActiveAndroid performs synchronous DB I/O across many threads. Room requires
explicit threading:

- New queries become `suspend`/`Flow` DAO methods (run on IO dispatchers).
- Legacy synchronous call sites (inside the façades) can temporarily use
  `allowMainThreadQueries` + `runBlocking`/background threads, then be migrated
  to `Flow` over time.

---

## Sequencing (actual order)

All steps are complete:

1. **Room foundation** — runtime/ktx/compiler + an empty `@Database` side-by-side. ✔
2. **Pilot** — `CalibrationRequest` (chosen over `UploaderQueue`, whose manual raw-SQL
   `fixUpTable()` schema made it a poor first candidate). ✔
3. **FK-free leaf models** — `ActiveBgAlert`, `AlertType`, `PenData`, `Reminder`,
   `ShareGlucose`, `HeartRate`, `StepCounter`, `TransmitterData`, `ActiveBluetoothDevice`,
   `BloodTest`, `Treatments`, `Libre*`, `Accuracy`, `APStatus`, `DesertSync`, `Prediction`,
   `UserNotification`, `LibreBlock`, `LibreData`. ✔
   (The queues could not go first: they FK into the core models and `UploaderQueue` did
   class-based ActiveAndroid reflection.)
4. **FK spine + queues** — `Sensor → Calibration → BgReading`, then `SensorSendQueue`,
   `CalibrationSendQueue`, `BgSendQueue`. ✔
5. **Last two** — `UploaderQueue`, `UserError`. ✔
6. **Retire ActiveAndroid** — AAR, ContentProvider, `initialize()`, `@Table`/`Model`. ✔
7. **Real migrations** — `exportSchema` + `Migrations` registry; destructive fallback removed. ✔

Each step shipped independently; the app kept working throughout because unmigrated models
stayed on ActiveAndroid and migrated models kept their façade.

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
- **2026-09-28 — `ActiveBluetoothDevice` (8/29).**
  - Migrated `ActiveBluetoothDevice`; kept an instance `save()` and added a `last()`
    façade so the two external direct `Select().from(ActiveBluetoothDevice.class)`
    callers (`BluetoothScan`, `ShareTest`) now use the façade.
  - Added `ActiveBluetoothDevice.deleteAll()` for `BlueReaderTest`'s reset.
  - Added `ActiveBluetoothDeviceTest`. Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `Reminder` (9/29).**
  - Migrated `Reminder` (persistent user data; dropped the manual `fixUpTable`
    schema). Kept instance `save()`/`delete()`/`getId()` because `Reminders` uses
    them. Split the `getNextActiveReminder` home-wifi `homeonly` branch into two DAO
    queries.
  - Added `ReminderTest`. Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `ShareGlucose`, `UserNotification` (11/29).**
  - `ShareGlucose`: Room @Entity (no DAO; the class has no external callers — the
    share-follow path uses a separate `ShareGlucoseRecord`). Excluded the transient
    `Context` field with `@Ignore`.
  - `UserNotification`: Room @Entity + DAO; table is `Notifications`. Kept instance
    `save()`/`delete()`; `GetNotificationByType` now switches over a fixed DAO set
    (Room cannot bind column names). Dropped the manual `updateDB` schema and its
    `IdempotentMigrations` call, and updated `UserNotificationTest` /
    `BgReadingPreferencesTest` resets to `deleteAll()`.
  - Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `Prediction`, `APStatus` (13/29).**
  - `Prediction`: Room @Entity + DAO (`create(...).save()` is chained, so the entity
    keeps an instance `save()`).
  - `APStatus`: Room @Entity + DAO; added an explicit 3-arg constructor because
    `@AllArgsConstructor` now includes `_id` and callers use
    `new APStatus(ts, percent, absolute)`.
  - Dropped the manual `updateDB` schemas and their `IdempotentMigrations` calls;
    removed `APStatus.updateDB()` from `UploadChunkTest`.
  - Added `PredictionTest`. Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `Accuracy`, `LibreData` (15/29).**
  - `Accuracy`: Room @Entity + DAO (the precise-timestamp query and the ordered graph
    query). Kept instance `save()`; dropped the manual schema.
  - `LibreData`: Room @Entity (no DAO — only `updateDB()` referenced it; that call is
    gone from `IdempotentMigrations`).
  - Added `AccuracyTest`. Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `Libre2RawValue`, `Libre2Sensor` (16 tables + 1 view).**
  - `Libre2RawValue`: Room @Entity, table `Libre2RawValue2` (columns `ts`/`serial`/
    `glucose`); kept instance `save()`.
  - `Libre2Sensor`: this was never a table — ActiveAndroid created it as a **VIEW**
    over `Libre2RawValue2`. Modelled with Room `@DatabaseView` (added to
    `@Database(views = …)`), so it is intentionally **not** in `MIGRATED_TABLES`
    (views are derived; there is nothing to copy).
  - Dropped both `updateDB()` calls from `IdempotentMigrations`.
  - Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — Fix on-device crash from Room schema identity mismatch.**
  - A build that added entities to an existing `xdrip-room.db` (same `version = 1`)
    crashed with `Room cannot verify the data integrity … forgot to update the version
    number`. Fixed by bumping `@Database` to `version = 2`, adding
    `fallbackToDestructiveMigration(true)`, and clearing the `LegacyDataImporter` flags
    from `onDestructiveMigration` so the recreated DB is re-imported from `Application.db`.
  - Added a regression test (`destructiveMigrationClearsImportFlagSoDataIsReimported`).
  - Made `AppDatabase.open()` self-healing: it force-opens on first use and, if Room still
    refuses (same-version identity-hash mismatch), deletes and rebuilds the file. Added
    `sameVersionSchemaMismatchSelfHeals` covering it.
- **2026-09-28 — `BloodTest` (17/29).**
  - `BloodTest`: Room @Entity + DAO (unique `uuid`/`timestamp`, state bitfield queries,
    precise-timestamp lookup). Kept instance `save()`/`saveit()` (`NightscoutTreatments`
    calls `saveit()`); `@Ignore` on the transient `glucoseReadingRx`.
  - Generalised `UploaderQueue.newEntry`/`newEntryForWatch` to take `Object` and added
    `referenceId(Object)` (ActiveAndroid `getId()`, else the public Room `_id` field) —
    `BloodTest` (and later `Treatments`) are no longer `Model`s.
  - Bumped `@Database` to `version = 3`.
  - Added `BloodTestTest`. Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `Treatments` (18/29).**
  - `Treatments`: Room @Entity + DAO (all the ordered/precise-timestamp/like queries,
    `deleteAll`, `cleanup`). Kept instance `save()`/`delete()` (external callers in
    `SaveCompleted`, `PendiqService`); `@Ignore` the transient `insulinInjections`.
  - Re-pointed the raw-SQL stats queries (`StatsResult.getTotal_carbs`/`getTotal_insulin`
    read `treatments`) at new `TreatmentsDao.sumCarbs`/`sumInsulin`.
  - Bumped `@Database` to `version = 4`.
  - Added `TreatmentsTest`. Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `LibreBlock` (19/29).**
  - `LibreBlock`: Room @Entity + DAO. The trend query used a hand-written
    `INDEXED BY` raw query; re-expressed as a normal Room query (the timestamp index is
    still used). Kept instance `save()`; dropped `updateDB`/`getFromCursor`.
  - Bumped `@Database` to `version = 5`.
  - Added `LibreBlockTest`. Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `DesertSync` (20/29).**
  - `DesertSync`: Room @Entity + DAO. Its `@Builder` private constructor hid the
    Lombok no-arg constructor from Room, so an explicit public no-arg constructor was
    added; the `processed` column had to become `public` (Room's generated DAO is in a
    different package). Kept instance `save()`; dropped `updateDB`.
  - Bumped `@Database` to `version = 6`.
  - Added `DesertSyncTest`. Full suite + `assembleFastDebug` (R8) pass.
- **2026-09-28 — FK spine + queues (26/28 tables).**
  - Migrated `Sensor`, `Calibration`, `BgReading` and the three `*SendQueue` classes.
  - **FK pattern (option A):** the object fields (`sensor`, `calibration`, queues' `sensor`/
    `calibration`/`bgReading`) are now `@Ignore` transients; the persisted id lives in a
    `*_id` field mapped to the original column name. `save()` syncs the id from the object,
    so existing write sites are unchanged. `BgReading`/`Calibration` gained `getId()`/`delete()`
    shims. `BgReading.calibration` is transient, so `BgReading.getCalibration()` lazily loads
    it from `calibration_id` and the handful of read sites (`BgReading`, `BloodTest`,
    `WatchUpdaterService`) use it.
  - Re-pointed raw-SQL readers of migrated tables: `StatsResult` and `DBSearchUtil` (bgreadings
    stats) now use `BgReadingDao`; `UploaderQueue`'s `getLegacyCount` for the queue classes was
    replaced with DAO counts.
  - **Pitfall:** Room makes primitive `double` columns `NOT NULL`, but SQLite stores `NaN` as
    `NULL`, so `Calibration`'s "invalid slope" path crashed — coerce non-finite values to 0
    before saving (equivalent filtering, since `lastValid()` excludes `slope == 0` too).
  - Bumped `@Database` to `version = 7`.
  - Full suite (921 tests) + `assembleFastDebug` (R8) pass.
- **2026-09-28 — `UploaderQueue`, `UserError` (28/28); ActiveAndroid retired.**
  - `UploaderQueue`: Room @Entity + DAO (bitfield queries in SQL, distinct types, cleanup);
    dropped the hand-written schema/raw-SQL counts.
  - `UserError` (`UserErrors`): Room @Entity + DAO (`severity IN (:levels)`). Its writes are
    **best-effort and non-gating** (`AppDatabase.getInstanceWithoutImportWait`) so logging can
    never block or crash on the legacy import.
  - With no `@Table` classes left, ActiveAndroid is retired: removed the AAR dependency, the
    `initialize()`/`clearCache()` calls, the manifest ContentProvider + `AA_*` meta-data, and the
    dead `PlusModel`/`SqliteRejigger`. `JoH.fullDatabaseReset()` now reopens the Room DB.
  - **Legacy DB name fixed:** it is `DexDrip.db` (`AA_DB_NAME`), not ActiveAndroid's
    `Application.db` default. This was a real data-loss bug — the importer never found the file.
  - **Importer hardened** (see below): `COALESCE` for NOT-NULL columns + `_id`-based idempotency +
    a per-table import generation (`CURRENT_IMPORT_GENERATION = 2`) that re-runs the copy to
    recover rows an earlier version dropped. This backfills the missing old readings.
  - **Backup** (`Backup.doCompleteBackup`) and DB size now include `xdrip-room.db`; note
    `DatabaseUtil.saveSql/loadSql` still handle only the legacy DB (advanced raw-DB tool).
  - Re-pointed the remaining raw-SQL callers at DAOs: `NoteSearch`, `tables/SensorDataTable`,
    `DatabaseAdmin`, `DatabaseUtil.saveCSV`.
  - Bumped `@Database` to `version = 9`. Full suite + `assembleFastDebug` (R8) pass.

> ⚠️ **One-time dev caveat:** upgrading from an intermediate build to `version = 9` recreates
> `xdrip-room.db` (schema change) and re-imports from `DexDrip.db`. Rows written to Room *after* a
> table had already moved (e.g. BgReadings collected while running the spine build) are not in the
> legacy DB and are not recovered by this path. A production release should ship a single
> `@Database` version + a real `Migration` so no wipe is needed.
- **2026-09-28 — second pass: real migration infrastructure + cleanup.**
  - `exportSchema = true` with `room.schemaLocation`; baseline schema exported to
    `app/schemas/…/9.json`. Added `db/Migrations.ALL` (empty registry; the pattern documented).
  - Removed `fallbackToDestructiveMigration` and the identity-mismatch self-heal: upgrades now
    require a real `Migration` and never wipe user data. Removed the corresponding tests.
  - `DatabaseUtil.saveSql` now zips **both** `DexDrip.db` and `xdrip-room.db`; `loadSql` detects
    the target DB (via `room_master_table`) and, when a legacy DB is imported, clears the import
    generation so Room is re-filled on the next launch. `getDataBaseSizeInBytes` sums both.
  - Fixed `ImportDatabaseActivity.getDBVersion()` (it read the now-removed `AA_DB_VERSION`).
  - Tidied stale ActiveAndroid comments/PowerMock ignores.
  - Still deferred (documented): `allowMainThreadQueries` → `Flow`/executors, and real
    `@ForeignKey`/`@Relation` (the transient-object + `getCalibration()` pattern stays for now).

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
