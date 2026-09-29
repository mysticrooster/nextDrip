package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.UserNotification;

@Dao
public interface UserNotificationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UserNotification userNotification);

    @Update
    void update(UserNotification userNotification);

    @Delete
    void delete(UserNotification userNotification);

    @Query("DELETE FROM Notifications")
    void deleteAll();

    @Query("SELECT * FROM Notifications WHERE bg_alert = 1 ORDER BY _id DESC LIMIT 1")
    UserNotification lastBgAlert();

    @Query("SELECT * FROM Notifications WHERE calibration_alert = 1 ORDER BY _id DESC LIMIT 1")
    UserNotification lastCalibrationAlert();

    @Query("SELECT * FROM Notifications WHERE double_calibration_alert = 1 ORDER BY _id DESC LIMIT 1")
    UserNotification lastDoubleCalibrationAlert();

    @Query("SELECT * FROM Notifications WHERE extra_calibration_alert = 1 ORDER BY _id DESC LIMIT 1")
    UserNotification lastExtraCalibrationAlert();

    @Query("SELECT * FROM Notifications WHERE bg_unclear_readings_alert = 1 ORDER BY _id DESC LIMIT 1")
    UserNotification lastBgUnclearReadingsAlert();

    @Query("SELECT * FROM Notifications WHERE bg_missed_alerts = 1 ORDER BY _id DESC LIMIT 1")
    UserNotification lastBgMissedAlerts();

    @Query("SELECT * FROM Notifications WHERE bg_rise_alert = 1 ORDER BY _id DESC LIMIT 1")
    UserNotification lastBgRiseAlert();

    @Query("SELECT * FROM Notifications WHERE bg_fall_alert = 1 ORDER BY _id DESC LIMIT 1")
    UserNotification lastBgFallAlert();
}
