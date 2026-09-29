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
| Room dependency | **Not present yet** |
| Existing tests | `CalibrationTest`, `TreatmentsTest`, `SensorTest`, … (parity baseline) |

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

### Database & migration (the hard part)

- Create a `@Database` listing the migrated entities.
- Initialize via `Room.databaseBuilder(context, …)` with the **same database
  file** ActiveAndroid used.
- Because Room and ActiveAndroid use the same table/column names, the schema can
  match **exactly**, allowing a no-op migration (same `version`) or a careful
  `Migration` for the switch-over. This must be validated against a real user DB
  before release.
- Remove the ActiveAndroid `ContentProvider` and `initialize()` call only once
  the last table has moved.

### Threading

ActiveAndroid performs synchronous DB I/O across many threads. Room requires
explicit threading:

- New queries become `suspend`/`Flow` DAO methods (run on IO dispatchers).
- Legacy synchronous call sites (inside the façades) can temporarily use
  `allowMainThreadQueries` + `runBlocking`/background threads, then be migrated
  to `Flow` over time.

---

## Sequencing (suggested order)

1. **Add Room** (runtime, ktx, compiler) + an empty `@Database` skeleton
   side-by-side with ActiveAndroid.
2. **Pilot one leaf model** — `UploaderQueue` (or `BgSendQueue`): no foreign
   keys, few dependents. Establish the full entity → DAO → façade → data-safe
   migration → parity-test pattern.
3. **Migrate the remaining queues** (the other `*SendQueue`/`*Request` classes).
4. **Migrate core glucose data** (`BgReading`, `Calibration`, `Sensor`,
   `Treatments`, `BloodTest`) — the ones Home/charts consume.
5. **Migrate the rest** (health/activity, alerts, devices, reminders).
6. **Remove ActiveAndroid** (AAR, ContentProvider, `initialize()`).

Each step is independently shippable; the app keeps working throughout because
unmigrated models still use ActiveAndroid and migrated models keep their façade.

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
- **Metrics for planning:** 29 entities · 64 files · ~478 query sites; the queue
  models (~5) are Low-effort pilots, the core models (~5) are High-effort, the
  remaining ~19 are Low/Medium.
