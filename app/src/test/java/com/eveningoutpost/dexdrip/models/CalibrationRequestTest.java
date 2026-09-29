package com.eveningoutpost.dexdrip.models;

import androidx.room.Room;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.CalibrationRequestDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

public class CalibrationRequestTest extends RobolectricTestWithConfig {

    private AppDatabase database;
    private CalibrationRequestDao dao;

    @Before
    public void setUpDatabase() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstanceForTesting(database);
        dao = database.calibrationRequestDao();
    }

    @After
    public void tearDownDatabase() {
        database.close();
        AppDatabase.setInstanceForTesting(null);
    }

    @Test
    public void createRangeInsertsMatchableRow() {
        CalibrationRequest.createRange(140.0, 160.0);

        assertWithMessage("one row inserted").that(dao.getAll()).hasSize(1);
        assertWithMessage("value inside range matches").that(dao.findMatching(150.0)).isNotNull();
        assertWithMessage("value outside range does not match").that(dao.findMatching(120.0)).isNull();
    }

    @Test
    public void createOffsetInsertsTwoRows() {
        CalibrationRequest.createOffset(150.0, 20.0);

        assertWithMessage("two rows inserted").that(dao.getAll()).hasSize(2);
        assertWithMessage("above row matches").that(dao.findMatching(200.0)).isNotNull();
        assertWithMessage("below row matches").that(dao.findMatching(100.0)).isNotNull();
        assertWithMessage("midpoint does not match").that(dao.findMatching(150.0)).isNull();
    }

    @Test
    public void clearAllRemovesRows() {
        CalibrationRequest.createRange(140.0, 160.0);
        CalibrationRequest.clearAll();

        assertWithMessage("all rows removed").that(dao.getAll()).isEmpty();
    }

    @Test
    public void shouldRequestCalibrationWhenInRangeAndSlopeFlat() {
        CalibrationRequest.createRange(140.0, 160.0);
        final BgReading bgReading = new BgReading();
        bgReading.calculated_value = 150.0;
        bgReading.calculated_value_slope = 0.0;

        assertWithMessage("in-range flat reading requests calibration")
                .that(CalibrationRequest.shouldRequestCalibration(bgReading)).isTrue();
    }

    @Test
    public void shouldNotRequestCalibrationWhenOutOfRange() {
        CalibrationRequest.createRange(140.0, 160.0);
        final BgReading bgReading = new BgReading();
        bgReading.calculated_value = 120.0;
        bgReading.calculated_value_slope = 0.0;

        assertWithMessage("out-of-range reading does not request calibration")
                .that(CalibrationRequest.shouldRequestCalibration(bgReading)).isFalse();
    }
}
