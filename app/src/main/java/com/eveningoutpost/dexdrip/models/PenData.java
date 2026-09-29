package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.PenDataDao;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.UUID;

@Entity(tableName = "PenData",
        indices = {
                @Index("pen_mac"),
                @Index("typ"),
                @Index(value = "timestamp", unique = true),
                @Index(value = {"pen_mac", "idx"}, unique = true),
                @Index(value = "uuid", unique = true)
        })
public class PenData {

    private static final String TAG = "PenData";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "pen_mac")
    public String mac;

    @Expose
    @ColumnInfo(name = "typ")
    public String type;

    @Expose
    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @Expose
    @ColumnInfo(name = "idx")
    public long index;

    @Expose
    @ColumnInfo(name = "created_timestamp")
    public long created_timestamp;

    @Expose
    @ColumnInfo(name = "units")
    public double units;

    @Expose
    @ColumnInfo(name = "temperature")
    public double temperature;

    @Expose
    @ColumnInfo(name = "battery")
    public int battery;

    @Expose
    @ColumnInfo(name = "flags")
    public long flags;

    @Expose
    @ColumnInfo(name = "bitmap")
    public long bitmap_flags;

    @Expose
    @ColumnInfo(name = "uuid")
    public String uuid;

    @Expose
    @ColumnInfo(name = "insulin_name")
    public String insulin_name;

    @Expose
    @ColumnInfo(name = "raw")
    public byte[] raw;

    public static PenData create(final String mac, final String type, final int index, final double units, final long timestamp, final double temperature, final byte[] raw) {

        // TODO baulk on very old records

        if (mac == null || type == null || index < 0 || units == -1 || timestamp < 0) {
            UserError.Log.wtf(TAG, "Invalid data sent to PenData.create() - skipping");
            return null;
        }

        // NOTE negative units indicates rewind
        final PenData penData = new PenData();
        penData.created_timestamp = JoH.tsl();
        penData.uuid = UUID.randomUUID().toString();
        penData.index = index;
        penData.units = units;
        penData.timestamp = timestamp;
        penData.temperature = temperature;
        penData.mac = mac;
        penData.type = type;
        penData.raw = raw;
        return penData;
    }

    public void save() {
        if (_id != 0) {
            dao().update(this);
        } else {
            _id = dao().insertIgnoringConflicts(this);
        }
    }

    public static long getHighestIndex(final String mac) {
        if (mac == null) return -1;
        final PenData penData = dao().highestIndex(mac);
        return penData != null ? penData.index : -1;
    }


    public static long getMissingIndex(final String mac) {
        if (mac == null) return -1;
        final List<PenData> list = dao().recentByMac(mac, JoH.tsl() - Constants.WEEK_IN_MS);
        long got = -1;
        for (final PenData pd : list) {
            if (got != -1 && pd.index != got - 1) {
                UserError.Log.d(TAG, "Tripped missing index on: " + got + " vs " + pd.index);
                return got - 1;
            }
            got = pd.index;
        }
        return -1;
    }

    public static List<PenData> getAllRecordsBetween(final long start, final long end) {
        return dao().allRecordsBetween(start, end);
    }


    public String brief() {
        return mac + " " + JoH.dateTimeText(timestamp) + " " + units + "U";
    }

    public String penName() {
        return type + " " + mac; // TODO have some way to name pen better
    }

    private static PenDataDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).penDataDao();
    }
}
