package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

public class BgReadingRoomTest extends RobolectricTestWithConfig {

    private AppDatabase database;

    @Before
    public void setUpDatabase() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstanceForTesting(database);
    }

    @After
    public void tearDownDatabase() {
        database.close();
        AppDatabase.setInstanceForTesting(null);
    }

    @Test
    public void foreignKeyIdsPersistAndCalibrationHydrates() {
        final Sensor sensor = new Sensor();
        sensor.uuid = "s1";
        sensor.started_at = 1000L;
        sensor.save();

        final Calibration calibration = new Calibration();
        calibration.uuid = "c1";
        calibration.timestamp = 1000L;
        calibration.sensor = sensor;
        calibration.slope = 1;
        calibration.intercept = 0;
        calibration.save();

        final BgReading bg = new BgReading();
        bg.uuid = "b1";
        bg.timestamp = 1000L;
        bg.calculated_value = 100;
        bg.raw_data = 100;
        bg.sensor = sensor;
        bg.calibration = calibration;
        bg.save();

        final BgReading loaded = BgReading.byid(bg._id);
        assertThat(loaded.sensor_id).isEqualTo(sensor._id);
        assertThat(loaded.calibration_id).isEqualTo(calibration._id);
        // The calibration object is transient, so it must be hydrated from the id on demand.
        assertThat(loaded.calibration).isNull();
        assertThat(loaded.getCalibration()).isNotNull();
        assertThat(loaded.getCalibration().uuid).isEqualTo("c1");
    }

    @Test
    public void calibrationSensorIdPersists() {
        final Sensor sensor = new Sensor();
        sensor.uuid = "s2";
        sensor.started_at = 2000L;
        sensor.save();

        final Calibration calibration = new Calibration();
        calibration.uuid = "c2";
        calibration.timestamp = 2000L;
        calibration.sensor = sensor;
        calibration.save();

        assertThat(Calibration.byuuid("c2").sensor_id).isEqualTo(sensor._id);
    }
}
