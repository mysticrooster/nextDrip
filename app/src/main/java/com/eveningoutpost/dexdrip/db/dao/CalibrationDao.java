package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.Calibration;

import java.util.List;

@Dao
public interface CalibrationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Calibration calibration);

    @Update
    void update(Calibration calibration);

    @Query("DELETE FROM Calibration")
    void deleteAll();

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND timestamp <= :upper ORDER BY timestamp DESC LIMIT 1")
    Calibration closestBefore(long sensor_id, long upper);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND slope_confidence != 0 AND sensor_confidence != 0 "
            + "AND timestamp < :timestamp ORDER BY timestamp DESC LIMIT 1")
    Calibration getForTimestamp(long sensor_id, double timestamp);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND timestamp = :timestamp LIMIT 1")
    Calibration getByTimestamp(long sensor_id, double timestamp);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND slope_confidence != 0 AND sensor_confidence != 0 "
            + "AND timestamp > :since ORDER BY timestamp DESC")
    List<Calibration> allForSensorSince(long sensor_id, long since);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND slope_confidence != 0 AND sensor_confidence != 0 "
            + "ORDER BY timestamp DESC")
    List<Calibration> allForSensorDesc(long sensor_id);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND slope_confidence != 0 AND sensor_confidence != 0 "
            + "ORDER BY timestamp DESC LIMIT :limit")
    List<Calibration> allForSensorLimited(long sensor_id, int limit);

    @Query("SELECT * FROM Calibration WHERE _id = :id LIMIT 1")
    Calibration byid(long id);

    @Query("SELECT * FROM Calibration WHERE uuid = :uuid ORDER BY _id DESC LIMIT 1")
    Calibration byuuid(String uuid);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id ORDER BY timestamp DESC LIMIT 1")
    Calibration last(long sensor_id);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND slope_confidence != 0 AND sensor_confidence != 0 "
            + "AND slope != 0 AND intercept <= :maxIntercept ORDER BY timestamp DESC LIMIT 1")
    Calibration lastValid(long sensor_id, double maxIntercept);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND slope_confidence != 0 AND sensor_confidence != 0 "
            + "ORDER BY timestamp ASC LIMIT 1")
    Calibration first(long sensor_id);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND slope_confidence != 0 AND sensor_confidence != 0 "
            + "AND timestamp > :since ORDER BY bg DESC LIMIT 1")
    Calibration maxRecent(long sensor_id, long since);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND slope_confidence != 0 AND sensor_confidence != 0 "
            + "AND timestamp > :since ORDER BY bg ASC LIMIT 1")
    Calibration minRecent(long sensor_id, long since);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id ORDER BY timestamp DESC LIMIT :number")
    List<Calibration> latest(long sensor_id, int number);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND slope_confidence != 0 AND sensor_confidence != 0 "
            + "AND slope != 0 AND timestamp <= :until ORDER BY timestamp DESC LIMIT :number")
    List<Calibration> latestValid(long sensor_id, long until, int number);

    @Query("SELECT * FROM Calibration WHERE timestamp >= :start AND timestamp <= :end "
            + "AND (slope != 0 OR slope_confidence = :marker) ORDER BY timestamp DESC LIMIT :number")
    List<Calibration> latestForGraph(long start, long end, double marker, int number);

    @Query("SELECT * FROM Calibration WHERE sensor = :sensor_id AND timestamp >= :start AND timestamp <= :end "
            + "AND (slope != 0 OR slope_confidence = :marker) ORDER BY timestamp DESC LIMIT :number")
    List<Calibration> latestForGraphSensor(long sensor_id, long start, long end, double marker, int number);

    @Query("SELECT * FROM Calibration WHERE sensor_uuid = :uuid ORDER BY timestamp DESC LIMIT :limit")
    List<Calibration> forSensorUuid(String uuid, int limit);

    @Query("SELECT * FROM Calibration WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    List<Calibration> future(double timestamp);
}
