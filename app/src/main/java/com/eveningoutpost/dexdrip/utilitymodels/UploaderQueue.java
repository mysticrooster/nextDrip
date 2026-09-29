package com.eveningoutpost.dexdrip.utilitymodels;

import android.util.Log;
import android.util.LongSparseArray;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.UploaderQueueDao;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.tidepool.TidepoolEntry;
import com.eveningoutpost.dexdrip.tidepool.TidepoolStatus;
import com.eveningoutpost.dexdrip.tidepool.TidepoolUploader;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.services.SyncService.startSyncService;

/**
 * Created by jamorham on 15/11/2016.
 */

@Entity(tableName = "UploaderQueue",
        indices = {
                @Index("action"),
                @Index("otype"),
                @Index("timestamp"),
                @Index("bitfield_complete"),
                @Index("bitfield_wanted")
        })
public class UploaderQueue {
    private static final boolean d = false;
    private final static String TAG = "UploaderQueue";

    // table creation
    private static long last_cleanup = 0;
    private static long last_new_entry = 0;
    private static long last_query = 0;

    // mega status cache
    private static ArrayList<String> processedBaseURIs;
    private static ArrayList<String> processedBaseURInames;


    // Bitfields
    public static final long MONGO_DIRECT = 1;
    public static final long NIGHTSCOUT_RESTAPI = 1 << 1;
    public static final long TEST_OUTPUT_PLUGIN = 1 << 2;
    public static final long INFLUXDB_RESTAPI = 1 << 3;
    public static final long WATCH_WEARAPI = 1 << 4;
    public static final long NOCTURNE_RESTAPI = 1 << 5;    // 32


    public static final long DEFAULT_UPLOAD_CIRCUITS = 0;

    private static final LongSparseArray circuits_for_stats = new LongSparseArray<String>() {
        {
            append(MONGO_DIRECT, "Mongo Direct");
            append(NIGHTSCOUT_RESTAPI, "Nightscout REST");
            append(TEST_OUTPUT_PLUGIN, "Test Plugin");
            append(INFLUXDB_RESTAPI, "InfluxDB REST");
            append(WATCH_WEARAPI, "Watch Wear API");
            append(NOCTURNE_RESTAPI, "Nocturne REST");
        }
    };


    //...


    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @Expose
    @ColumnInfo(name = "action")
    public String action;

    @Expose
    @ColumnInfo(name = "otype")
    public String type;

    @Expose
    @ColumnInfo(name = "reference_id")
    public long reference_id;

    @Expose
    @ColumnInfo(name = "reference_uuid")
    public String reference_uuid;

    @Expose
    @ColumnInfo(name = "bitfield_wanted")
    public long bitfield_wanted;

    @Expose
    @ColumnInfo(name = "bitfield_complete")
    public long bitfield_complete;

    //////////////////////////////////////////

    // patches and saves
    public Long saveit() {
        return save();
    }

    public Long completed(long bitfield) {
        UserError.Log.d(TAG, "Marking bitfield " + bitfield + " completed on: " + getId() + " / " + action + " " + type + " " + reference_id);
        bitfield_complete |= bitfield;
        return saveit();
    }

    public String toS() {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }

    //////////////////////////////////////////

    public static UploaderQueue newEntry(String action, Object obj) {
        UserError.Log.d(TAG, "new entry called");
        final UploaderQueue result = new UploaderQueue();
        result.bitfield_wanted = DEFAULT_UPLOAD_CIRCUITS
                | (Pref.getBooleanDefaultFalse("cloud_storage_mongodb_enable") ? MONGO_DIRECT : 0)
                | (Pref.getBooleanDefaultFalse("cloud_storage_api_enable") ? NIGHTSCOUT_RESTAPI : 0)
                | (Pref.getBooleanDefaultFalse("cloud_storage_influxdb_enable") ? INFLUXDB_RESTAPI : 0)
                | (Pref.getBooleanDefaultFalse("wear_sync") ? WATCH_WEARAPI : 0)
                | (Pref.getBooleanDefaultFalse("nocturne_upload_enable") ? NOCTURNE_RESTAPI : 0);
        if (result.bitfield_wanted == 0) return null; // no queue required
        result.timestamp = JoH.tsl();
        result.reference_id = referenceId(obj);
        // TODO this probably could be neater
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof BgReading ? ((BgReading) obj).uuid : null;
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof Treatments ? ((Treatments) obj).uuid : null;
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof Calibration ? ((Calibration) obj).uuid : null;
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof BloodTest ? ((BloodTest) obj).uuid : null;
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof LibreBlock ? ((LibreBlock) obj).uuid : null;

        if (result.reference_uuid == null) {
            Log.d(TAG, "reference_uuid was null so refusing to create new entry");
            return null;
        }

        if (result.reference_id < 0) {
            UserError.Log.wtf(TAG, "ERROR ref id was: " + result.reference_id + " for uuid: " + result.reference_uuid + " refusing to create");
            return null;
        }

        result.action = action;

        result.bitfield_complete = 0;
        result.type = obj.getClass().getSimpleName();
        result.saveit();
        if (d) UserError.Log.d(TAG, result.toS());
        last_new_entry = JoH.tsl();
        return result;
    }

    public static void newTransmitterDataEntry(String action, Object obj) {
    	if(!Pref.getBooleanDefaultFalse("mongo_load_transmitter_data")) {
    		return;
    	}
    	newEntry(action, obj);
    	// For libre us sensors, we have a reading, it might not create a BG entry, but we still need
    	// to upload it.
    	startSyncService(3000); // sync in 3 seconds
    }
    
    // TODO remove duplicated functionality, replace with generic multi-purpose method
    public static UploaderQueue newEntryForWatch(String action, Object obj) {
        UserError.Log.d(TAG, "new entry called for watch");
        final UploaderQueue result = new UploaderQueue();
        result.bitfield_wanted = DEFAULT_UPLOAD_CIRCUITS
                | (Pref.getBooleanDefaultFalse("wear_sync") ? WATCH_WEARAPI : 0);
        if (result.bitfield_wanted == 0) return null; // no queue required
        result.timestamp = JoH.tsl();
        result.reference_id = referenceId(obj);
        // TODO this probably could be neater
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof BgReading ? ((BgReading) obj).uuid : null;
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof Treatments ? ((Treatments) obj).uuid : null;
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof Calibration ? ((Calibration) obj).uuid : null;
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof BloodTest ? ((BloodTest) obj).uuid : null;

        if (result.reference_uuid == null) {
            Log.d(TAG, "reference_uuid was null so refusing to create new entry");
            return null;
        }

        if (result.reference_id < 0) {
            UserError.Log.wtf(TAG, "Watch ERROR ref id was: " + result.reference_id + " for uuid: " + result.reference_uuid + " refusing to create");
            return null;
        }

        result.action = action;

        result.bitfield_complete = 0;
        result.type = obj.getClass().getSimpleName();
        result.saveit();
        if (d) UserError.Log.d(TAG, result.toS());
        last_new_entry = JoH.tsl();
        return result;
    }

    public static List<UploaderQueue> getPendingbyType(String className, long bitfield) {
        return getPendingbyType(className, bitfield, 300);
    }

    public static List<UploaderQueue> getPendingbyType(String className, long bitfield, int limit) {
        if (d) UserError.Log.d(TAG, "get Pending by type: " + className);
        last_query = JoH.tsl();
        try {
            return dao().getPendingByType(className, bitfield, limit);
        } catch (android.database.sqlite.SQLiteException e) {
            if (d) UserError.Log.d(TAG, "Exception: " + e.toString());
            return new ArrayList<UploaderQueue>();
        }
    }


    /**
     * Primary key of an object being queued. All data models expose their key as a public
     * long _id field.
     */
    private static long referenceId(Object obj) {
        try {
            return obj.getClass().getField("_id").getLong(obj);
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Unable to determine reference id for " + obj.getClass().getSimpleName());
            return -1;
        }
    }


    private static List<String> getClasses() {
        return dao().distinctTypes();
    }


    public static int getQueueSizeByType(String className, long bitfield, boolean completed) {
        if (d) UserError.Log.d(TAG, "get Pending count by type: " + className);
        try {
            return completed
                    ? dao().countCompletedByType(className, bitfield)
                    : dao().countPendingByType(className, bitfield);
        } catch (android.database.sqlite.SQLiteException e) {
            if (d) UserError.Log.d(TAG, "Exception: " + e.toString());
            return 0;
        }
    }


    public static void emptyQueue() {
        try {
            dao().deleteAll();
            last_cleanup = JoH.tsl();
            JoH.static_toast_long("Uploader queue emptied!");
        } catch (Exception e) {
            UserError.Log.d(TAG, "Exception cleaning uploader queue: " + e);
        }
    }


    public static void cleanQueue() {
        // delete all completed records > 24 hours old
        try {
            dao().deleteCompletedOlderThan(JoH.tsl() - 86400000L);

            // delete everything > 7 days old
            dao().deleteOlderThan(JoH.tsl() - 86400000L * 7L);
        } catch (Exception e) {
            UserError.Log.d(TAG, "Exception cleaning uploader queue: " + e);
        }
        last_cleanup = JoH.tsl();
    }


    public static String getCircuitName(long i) {
        try {
            return circuits_for_stats.get(i).toString();
        } catch (Exception e) {
            return "Unknown Circuit";
        }
    }

    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();
        // per circuit
        for (int i = 0, size = circuits_for_stats.size(); i < size; i++) {
            final long bitfield = circuits_for_stats.keyAt(i);

            // per class of data
            for (String type : getClasses()) {
                if (JoH.quietratelimit("uploader-stats", 10)) {
                    Log.d(TAG, "Getting stats for class: " + type + " in " + circuits_for_stats.valueAt(i));
                }
                int count_pending = getQueueSizeByType(type, bitfield, false);
                int count_completed = getQueueSizeByType(type, bitfield, true);
                int count_total = count_pending + count_completed;

                if (count_total > 0) {
                    l.add(new StatusItem(circuits_for_stats.valueAt(i).toString(), count_pending + " " + type, (count_pending > 1000) ? StatusItem.Highlight.BAD : StatusItem.Highlight.NORMAL));
                }
            }

               /*
                // handle legacy tables
                if (bitfield == MONGO_DIRECT) {
                    // legacy
                    l.add(new StatusItem(circuits_for_stats.valueAt(i).toString(), BgSendQueue.countByMongoSuccess(false) + " Legacy Glucose Values"));
                    l.add(new StatusItem(circuits_for_stats.valueAt(i).toString(), CalibrationSendQueue.countByMongoSuccess(false) + " Legacy Calibrations"));
                }
                // handle legacy tables
                if (bitfield == NIGHTSCOUT_RESTAPI) {
                    // legacy
                    l.add(new StatusItem(circuits_for_stats.valueAt(i).toString(), BgSendQueue.countBySuccess(false) + " Legacy Glucose Values"));
                    l.add(new StatusItem(circuits_for_stats.valueAt(i).toString(), CalibrationSendQueue.countBySuccess(false) + " Legacy Calibrations"));
                }*/
        }

        if (UploaderTask.exception != null) {
            l.add(new StatusItem("Exception", UploaderTask.exception.toString(), StatusItem.Highlight.BAD, "long-press",
                    new Runnable() {
                        @Override
                        public void run() {
                            JoH.static_toast_long("Cleared error message");
                            UploaderTask.exception = null;
                        }
                    }));
        }


        if (last_query > 0)
            l.add(new StatusItem("Last poll", JoH.niceTimeSince(last_query) + " ago", StatusItem.Highlight.NORMAL, "long-press",
                    new Runnable() {
                        @Override
                        public void run() {
                            if (JoH.ratelimit("nightscout-manual-poll", 15)) {
                                startSyncService(100);
                                JoH.static_toast_short("Polling");
                                if (TidepoolEntry.enabled()) {
                                    TidepoolUploader.doLogin(true);
                                }
                            }
                        }
                    }));


        // enumerate status items for nightscout rest-api
        if (Pref.getBooleanDefaultFalse("cloud_storage_api_enable")) {
            try {

                /*if (NightscoutUploader.last_success_time > 0) {
                    l.add(new StatusItem("REST Success", JoH.niceTimeSince(NightscoutUploader.last_success_time) + " ago", StatusItem.Highlight.NORMAL));
                }*/

                if ((processedBaseURIs == null) || (JoH.ratelimit("uploader-base-urls-cache", 60))) {
                    // Rebuild url cache
                    processedBaseURIs = new ArrayList<>();
                    processedBaseURInames = new ArrayList<>();
                    final String baseURLSettings = Pref.getStringDefaultBlank("cloud_storage_api_base");
                    final ArrayList<String> baseURIs = new ArrayList<>();

                    for (String baseURLSetting : baseURLSettings.split(" ")) {
                        String baseURL = baseURLSetting.trim();
                        if (baseURL.isEmpty()) continue;
                        baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
                    }
                    for (String baseURI : baseURIs) {
                        final URI uri = new URI(baseURI);
                        final String baseURL = baseURI.replaceFirst("//[^@]+@", "//");
                        processedBaseURIs.add(baseURL);
                        processedBaseURInames.add(uri.getHost());
                    }
                }

                // lookup status for each url in cache
                for (int i = 0; i < processedBaseURIs.size(); i++) {
                    try {
                        final String store_marker = "nightscout-status-poll-" + processedBaseURIs.get(i);
                        final JSONObject status = new JSONObject(PersistentStore.getString(store_marker));
                        l.add(new StatusItem(processedBaseURInames.get(i), status.getString("name") + " " + status.getString("version"), status.getString("version").startsWith("0.8") ? StatusItem.Highlight.CRITICAL : StatusItem.Highlight.NORMAL, "long-press",
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshStatus(store_marker);
                                    }
                                }));

                        if (status.has("careportalEnabled") && !status.getBoolean("careportalEnabled")) {
                            l.add(new StatusItem("Config error in Nightscout at " + processedBaseURInames.get(i), "You must enable the careportal plugin or treatment sync will be broken", StatusItem.Highlight.BAD, "long-press",
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            refreshStatus(store_marker);
                                        }
                                    }));
                        }

                        final JSONObject extended = status.getJSONObject("extendedSettings");
                        final JSONObject dextended = (extended.has("devicestatus") ? extended.getJSONObject("devicestatus") : null);
                        if ((dextended == null) || !dextended.has("advanced") || !dextended.getBoolean("advanced"))
                            l.add(new StatusItem("Config error in Nightscout at " + processedBaseURInames.get(i), "You must set DEVICESTATUS_ADVANCED env item to True for multiple battery status to work properly", StatusItem.Highlight.BAD, "long-press",
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            refreshStatus(store_marker);
                                        }
                                    }));


                    } catch (JSONException e) {

                    }
                }
            } catch (Exception e) {
                //
            }

        }


        ///

        if (NightscoutUploader.last_exception_count > 0) {
            l.add(new StatusItem("REST-API problem\n" + JoH.dateTimeText(NightscoutUploader.last_exception_time) + " (" + NightscoutUploader.last_exception_count + ")", NightscoutUploader.last_exception, StatusItem.Highlight.BAD));
        }


        if (TidepoolEntry.enabled()) {
            l.addAll(TidepoolStatus.megaStatus());
        }


        if (last_cleanup > 0)
            l.add(new StatusItem("Last clean up", JoH.niceTimeSince(last_cleanup) + " ago"));

        return l;
    }

    private static void refreshStatus(String store_marker) {
        PersistentStore.setString(store_marker, "");
        if (JoH.ratelimit("nightscout-manual-poll", 15)) {
            startSyncService(100);
            JoH.static_toast_short("Refreshing Status");
        }
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

    /** Mirrors the ActiveAndroid Model.getId() used by callers. */
    public Long getId() {
        return _id;
    }

    private static UploaderQueueDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).uploaderQueueDao();
    }
}
