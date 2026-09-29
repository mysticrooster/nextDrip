package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.APStatusDao;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.val;

/**
 * Created by jamorham on 11/06/2018.
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "APStatus",
        indices = {
                @Index(value = "timestamp", unique = true)
        })
public class APStatus {

    private final static String TAG = APStatus.class.getSimpleName();
    private final static boolean d = false;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @Expose
    @ColumnInfo(name = "basal_percent")
    public int basal_percent;

    @Expose
    @ColumnInfo(name = "basal_absolute")
    public double basal_absolute;

    public APStatus(long timestamp, int basal_percent, double basal_absolute) {
        this.timestamp = timestamp;
        this.basal_percent = basal_percent;
        this.basal_absolute = basal_absolute;
    }

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

    // static methods

    public static APStatus createEfficientRecord(long timestamp_ms, int basal_percent, double basal_absolute) {
        final APStatus existing = last();
        if (existing == null || (existing.basal_percent != basal_percent) || (existing.basal_absolute != basal_absolute)) {

            if (existing != null && existing.timestamp > timestamp_ms) {
                UserError.Log.e(TAG, "Refusing to create record older than current: " + JoH.dateTimeText(timestamp_ms) + " vs " + JoH.dateTimeText(existing.timestamp));
                return null;
            }

            final APStatus fresh = APStatus.builder()
                    .timestamp(timestamp_ms)
                    .basal_absolute(basal_absolute)
                    .basal_percent(basal_percent)
                    .build();

            UserError.Log.d(TAG, "New record created: " + fresh.toS());

            fresh.save();
            return fresh;
        } else {
            return existing;
        }

    }

    public static APStatus createEfficientRecord(long timestamp_ms, int basal_percent) {
        val basal_absolute = Profile.getBasalRateAbsoluteFromPercent(timestamp_ms, basal_percent);
        return createEfficientRecord(timestamp_ms, basal_percent,  basal_absolute);
    }

    public static APStatus createEfficientRecord(long timestamp_ms, double basal_absolute) {
        val basal_percent = Profile.getBasalRatePercentFromAbsolute(timestamp_ms, basal_absolute);
        return createEfficientRecord(timestamp_ms, basal_percent,  basal_absolute);
    }

    // TODO use persistent store?
    public static APStatus last() {
        return dao().last();
    }

    public static List<APStatus> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<APStatus> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<APStatus> latestForGraph(int number, long startTime, long endTime) {
        return latestForGraph(number, startTime, endTime, true);
    }

    public static List<APStatus> latestForGraph(int number, long startTime, long endTime, boolean extensionRecord) {
        final List<APStatus> results = dao().latestForGraph(Math.max(startTime, 0), endTime, number);

        if (extensionRecord) {
            // extend line to now if we have current data but it is continuation of last record
            // so not generating a new efficient record.
            if (results != null && (results.size() > 0)) {
                final APStatus last = results.get(results.size() - 1);
                final long last_raw_record_timestamp = ExternalStatusService.getLastStatusLineTime();
                // check are not already using the latest.
                if (last_raw_record_timestamp > last.timestamp) {
                    Double last_recorded_absolute = ExternalStatusService.getAbsoluteBRDouble();
                    final Integer last_recorded_tbr;
                    if (last_recorded_absolute == null) {
                        last_recorded_tbr = ExternalStatusService.getTBRInt();
                    } else {
                        last_recorded_tbr = Profile.getBasalRatePercentFromAbsolute(last_raw_record_timestamp, last_recorded_absolute);
                    }

                    if (last_recorded_tbr != null) {
                        if ((last.basal_percent == last_recorded_tbr)
                                && (JoH.msSince(last.timestamp) < Constants.HOUR_IN_MS * 3)
                                && (JoH.msSince(ExternalStatusService.getLastStatusLineTime()) < Constants.MINUTE_IN_MS * 20)) {
                            if (last_recorded_absolute == null) {
                                last_recorded_absolute = Profile.getBasalRateAbsoluteFromPercent(last_raw_record_timestamp, last_recorded_tbr);
                            }
                            results.add(new APStatus(JoH.tsl(), last_recorded_tbr, last_recorded_absolute));
                            UserError.Log.d(TAG, "Adding extension record");
                        }
                    }
                }
            }
        }
        return results;
    }


    public static List<APStatus> cleanup(int retention_days) {
        final int deleted = dao().cleanup(JoH.tsl() - (retention_days * 86400000L));
        UserError.Log.d(TAG, "APStatus cleanup removed " + deleted + " record(s)");
        return new ArrayList<>();
    }

    /**
     * Insert-or-update, mirroring the ActiveAndroid Model.save() used before the Room migration.
     */
    public Long save() {
        if (_id != 0) {
            dao().update(this);
        } else {
            final long id = dao().insert(this);
            if (id > 0) {
                _id = id;
            }
        }
        return _id;
    }

    private static APStatusDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).apStatusDao();
    }
}
