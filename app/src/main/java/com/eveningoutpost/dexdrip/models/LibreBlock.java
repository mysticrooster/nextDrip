package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.LibreBlockDao;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.UploaderQueue;
import com.eveningoutpost.dexdrip.utils.LibreTrendUtil;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by jamorham on 19/10/2017.
 */

@Entity(tableName = "LibreBlock",
        indices = {
                @Index("timestamp"),
                @Index("bytestart"),
                @Index("byteend"),
                @Index("reference"),
                @Index("uuid")
        })
public class LibreBlock {

    private static final String TAG = "LibreBlock";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @Expose
    @ColumnInfo(name = "bytestart")
    public long byte_start;

    @Expose
    @ColumnInfo(name = "byteend")
    public long byte_end;

    @Expose
    @ColumnInfo(name = "reference")
    public String reference;

    @Expose
    @ColumnInfo(name = "blockbytes")
    public byte[] blockbytes;

    @Expose
    @ColumnInfo(name = "calculatedbg")
    public double calculated_bg;

    @Expose
    @ColumnInfo(name = "uuid")
    public String uuid;

    @Expose
    @ColumnInfo(name = "patchUid")
    public byte[] patchUid;

    @Expose
    @ColumnInfo(name = "patchInfo")
    public byte[] patchInfo;

    // Fields to store battery value. Not persistent in the DB.

    // Only called by blucon with partial data.
    public static LibreBlock createAndSave(String reference, long timestamp, byte[] blocks, int byte_start) {
        return createAndSave(reference, timestamp, blocks, byte_start, false, null, null);
    }

    // if you are indexing by block then just * 8 to get byte start
    public static LibreBlock createAndSave(String reference, long timestamp, byte[] blocks, int byte_start, boolean allowUpload, byte[] patchUid, byte[] patchInfo) {
        final LibreBlock lb = create(reference, timestamp, blocks, byte_start, patchUid, patchInfo);
        if (lb != null) {
            lb.save();
            if (byte_start == 0 && blocks.length == Constants.LIBRE_1_2_FRAM_SIZE && allowUpload) {
                Log.d(TAG, "sending new item to queue");
                UploaderQueue.newTransmitterDataEntry("create", lb);
            }
        }
        return lb;
    }

    public static void Save(LibreBlock lb) {
        lb.save();
    }

    public static LibreBlock create(String reference, long timestamp, byte[] blocks, int byte_start, byte[] patchUid, byte[] patchInfo) {
        if (reference == null) {
            UserError.Log.e(TAG, "Cannot save block with null reference");
            return null;
        }
        if (blocks == null) {
            UserError.Log.e(TAG, "Cannot save block with null data");
            return null;
        }

        final LibreBlock lb = new LibreBlock();
        lb.reference = reference;
        lb.blockbytes = blocks;
        lb.byte_start = byte_start;
        lb.byte_end = byte_start + blocks.length;
        lb.timestamp = timestamp;
        lb.patchUid = patchUid;
        lb.patchInfo = patchInfo;
        lb.uuid = UUID.randomUUID().toString();
        return lb;
    }

    public static LibreBlock getLatestForTrend() {
        return getLatestForTrend(JoH.tsl() - Constants.DAY_IN_MS, JoH.tsl());
    }

    public static LibreBlock getLatestForTrend(long start_time, long end_time) {
        // Using the timestamp index directly (via Room) since the naive ActiveAndroid query
        // did not pick the index and could take several seconds.
        return dao().latestForTrend(start_time, end_time, Constants.LIBRE_1_2_FRAM_SIZE);
    }

    public static List<LibreBlock> getForTrend(long start_time, long end_time) {
        final List<LibreBlock> res1 = dao().forTrend(start_time, end_time);
        // One can think that we could do this filtering as part of the SQL. practically speaking
        // the wrong key was used for the query, and it takes 2-3 minutes.
        final List<LibreBlock> res = new ArrayList<LibreBlock>();
        for (LibreBlock lb : res1) {
            if (lb.byte_start == 0 && (lb.byte_end == Constants.LIBRE_1_2_FRAM_SIZE || lb.byte_end == 44)) {
                res.add(lb);
            }
        }
        return res;
    }

    public static LibreBlock getForTimestamp(long timestamp) {
        final long margin = (3 * 1000);
        return dao().forTimestamp(timestamp - margin, timestamp + margin);
    }

    public static void UpdateBgVal(long timestamp, double calculated_value) {
        Log.d(TAG, "UpdateBgVal called " + JoH.dateTimeText(timestamp) + " bgval = " + calculated_value);
        LibreBlock libreBlock = getForTimestamp(timestamp);
        if (libreBlock == null) {
            return;
        }
        Log.d(TAG, "Updating bg for timestamp " + JoH.dateTimeText(timestamp) + " bg = " + calculated_value);
        libreBlock.calculated_bg = calculated_value;
        libreBlock.save();
        LibreTrendUtil.getInstance().updateLastReading(libreBlock);
    }

    public static LibreBlock findByUuid(String uuid) {
        try {
            return dao().findByUuid(uuid);
        } catch (Exception e) {
            Log.e(TAG, "findByUuid() Got exception on Select : " + e.toString());
            return null;
        }
    }

    private static final boolean d = false;

    public String toJson() {
        return JoH.defaultGsonInstance().toJson(this);
    }

    public static LibreBlock createFromJson(String json) {
        if (json == null) {
            return null;
        }
        LibreBlock fresh;
        try {
            fresh = JoH.defaultGsonInstance().fromJson(json, LibreBlock.class);
        } catch (Exception e) {
            Log.e(TAG, "Got exception processing json msg: " + e);
            return null;
        }
        Log.e(TAG, "Successfuly created LibreBlock value " + json);
        return fresh;
    }

    class ExtendedLibreBlock {
        @Expose
        public int bridge_battery;
        @Expose
        public int Tomatobattery;
        @Expose
        public int Bubblebattery;
        @Expose
        public int Atombattery;
        @Expose
        public int nfc_sensor_age;
        @Expose
        public LibreBlock libreBlock;
    }

    public String toExtendedJson() {
        ExtendedLibreBlock elb = new ExtendedLibreBlock();
        elb.bridge_battery = Pref.getInt("bridge_battery", 0);
        elb.Tomatobattery = PersistentStore.getStringToInt("Tomatobattery", 0);
        elb.Bubblebattery = PersistentStore.getStringToInt("Bubblebattery", 0);
        elb.Atombattery = PersistentStore.getStringToInt("Atombattery", 0);
        elb.nfc_sensor_age = Pref.getInt("nfc_sensor_age", 0);
        elb.libreBlock = this;
        return JoH.defaultGsonInstance().toJson(elb);
    }

    // This also saves the batteries data to the global state.
    public static LibreBlock createFromExtendedJson(String json) {
        if (json == null) {
            return null;
        }
        ExtendedLibreBlock elb;
        try {
            elb = JoH.defaultGsonInstance().fromJson(json, ExtendedLibreBlock.class);
        } catch (Exception e) {
            Log.e(TAG, "Got exception processing json msg: " + e);
            return null;
        }
        Log.e(TAG, "Successfuly created LibreBlock value " + json);
        Pref.setInt("bridge_battery", elb.bridge_battery);
        PersistentStore.setString("Tomatobattery", Integer.toString(elb.Tomatobattery));
        PersistentStore.setString("Bubblebattery", Integer.toString(elb.Bubblebattery));
        PersistentStore.setString("Atombattery", Integer.toString(elb.Atombattery));
        Pref.setInt("nfc_sensor_age", elb.nfc_sensor_age);
        return elb.libreBlock;
    }

    public static LibreBlock byid(long id) {
        return dao().byid(id);
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

    private static LibreBlockDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).libreBlockDao();
    }
}
