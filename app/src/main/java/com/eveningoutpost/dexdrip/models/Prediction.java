package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.PredictionDao;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Created by jamorham on 11/06/2018.
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "Prediction",
        indices = {
                @Index("source"),
                @Index(value = "timestamp", unique = true)
        })
public class Prediction {

    private final static String TAG = Prediction.class.getSimpleName();
    private final static boolean d = false;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @Expose
    @ColumnInfo(name = "glucose")
    public double glucose;

    @Expose
    @ColumnInfo(name = "source")
    public String source;

    @Expose
    @ColumnInfo(name = "note")
    public String note;


    public static Prediction create(long timestamp, int glucose, String source) {
        final Prediction prediction = new Prediction();
        prediction.timestamp = timestamp;
        prediction.glucose = glucose;
        prediction.source = source;
        return prediction;
    }

    public Prediction addNote(String note) {
        this.note = note;
        return this;
    }


    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

    // static methods

    public static Prediction last() {
        return dao().last();
    }

    public static List<Prediction> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<Prediction> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<Prediction> latestForGraph(int number, long startTime, long endTime) {
        return dao().latestForGraph(Math.max(startTime, 0), endTime, number);
    }


    public static List<Prediction> cleanup(int retention_days) {
        final int deleted = dao().cleanup(JoH.tsl() - (retention_days * 86400000L));
        UserError.Log.d(TAG, "Prediction cleanup removed " + deleted + " record(s)");
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

    private static PredictionDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).predictionDao();
    }
}
