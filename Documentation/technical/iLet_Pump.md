# iLet (Beta Bionics) pump integration

Read-only integration with the Beta Bionics iLet insulin pump, ported from the verified
Python client at `WebProjects/ilet-testing` (see its `PROTOCOL.html`) and the Compose/Room
prototype. It can act as the primary glucose collector or as an independent pump data
source, and uploads to Nightscout, Nocturne and Tidepool to the extent each supports pump
data.

Beta Bionics' Mint patch pump (FDA-cleared September 2026, full US launch expected Q1 2027)
is **not** supported yet: the protocol items that *may* be Mint-specific are documented in
"Mint patch pump (unverified)" below but have never been exercised against Mint hardware,
and there is no in-repo evidence tying them to Mint — only that they are inert on the
sampled iLet firmware.

This worktree is plain NightscoutFoundation xDrip: **ActiveAndroid ORM** and legacy XML
`PreferenceScreen` settings. The protocol/cloud/BLE Kotlin is shared with the prototype and
is ported nearly verbatim; the Android-facing pieces were adapted (no Compose, no Room).

## Safety (must hold)

- **Read-only.** No bolus, no basal change, no therapy command. The following are
  deliberately not implemented: `CmdReset` (0x1316), `CmdResetBle` (0x3002), the
  firmware-update family, `CmdRecordRead` (0x1509), `CmdGetRtcTime` (0x160C),
  `CmdSetTime` (0x1511). The control-plane read commands `CmdPatchBackFillHistory`
  (0x1380) and `CmdGetControlSequenceNumberRanges` (0x1381) are not sent either: they
  are unsupported on the sampled iLet firmware and may be Mint patch-pump commands
  (see "Mint patch pump (unverified)").
- No dosing UI anywhere in the feature.
- Credentials are a medical-device key: stored encrypted, excluded from Android backup
  (`android:allowBackup="false"`), never logged.

## Layout

```
cgm/ilet/
  protocol/     pure JVM, no Android imports, unit-tested against the Python vectors
    IletConstants.kt  IletCrc.kt  IletCodec.kt  IletFrame.kt
    IletSecureChannel.kt  IletMessages.kt  IletSector.kt  IletClient.kt
  cloud/        IletCredentialStore, IletCognitoAuth, IletCloudSigner
  ble/          IletTransport (RxAndroidBle)
  ILetService.kt, ILetEntry.kt, IletDataProcessor.kt, IletMapping.kt,
  IletState.kt, ILetLoginActivity.kt, IletPrefs.kt, IletScanner.kt
models/PumpIobReading.java   ActiveAndroid table
```

## Settings placement (mainline XML)

The pump driver lives under **Insulin Pens and Pumps** (`pens_screen` in
`res/xml/xdrip_plus_prefs.xml`; only the `insulin_pens` string *value* was changed, so the
key and translations keep working). That section was moved from the Experimental category
to a top-level entry directly after **Smart Watch Features** in the Data Sync area
(`pref_advanced_settings.xml`); the Experimental category is gone. The `ilet_screen` sub-screen holds the read-only notice,
a master **switch**, the Bluetooth address, pump identity, a
**Test connection / live status** entry (`ILetStatusActivity`, which connects once and shows
device info + a real-time packet, and publishes the snapshot into `PumpStatus`/`IletState`.
When the background service is running it does not open a second BLE connection — it asks the
service for an immediate cycle and reports the fresh contact/status, since the pump allows
only one link), the account/login link, data-download toggles (**backfill glucose** — opt-in,
off by default — plus boluses and meal labels), the pump-IOB line toggle, a set of **iLet
extra-status-line options** (master switch plus run-suspend state, last-update age, **glucose
delta** and **time since the last reading**, wired into `StatusLine.extraStatusLineReal()` and
shown by `Home` whenever `status_line_ilet` is on and iLet is enabled, independently of the
general `extra_status_line`; delta/time come from `BestGlucose.getDisplayGlucose()`). Pump IOB / reservoir /
battery use xDrip's **built-in** pump status line (`status_line_pump_reservoir` block from
`PumpStatus`), which also renders when the iLet line is on — the iLet line no longer
duplicates those fields. The run/suspend item shows "INSULIN DELIVERY SUSPENDED" plus the
elapsed time only while suspended, and nothing while running. `Home` hides the extra status
line entirely when it would be empty (e.g. iLet disabled or no items selected), and "backfill
from date" (with the **account/login** link directly under the master switch).
The **Advanced options** sub-screen sits last and holds **Pair pump**, the Bluetooth address
and the non-working-on-this-firmware **Download basal** toggle, plus **Unpair pump** (removes
the Bluetooth bond via reflection `removeBond()`, behind a confirmation dialog). **Pair pump**
scans for the iLet (advertised service UUID, then a name containing "iLet"), remembers its
address and calls `BluetoothDevice.createBond()`, which shows the Android pairing dialog; the
transport also bonds on demand during connect. The credential actions (forget session
/ credential / everything) live only on the account/login page, not in settings. Pump
identity, the latest pump status and pump-clock drift are shown on the System Status screen,
not in settings. The enable switch summary reminds the user to pair the pump in Android
Bluetooth settings first. Handlers are
registered in `Preferences.setupIletPreferences()` **after**
`addPreferencesFromResource(R.xml.xdrip_plus_prefs)`.

iLet is also added to the Hardware Data Source list (`DexCollectionMethods` /
`DexCollectionMethodValues`, value `iLet`). When iLet is the selected collector the service
writes glucose; when another collector is selected but `ilet_enabled` is on, the service
runs **pump-only** and suppresses `BgReading` writes (`IletPrefs.isPumpOnly()` /
`downloadGlucose()`). The service is started from `CollectionServiceStarter.start()` and kept
alive from `MissedReadingService` via `ILetEntry.immortality()`; it self-stops when disabled.

## Schema

`PumpIobReading` is an ActiveAndroid `@Table` (`timestamp` unique, plus `iob`, `reservoir`,
`battery`). It is registered in the manifest `AA_MODELS` list and `AA_DB_VERSION` was bumped
**41 → 42** (there is no Room migration here; the model also self-creates its table via a
`fixUpTable()` guard, mirroring `HeartRate`).

## Protocol summary

- Credential = `app_uuid` + X25519 private key + 32-byte nonce + 64-byte cloud signature.
  Signing input is `pad_ascii(app_uuid, 64) || pubkey(32)` (96 bytes), base64'd into the
  `data=` query param of `GET https://us-apps.betabionicsapi.com/1/attest/sign`. The
  response is base64 of 160 bytes; take the **last 64**.
- Session key = HKDF-SHA256(shared, salt = *our* nonce, info = empty, len 32). MacTags are
  plain HMAC-SHA256 over `label || role_id || other_id || role_nonce || other_nonce`;
  `KC_2_V` is what the pump sends and we verify, `KC_2_U` is what we send.
- AES-GCM nonce = little-endian u32 counter + 8 zero bytes; the counter advances by
  **+2 encrypted / +1 cleartext** and is overwritten from the pump's `messageCounterU` in
  the confirm-key reply. The 4-byte header is the GCM AAD.
- Cleartext opcodes are only 0x3006/0x3007. Frame = `opcode(u16 LE) || length(u16 LE,
  includes the 4-byte header) || payload`.
- One CRC variant on all four layers: poly 0x04C11DB7, init 0xC704DD7B, no reflection,
  xorout 0.
- Transport: the pump drops the link after ~2.5 s idle; a ~50 ms floor is needed after a
  frame write before its OOB or the pump sees an OOB with no frame and drops the link. Body
  notifications (`A0090102`) accumulate into chunks; the OOB notification (`A0090103`)
  terminates a response. Chunk writes at MTU-3.
- Realtime (`A0090107`, 0x1506): 120-byte payload with a trailing CRC over `payload[0:-4]`.
  The service reads the characteristic directly (`readCharacteristic`) rather than relying on
  BLE notifications, which are not delivered on the sampled firmware — the connection test
  uses the same read.
- Unsupported on the sampled firmware (accepted then ignored or link-dropped): 0x2509,
  0x160C — and the 0x2508 partition family is not used because it reports the
  smaller flash partition. Their client methods and request builders were **removed**
  (`getRtcTime`, `flushRecords`/`recordBounds`/`readRecord`), so the
  code cannot even attempt them. The control-plane family 0x1380/0x1381/0x2380/0x2381 is
  inert on the sampled iLet firmware and may be Mint patch-pump specific
  (see "Mint patch pump (unverified)"): it is
  never sent, but its *response parsers* are kept so the layout is not lost
  (`IletPatchBackFillHistoryResponse`, `IletControlSequenceNumberRangesResponse`).
  Working primitives only: handshake, 0x2321 info, 0x2510 time,
  0x1506 realtime, 0x2334 single records, 0x2333 backfill sectors, 0x2335 bounds.
- Glucose reading/predicted are **i16 little-endian** at 0x2334 body offsets 49-50 / 51-52.
  Historical import requires only `reading_mg_dl > 0`; a zero reading is a sensor artifact,
  not hypo. The CGM "active" marker (body 57-60) is logged but is not a gate, because it is
  not set consistently on sector-sourced history. The body magic and checksum are verified at parse time, so the pump-epoch field is
  treated as a clock-skew *warning* (logged) rather than a hard gate — hard-gating on the
  GetTime-derived offset silently dropped every record when the two differed. The epoch
  warning compares against the *raw* GetTime offset, while timestamps use the
  timezone- and drift-corrected offset (below).
- Two incompatible record-type enums share numbers (`IletRecordType` for 0x2508/0x2509 vs
  `IletHistoricalRecordType` for 0x2333/0x2334/0x2335); mixing them returns nonsense.
- The RTC→Unix offset is derived from GetTime (0x2510), never hardcoded; a record whose
  embedded epoch does not match is logged and the record is still parsed (the body magic and
  checksum already prove the layout).
- **Clock drift**: after the timezone correction the service stores the residual pump-vs-phone
  drift (`ilet_clock_drift_seconds`), shows it in System Status and in the iLet settings
  ("Pump clock"), and **folds it into the timestamp offset** so every pump timestamp (glucose,
  insulin, basal, suspend, patch session, realtime) lands on the phone's true clock rather than
  the pump's own. When it exceeds two minutes the settings screen raises a one-off dialog
  (at most twice a day) asking the user to set the pump clock. `CmdSetTime` (0x1511) is **not**
  implemented: it is uninstrumented by the reference app with an undocumented payload, and this
  app is deliberately read-only, so it never writes the pump clock.
- The pump syncs its clock from the last phone that paired and may store that phone's *local*
  wall clock. `IletDataProcessor.updateEpoch` compares the pump's wall clock against the phone
  clock and, when treating it as UTC is clearly worse than treating it as local, subtracts the
  device UTC offset so timestamps are true UTC. This fixes a timezone-sized (e.g. 4 h) error
  without disturbing pumps that store true UTC. It then adds the residual clock drift
  (`clockDriftSeconds`, positive when the pump is behind) to the same offset, so a residual
  pump clock error — a pump that is simply set wrong — no longer shifts pump data by that
  error. Unit-tested in `IletEpochTest`.

## Data mapping

| iLet data | Source | xDrip target |
| --- | --- | --- |
| Glucose live | 0x1506 `glucose.readingMgDl` | `BgReading.bgReadingInsertFromIlet` (collector mode only) |
| Glucose history | 0x2334 / 0x2333 sectors | `BgReading` backfill |
| Bolus | `IletHistoricalInsulinRecord.totalDose1000` | `Treatments.createWithNotes` for `>0` rows |
| Meal | `mealType` / `mealSize` | No carbs; label `"<Type>, <Size>"` (e.g. `Dinner, More`) written as the bolus treatment's note, or a note-only treatment when the dose is zero |
| Basal | `IletAlgoStepRecord.nominal/instantBasalX10` | `APStatus.createEfficientRecord` |
| IOB / reservoir / battery | 0x1506 (`iob` f32 / 1000) | `PumpStatus` + `PumpIobReading` sample; re-read every 25 records during a backfill so the live value does not go stale |
| Suspend / charging / cartridge-empty / BG run | 0x1506 | `IletState` (PersistentStore) + Nightscout devicestatus |
| Pump identity | `IletDeviceInfoResponse` | `IletPrefs` (`ilet_device_serial/model`) |

- Boluses are de-duplicated with a deterministic UUID derived from the pump serial and record
  sequence (`serial:insulin:seq`). Zero-dose rows are periodic snapshots, not boluses, and
  are skipped; the realtime snapshot is **never** a bolus.
- Historical glucose is **opt-in** (`ilet_backfill_glucose`, off by default) and independent
  of the live collector, so it also works in pump-only mode. It only fills gaps: any row
  already within half the collector's sample period (from the primary CGM or a previous
  import) is skipped, so pump glucose never pads alongside live xDrip data. Real-time
  glucose is unaffected by the toggle and still follows the collector selection.
- Glucose is de-duplicated with a single query per record: `bgReadingInsertFromIlet` takes
  the dedupe window and returns `null` when a row already exists, so the realtime and history
  paths cannot disagree and the historical import no longer double-checks the timestamp. The
  window is `max(ILET_DEDUPE_WINDOW_MS, collectorSamplePeriod/2)`, i.e. at least
  `BgReading.ILET_DEDUPE_WINDOW_MS` (two minutes), so realtime and backfilled rows for the same
  pump CGM slot do not overlap, and a backfill that restarts from an earlier date is safe. It
  is **not** widened by the clock drift: the drift is already folded into the timestamps, and
  the old widening made freshly corrected rows collapse onto rows stamped under the previous
  offset, pinning the graph to the stale times. When the existing row is an iLet row whose time
  differs by more than a second, it is **re-stamped** to the corrected time (rows from another
  collector are left alone), so a clock-offset change converges as history is re-read instead
  of leaving old rows behind. Basal already collapses duplicates in
  `APStatus.createEfficientRecord` (same-value rows and out-of-order timestamps are rejected).
- Basal is written oldest→newest only (`APStatus.createEfficientRecord` rejects out-of-order
  timestamps); the walk resets its ordering guard per backfill pass.
- `PumpIobReading` feeds an optional graph line (`show_ilet_pump_iob_line`, **off by default**)
  with its own `color_ilet_pump_iob` colour, distinct from xDrip's treatment-derived IOB.
- The pump's predicted BG (`IletRealTimeData.glucose.predictedReading`, mg/dL) is **not
  persisted**: the latest value and the reading it came with are kept in prefs and the graph
  draws a single live straight stub from that reading to the prediction, gated by
  `show_ilet_predicted_bg_line` (**off by default**, colour `color_ilet_predicted_bg`). The
  stub is hidden once the value goes stale, so it only ever shows for the newest reading, not
  a history. The pump protocol does not document the forecast horizon, so the stub extends a
  fixed `PREDICTED_STUB_HORIZON_MS` (30 min) ahead.
- Pump identity is never hardcoded: the serial is derived from `GetDeviceInfo` (0x2321) on
  first connection, and the Bluetooth address is user-settable, falling back to a scan when
  left blank. Deterministic dedupe falls back to the address when the serial is unknown.

### Backfill

Bounds come from `CmdGetSequenceNumberHistory` (0x1335/0x2335) using `HistoricalRecordType`
numbering; the control-plane 0x1381 family is inert on the sampled firmware (and may be
Mint-specific — see below) and is never
used. On first run the look-back is bounded **by time** (`FIRST_RUN_LOOKBACK_MS`, 24 h) via a
binary search on record timestamps, not by a fixed record count — some record types (insulin
snapshots) are emitted densely, so a count cap only reached back a few hours. Because an
existing install's watermark is already set (and only ever moves forward), a **one-time deep
backfill floor** (`ilet_deep_backfill_floor`) rewinds all watermarks to 24 h ago on the first
cycle after install/upgrade, so upgrades also extend back a full day. It is paced by
`MAX_BACKFILL_PER_CYCLE` per type per service cycle, so a large first run catches up over
several cycles. Per-record parse failures are skipped and logged without stalling the watermark;
insulin/basal timestamps use the validated RTC + GetTime offset because the pump's internal
UTC field can be years stale.

The walk is **newest-first**. Each type keeps a high cursor (newest sequence seen), a low
cursor (oldest sequence of the processed range), and a history floor. New records above the
high cursor are read ascending (the pump needs that order for algorithm rows); the backlog
below the low cursor is read with the **batched** `0x2333` back-fill primitive — one request
streams a whole 4096-byte sector holding many glucose/insulin records
(`IletClient.readBackfillSectors` → `IletBackFillAssembler` → `IletSector.parseSector`),
rather than one `0x2334` round-trip per record. The pump answers one sector per request, so
the service issues several back-fill requests per cycle (`MAX_SECTOR_REQUESTS_PER_CYCLE`)
until the per-cycle record budget is spent. Anything that fails to parse (or returns no
sector) falls back to the single-record walk, so the cursor only advances past records
actually read and a mid-walk failure retries rather than skips. Algorithm/alarm (unsupported
here) keep the per-record path.

The sector path depends on the transport reassembling multi-frame streams correctly: body and
OOB arrive on two separate notification streams whose callbacks can race, so an OOB may land
before the trailing body chunk. `IletTransport` therefore holds an early OOB until the frame
reaches the length in its cleartext header (`isFrameComplete`), otherwise a truncated frame
is produced and AES-GCM fails.

Backfill progress is surfaced on the xDrip **System Status** screen as an "iLet Pump Status"
section (`ILetService.megaStatus()`), showing connection state, last contact, any pending
back-fill request, the device, and per-type floor/low/high plus the records processed in the
last pass (`IletStatus`). That makes a slow catch-up observable instead of looking stalled.

"Backfill from date" stores a target timestamp (`ilet_backfill_from`) and immediately wakes
the service (`ILetEntry.requestBackfill` → `ILetService.requestImmediateCycle`): a running
service is pulled out of its retry delay via a conflated wake-up channel, and a stopped
service is started. On the next cycle the service binary-searches each record type's
sequences (`0x2334` timestamps) for the first record at/after the target, sets it as the
history floor, and walks **forward** from there to the current sequence — filling from the
chosen date up to now. That forward gap is batched too, so a multi-day range completes in a
few short cycles (`MAX_BACKFILL_PER_CYCLE`). Existing dedupe (timestamp for glucose, UUID
for insulin) makes overlap safe, and the System Status section shows `high` advancing toward
the newest sequence. When the fill finally catches up to the newest sequence the service
shows a one-off "iLet backfill complete" toast (`notifyIfBackfillComplete`). If it makes no
progress for three consecutive cycles (no comparable type, or nothing processed) the request
is abandoned and the flag cleared, so a stale request cannot keep the flag set or keep
toasting; disabling iLet also clears it.

The service cycle is split by outcome: a healthy cycle idles for `IDLE_INTERVAL_MS` (60 s),
while a failed cycle retries after `RETRY_DELAY_MS` (15 s). A user request still cuts the
wait short via the conflated wake-up channel.

## Alarms and meal announcements

- **Meals** are carried in the historical insulin records (`mealType`/`mealSize`); the label
  is `"<Type>, <Size>"` (e.g. `Dinner, More`). It is written as the bolus treatment's note (or
  a note-only treatment when the dose is zero). Records with `mealType != 0` are logged
  (`iLet insulin record meal seq=... dose=... type=... size=...`) so the enum can be checked
  against real data. Labels are a best guess and always include the raw codes.
- **Alarms** are *not* available through the current read paths on the sampled firmware: the
  historical ALARM partition (`0x2334`/`0x2335`, record type 4) returns status 3 (empty), and
  the 0x1506 stream has no alert field (reference: "alarm ... empty on this firmware"). The
  infusion-set-expired notification is a pump-side annunciation; its likely source is the
  0x1515 superset's pump-patch session window.
- **0x1515** is parsed if the pump emits it on the real-time characteristic:
  `IletClient.readRealtimeSnapshot()` returns either the 0x1506 stream or the 0x1515 control
  superset, and the control path (`IletDataProcessor.processControl`) updates status, logs
  the meal-announcement state, and logs the patch/infusion-set session end (marking it
  `EXPIRED` when it is in the past). The 0x1515 packet is unsolicited — there is no request
  opcode — so if the pump only emits 0x1506 nothing is lost but no alarms/meals appear from
  that source.
- **Infusion set (pump patch) age** comes only from those 0x1515 fields
  (`pumpPatchInfoSessionStartTime`/`EndTime`); there is no iLet command to request them
  and the historical ALARM partition is empty on this firmware. When a 0x1515 packet is seen
  the converted window is stored (`ilet_patch_start_ms`/`ilet_patch_end_ms`) and shown as an
  "Infusion set" row (age + expired flag) in System Status. The field names are
  patch-pump vocabulary carried over from the shared Beta Bionics protocol — on the iLet
  they describe the infusion-set session, not a Mint disposable.

## Credential handling

- `IletCredentialStore` keeps everything in a Keystore-wrapped AES-256-GCM file under
  `filesDir`. `clearIdentity()` forgets the key material but keeps the app UUID the pump is
  bound to; `clearSession()` forgets the Cognito tokens; `clearAll()` wipes everything.
  The store decrypts on construction, so it is loaded off the main thread: the settings screen
  and the login page read the cheap `ilet_has_identity` mirror pref
  (`IletPrefs.hasIdentityPref`, kept in step by the store) instead of touching the store on the
  UI thread, and the login page warms the store on `Dispatchers.IO` before enabling its
  controls.
- `IletCognitoAuth` implements Cognito `USER_SRP_AUTH` (self-contained BigInteger + HMAC,
  no AWS SDK) with optional `SOFTWARE_TOKEN_MFA`; only the refresh/access/id tokens are
  persisted, never the password. This is the highest-risk component; it is unit-tested at
  the SRP math level and needs a manual login test against a real account.
- The app UUID is bound to the pump, so re-signing the *same* app UUID with a fresh key is
  rejected by the attest route. `ILetLoginActivity` therefore **only mints when no credential
  exists**; to re-enrol, forget the credential (or everything) first. The signer tolerates a
  raw base64 body, a JSON object carrying the blob (`signature`/`data`/…), and a JSON *string
  literal* in which base64 characters are `\uXXXX`-escaped (the live route returns e.g.
  `\u002B` for `+`), plus standard/URL-safe base64 and stripped padding. On failure it reports
  the content-type and body snippet rather than a bare "not base64". Decoding lives in
  `IletSignResponse` and is unit-tested (`IletCloudSignerResponseTest`).
- Rejected credentials are re-minted on an exponential backoff (15 min doubling, capped at
  6 h) so a persistent mismatch does not hit the cloud every service cycle.

## Cloud uploads

- **Nightscout**: additive `devicestatus` (`device: "iLet"`) with `pump{reservoir,battery,
  suspended,charging,cartridgeEmpty}` (`pump.battery` as a `{percent: N}` object, the shape
  Nightscout's pump plugin reads — a bare number is ignored and left the phone's
  `uploader.battery` as the only battery shown; `pump.status` as a
  `{status,suspended,bolusing}` object only while suspended, otherwise omitted since
  Nightscout defaults a missing status to normal), `openaps.iob.iob` and `uploader.battery`.
  Fingerprinted in `PersistentStore` (`last-ilet-ns-device-status`) so it only uploads on
  change; battery-only behaviour is unchanged when iLet is disabled.
- **Nocturne**: boluses/meals flow via `Treatments`. Its bolus/meal resources have no notes
  field, so a treatment note (e.g. an iLet meal label) is also sent as a Nocturne **Note**
  resource alongside the bolus/meal. Pump status (reservoir, battery %, IOB, suspended) is
  uploaded as a `PumpSnapshot` (fingerprint-deduped), an insulin suspension is sent as a
  `PumpSuspend`/`PumpResume` `DeviceEvent` on each transition, and the opt-in 0 U/hr profile
  is sent as a `BasalSchedule` plus a `TherapySettings` profile of the same name, then set as
  the active profile.
- **Tidepool**: glucose, boluses and `APStatus` basal already upload. The scheduled 0 U/hr
  basal is only added when there is no real basal data in the window, so it never overlaps
  real basal segments.
- **0 U/hr basal profile** (opt-in, `ilet_publish_zero_basal`): the iLet has no scheduled
  basal, so this tells Nightscout and Tidepool that basal is 0 U/hr, and Nocturne to make a
  0 U/hr profile active. Nightscout and Nocturne are one-shot; Tidepool is reconstructed per
  chunk.
  - **Nightscout**: a **new** profile named `iLet 0 U/hr` is created and set as
    `defaultProfile`, leaving the user's existing profile contents intact — the prior default
    entry is cloned so its therapy fields (carbratio/sens/target_low/target_high/units/dia/…)
    are preserved and only `basal` is overridden with a single `00:00 0 U/hr` entry; other
    `store` entries are preserved. The document is assembled by the pure, unit-tested
    `NightscoutUploader.buildZeroBasalProfileDocument`; the existing profile is read via
    `GET profile/current`, falling back to the first document from `GET profile`. With nothing
    readable, a basal-only entry is created (never fabricating zeros for the existing profile).
  - **Nocturne**: `publishIletZeroBasalSchedule` creates the `BasalSchedule`, ensures a
    `TherapySettings` profile of the same name exists (creating one only if
    `profileGetTherapySettingsByName` returns nothing), then calls `profileSetDefaultProfile`
    so the profile is actually active — a basal schedule alone is not. The app's OAuth token
    may lack profile-write scope; a 403 is logged once as non-fatal and not retried.
- **UploaderQueue caveat**: destinations are snapshotted at insert time, so pump rows written
  while a cloud destination is disabled are not retro-uploaded.

## Open questions / unverified

1. Meal **size** is confirmed (0 = More, 1 = Usual, 2 = Less). Meal **type** (1 = Breakfast,
   2 = Lunch, 3 = Dinner) is still a best guess. Raw codes are always stored in the note so a
   wrong guess is recoverable.
2. Whether the pump ever emits `0x1515` (CGM-session/silent-mode/fill-detect are out of scope).
3. Alarm-record enum values (`alertEnum`/`alertLevel`) — records are empty on the sampled
   firmware; mapping is a debug event-log line only.
4. Nocturne pump/device-status support in the bundled SDK.
5. Nightscout basal shape (`devicestatus.openaps` vs Temp-Basal treatments) and devicestatus
   rendering — validate against a local Nightscout.
6. On-device validation of login, live glucose, bolus/basal/IOB display, disconnect backfill,
   backfill-from-date, pump-only mode and each enabled cloud destination.

## Mint patch pump (unverified)

Mint (FDA-cleared September 2026, full US launch expected Q1 2027) is a patch pump on the
same protocol family, but nothing here has been exercised against Mint hardware. The
following *may* be Mint-specific — the honest position is that they are inert on the
sampled iLet firmware and carry patch-session semantics a durable pump has no obvious use
for; **there is no in-repo evidence tying them to Mint** and nothing is sent:

- `CmdPatchBackFillHistory` (0x1380) / `RspPatchBackFillHistory` (0x2380) — "patch"
  backfill. The response parser (`IletPatchBackFillHistoryResponse`, 10-byte header:
  status/recordType/fromSeq/toSeq + fixed-size records) is kept for the layout, but no
  request builder or client method exists and 0x1380 is never sent.
- `CmdGetControlSequenceNumberRanges` (0x1381) / `RspGetControlSequenceNumberRanges`
  (0x2381) — control-plane sequence bounds. Only the response parser
  (`IletControlSequenceNumberRangesResponse`: glucose/insulin/event/alarm/algoStep
  min/max) is kept; 0x1381 is never sent.
- `0x1515 pumpPatchInfoSessionStartTime/EndTime` — parsed today and used for the iLet's
  infusion-set age (above); on Mint the same window presumably describes the disposable
  patch session, but that mapping is unconfirmed. The names are protocol vocabulary that
  predates Mint in this codebase — on the iLet the fields describe the infusion-set
  session, not a Mint disposable. Per your note, on Mint hardware these fields should
  describe the disposable patch session; that still needs confirming against the device.

If Mint is added later: confirm the 0x1380/0x1381 request layouts and status codes
against Mint hardware, confirm whether the 0x1515 patch fields need different handling
per device model (the model string from 0x2321 `GetDeviceInfo` is the natural
discriminator), and extend the infusion-set row rather than forking the protocol layer.
