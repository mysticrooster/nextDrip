package com.eveningoutpost.dexdrip.cgm.ilet.protocol

/**
 * The pump's RTC-to-Unix offset at the time the protocol was captured, used only to
 * build test vectors (and to assert [IletHistoricalGlucoseRecord.hasExpectedEpoch]).
 *
 * It lives in the test source set on purpose: production code must never use a fixed
 * offset. The live offset is derived per device, per cycle, from GetTime (0x2510) plus
 * the measured pump-vs-phone drift (see `IletDataProcessor.updateEpoch`), so it is
 * different on every pump.
 */
const val ILET_PUMP_EPOCH_REFERENCE: Long = 835_479_720L
