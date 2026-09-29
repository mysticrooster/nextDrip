package com.eveningoutpost.dexdrip.models;

import android.text.format.DateFormat;

import androidx.room.ColumnInfo;
import androidx.room.DatabaseView;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.Libre2SensorDao;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.List;

@DatabaseView(viewName = "Libre2Sensors",
        value = "SELECT MIN(_id) as _id, serial, MIN(ts) as ts_from, MAX(ts) AS ts_to, COUNT(*) AS readings "
                + "FROM Libre2RawValue2 GROUP BY serial ORDER BY ts DESC")
public class Libre2Sensor {
    static final String TAG = "Libre2Sensor";

    @ColumnInfo(name = "_id")
    public long _id;

    @ColumnInfo(name = "serial")
    public String serial;

    @ColumnInfo(name = "ts_from")
    public long ts_from;

    @ColumnInfo(name = "ts_to")
    public long ts_to;

    @ColumnInfo(name = "readings")
    public long readings;

    private static volatile String cachedStringSensors = null;

    public static String Libre2Sensors() {
        String Sum = "";

        if ((cachedStringSensors == null) || (JoH.ratelimit("libre2sensor-report", 120))) {

            List<Libre2Sensor> rs = dao().all();

            for (Libre2Sensor Sensorpart : rs) {
                Long Diff_ts = Sensorpart.ts_to - Sensorpart.ts_from;
                Sum = Sum + Sensorpart.serial +
                        "\n" + DateFormat.format("dd.MM.yy", Sensorpart.ts_from) +
                        " to: " + DateFormat.format("dd.MM.yy", Sensorpart.ts_to) +
                        " (" + JoH.niceTimeScalarShortWithDecimalHours(Diff_ts) + ")" +
                        " readings: " + ((Sensorpart.readings * 100) / (Diff_ts / 60000)) + "%\n" +
                        "------------------\n";
            }
            cachedStringSensors = Sum;
        }

        return cachedStringSensors;
    }

    private static Libre2SensorDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).libre2SensorDao();
    }
}
