package com.eveningoutpost.dexdrip.utilitymodels;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.SensorSendQueueDao;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.List;

/**
 * Created by Emma Black on 11/7/14.
 */
@Entity(tableName = "SensorSendQueue",
        indices = {
                @Index("Sensor"),
                @Index("success")
        })
public class SensorSendQueue {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @ColumnInfo(name = "Sensor")
    public long sensor_id;

    @ColumnInfo(name = "success")
    public boolean success;


    public static SensorSendQueue nextSensorJob() {
        return dao().nextUnsuccessful();
    }

    public static List<SensorSendQueue> queue() {
        return dao().queue();
    }

    public static void addToQueue(Sensor sensor) {
        SendToFollower(sensor);
        SensorSendQueue sensorSendQueue = new SensorSendQueue();
        sensorSendQueue.sensor_id = sensor._id;
        sensorSendQueue.success = false;
        dao().insert(sensorSendQueue);
    }
    
    public static void SendToFollower(Sensor sensor) {
       // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        if(Home.get_master()) {
            GcmActivity.syncSensor(sensor, true);
        }
    }

    public static void deleteAll() {
        dao().deleteAll();
    }

    private static SensorSendQueueDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).sensorSendQueueDao();
    }
}
