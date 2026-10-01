# iLet (Beta Bionics) pump integration

Read-only integration with the Beta Bionics iLet insulin pump, ported from the verified
Python client at `WebProjects/ilet-testing` (see its `PROTOCOL.html`). It can act as the
primary glucose collector or as an independent pump data source, and uploads to Nightscout,
Nocturne and Tidepool to the extent each supports pump data.

## Safety (must hold)

- **Read-only.** No bolus, no basal change, no therapy command. The following are
  deliberately not implemented: `CmdReset` (0x1316), `CmdResetBle` (0x3002), the
  firmware-update family, `CmdRecordRead` (0x1509), `CmdGetRtcTime` (0x160C),
  `CmdPatchBackFillHistory` (0x1380), `CmdGetControlSequenceNumberRanges` (0x1381) and
  `CmdSetTime` (0x1511).
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
  ILetLoginActivity.kt, IletPrefs.kt, IletScanner.kt
```

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
- Realtime (`A0090107`, 0x1506) streams as soon as the channel is up; 120-byte payload with
  a trailing CRC over `payload[0:-4]`.
- Unsupported on the sampled firmware (accepted then ignored): 0x2509, 0x160C, 0x1380,
  0x1381. Working primitives: handshake, 0x2321 info, 0x2510 time, 0x1506 realtime, 0x2334
  single records, 0x2333 backfill sectors, 0x2335 bounds, 0x2508 partition bounds.
- Glucose reading/predicted are **i16 little-endian** at 0x2334 body offsets 49-50 / 51-52.
  Accept only when `has_expected_epoch && cgm_active && reading_mg_dl > 0`; a zero reading is
  a sensor artifact, not hypo.
- Two incompatible record-type enums share numbers (`IletRecordType` for 0x2508/0x2509 vs
  `IletHistoricalRecordType` for 0x2333/0x2334/0x2335); mixing them returns nonsense.
- The RTC→Unix offset is derived from GetTime (0x2510), never hardcoded; a record whose
  embedded epoch does not match is skipped and logged.

## Data mapping

| iLet data | Source | xDrip target |
| --- | --- | --- |
| Glucose live | 0x1506 `glucose.reading_mg_dl` | `BgReading` (collector mode only) |
| Glucose history | 0x2334 / 0x2333 sectors | `BgReading` backfill |
| Bolus | `InsulinRecord.total_dose_1000` | `Treatments.create(0, units, ts, uuid)` |
| Basal | `AlgoStepRecord.nominal/instant_basal_x10` | `APStatus.createEfficientRecord` |
| IOB / reservoir / battery | 0x1506 | `PumpStatus` + `PumpIobReading` entity |
| Pump identity | `DeviceInfoResponse` | Prefs (`ilet_device_serial/model`) |

Boluses are de-duplicated with a deterministic UUID derived from the pump serial and record
sequence. Basal is written oldest→newest only (`APStatus.createEfficientRecord` rejects
out-of-order timestamps). `PumpIobReading` feeds an optional graph line
(`show_ilet_pump_iob_line`, **off by default**) distinct from xDrip's treatment-derived IOB.

Pump identity is never hardcoded: the serial is derived from `GetDeviceInfo` (0x2321) on
first connection, and the Bluetooth address is user-settable in the iLet settings screen,
falling back to a scan when left blank. When the serial is not yet known, deterministic
dedupe falls back to the Bluetooth address so it stays stable across sessions.

Backfill bounds come from `CmdGetSequenceNumberHistory` (0x1335/0x2335) using
`HistoricalRecordType` numbering; the control-plane 0x1381 family is unsupported on current
firmware and is never used. The first run is bounded to a recent look-back window rather
than the pump's whole retained history, per-record parse failures are skipped (and logged)
without stalling the watermark, and insulin/basal timestamps use the validated RTC +
GetTime offset because the pump's internal UTC field can be years stale. Rejected
credentials are re-minted on an exponential backoff so a persistent mismatch does not hit
the cloud every service cycle.

## Credential handling

- `IletCredentialStore` keeps everything in a Keystore-wrapped AES-256-GCM file under
  `filesDir`. `clearIdentity()` forgets the key material but keeps the app UUID the pump is
  bound to; `clearSession()` forgets the Cognito tokens; `clearAll()` wipes everything.
- `IletCognitoAuth` implements Cognito `USER_SRP_AUTH` (self-contained BigInteger + HMAC,
  no AWS SDK) with optional `SOFTWARE_TOKEN_MFA`; only the refresh/access/id tokens are
  persisted, never the password.

## Open questions

1. Insulin-record semantics: 0x2334 insulin rows may be periodic state snapshots rather than
   discrete boluses. Characterise on the real pump.
2. Meal mapping (`meal_type`/`meal_size` → carbs) is not yet characterised; the mapping hook
   returns 0 until known.
3. Basal availability: algorithm records may stay empty on current firmware; the `APStatus`
   path degrades to "unavailable" without error.
4. Nocturne pump/device-status support in the bundled SDK.
5. Nightscout basal shape (`devicestatus.openaps.enacted` vs Temp-Basal treatments).
