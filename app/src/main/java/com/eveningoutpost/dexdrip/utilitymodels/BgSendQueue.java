package com.eveningoutpost.dexdrip.utilitymodels;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.BgSendQueueDao;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.NewDataObserver;
import com.eveningoutpost.dexdrip.services.SyncService;
import com.eveningoutpost.dexdrip.WidgetUpdateService;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.xDripWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emma Black on 11/7/14.
 */
@Deprecated
@Entity(tableName = "BgSendQueue",
        indices = {
                @Index("bgReading"),
                @Index("success"),
                @Index("mongo_success")
        })
public class BgSendQueue {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @ColumnInfo(name = "bgReading")
    public long bgReading_id;

    @ColumnInfo(name = "success")
    public boolean success;

    @ColumnInfo(name = "mongo_success")
    public boolean mongo_success;

    @ColumnInfo(name = "operation_type")
    public String operation_type;

    /*
        public static List<BgSendQueue> queue() {
            return new Select()
                    .from(BgSendQueue.class)
                    .where("success = ?", false)
                    .orderBy("_ID asc")
                    .limit(20)
                    .execute();
        }
    */
    @Deprecated
    public static void emptyQueue() {
        try {
            dao().deleteAll();
        } catch (Exception e) {
            // failed
        }
    }

    @Deprecated
    public static void deleteAll() {
        dao().deleteAll();
    }

    @Deprecated
    public static List<BgSendQueue> mongoQueue() {
        return dao().mongoQueue();
    }

    @Deprecated
    public static List<BgSendQueue> cleanQueue() {
        dao().cleanQueue();
        return new ArrayList<>();
    }

    @Deprecated
    public static int countBySuccess(boolean success) {
        return dao().countBySuccess(success);
    }

    @Deprecated
    public static int countByMongoSuccess(boolean success) {
        return dao().countByMongoSuccess(success);
    }

    @Deprecated
    private static void addToQueue(BgReading bgReading, String operation_type) {
        // deprecated no-op: uploads now go through UploaderQueue
    }

    public static void handleNewBgReading(BgReading bgReading, String operation_type, Context context) {
        handleNewBgReading(bgReading, operation_type, context, false);
    }

    public static void handleNewBgReading(BgReading bgReading, String operation_type, Context context, boolean is_follower) {
        handleNewBgReading(bgReading, operation_type, context, is_follower, false);
    }

    // TODO extract to non depreciated class
    public static void handleNewBgReading(final BgReading bgReading, String operation_type, Context context, boolean is_follower, boolean quick) {
        if (bgReading == null) {
            UserError.Log.wtf("BgSendQueue", "handleNewBgReading called with null bgReading!");
            return;
        }
        final PowerManager.WakeLock wakeLock = JoH.getWakeLock("sendQueue", 120000);
        try {

            // Add to upload queue
            //if (!is_follower) {
                UploaderQueue.newEntry(operation_type, bgReading);
            //}

            // all this other UI stuff probably shouldn't be here but in lieu of a better method we keep with it..
            if (!quick) {
                if (Home.activityVisible) {
                    context.sendBroadcast(new Intent(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA));
                }

                if (AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, xDripWidget.class)).length > 0) {
                    //context.startService(new Intent(context, WidgetUpdateService.class));
                    JoH.startService(WidgetUpdateService.class);
                }
            }

            // emit local broadcast
            BroadcastGlucose.sendLocalBroadcast(bgReading);

                // TODO I don't really think this is needed anymore
                if (!quick && Pref.getBooleanDefaultFalse("excessive_wakelocks")) {
                    // just keep it alive for 3 more seconds to allow the watch to be updated
                    // dangling wakelock
                    JoH.getWakeLock("broadcstNightWatch", 3000);
                }


            if (!quick) {
                NewDataObserver.newBgReading(bgReading, is_follower);
            }

            if ((!is_follower) && (Pref.getBoolean("plus_follow_master", false))) {
                if (Pref.getBoolean("display_glucose_from_plugin", false))
                {
                    // TODO does this currently ignore noise or is noise properly calculated on the follower?
                    // munge bgReading for follower TODO will probably want extra option for this in future
                    // TODO we maybe don't need deep clone for this! Check how value will be used below
                    //GcmActivity.syncBGReading(PluggableCalibration.mungeBgReading(new Cloner().deepClone(bgReading)));
                    GcmActivity.syncBGReading(PluggableCalibration.mungeBgReading(BgReading.fromJSON(bgReading.toJSON(true))));
                } else {
                    // send as is
                    GcmActivity.syncBGReading(bgReading);
                }
            }

            // process the uploader queue
            if (JoH.ratelimit("start-sync-service", 30)) {
                JoH.startService(SyncService.class);
            }


        } finally {
            JoH.releaseWakeLock(wakeLock);
        }
    }

    public static void sendToPhone(Context context) {
        // This is just a stub - only used on Android Wear
    }

   /* @Deprecated
    public void markMongoSuccess() {
        this.mongo_success = true;
        save();
    }*/

    private static BgSendQueueDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).bgSendQueueDao();
    }

}
