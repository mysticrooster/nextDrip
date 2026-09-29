package com.eveningoutpost.dexdrip.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

/**
 * Created by jamorham on 19/10/2017.
 */

@Entity(tableName = "LibreData",
        indices = {
                @Index("timestamp")
        })
public class LibreData {
    private static final String TAG = "LibreData";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long _id;

    @Expose
    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @Expose
    @ColumnInfo(name = "temperature")
    public double temperature;

    @Expose
    @ColumnInfo(name = "temperatureraw")
    public long temperatureraw;


    public static LibreData create(byte[] temp_bytes) {
        final LibreData ld = new LibreData();
        ld.timestamp = JoH.tsl();
        // TODO
        //ld.temperatureraw = get byte order value from temp_bytes
        //ld.temperature = evaluate temperature from temperature raw
        return ld;
    }

    private static final boolean d = false;

}
