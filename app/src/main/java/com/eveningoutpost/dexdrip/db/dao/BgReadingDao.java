package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.BgReading;

import java.util.List;

@Dao
public interface BgReadingDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(BgReading bgReading);

    @Update
    void update(BgReading bgReading);

    @Delete
    void delete(BgReading bgReading);

    @Query("DELETE FROM BgReadings")
    void deleteAll();

    @Query("SELECT * FROM BgReadings WHERE sensor = :sensor_id AND timestamp <= :upper "
            + "AND calculated_value = 0 AND raw_calculated = 0 ORDER BY timestamp DESC LIMIT 1")
    BgReading getForTimestampUncalculated(long sensor_id, long upper);

    @Query("SELECT * FROM BgReadings WHERE sensor = :sensor_id AND timestamp <= :upper ORDER BY timestamp DESC LIMIT 1")
    BgReading getForTimestampExists(long sensor_id, long upper);

    @Query("SELECT * FROM BgReadings WHERE sensor = :sensor_id AND timestamp <= :upper AND timestamp >= :lower "
            + "ORDER BY abs(timestamp - :timestamp) ASC LIMIT 1")
    BgReading preciseLocked(long sensor_id, long lower, long upper, long timestamp);

    @Query("SELECT * FROM BgReadings WHERE timestamp <= :upper AND timestamp >= :lower "
            + "ORDER BY abs(timestamp - :timestamp) ASC LIMIT 1")
    BgReading preciseUnlocked(long lower, long upper, long timestamp);

    @Query("SELECT * FROM BgReadings WHERE calculated_value != 0 AND raw_data != 0 ORDER BY timestamp DESC LIMIT 1")
    BgReading lastAny();

    @Query("SELECT * FROM BgReadings WHERE sensor = :sensor_id AND calculated_value != 0 AND raw_data != 0 "
            + "ORDER BY timestamp DESC LIMIT 1")
    BgReading lastForSensor(long sensor_id);

    @Query("SELECT * FROM BgReadings WHERE sensor = :sensor_id AND raw_data != 0 ORDER BY timestamp DESC LIMIT :number")
    List<BgReading> latestRawForSensor(long sensor_id, int number);

    @Query("SELECT * FROM BgReadings WHERE calculated_value != 0 AND raw_data != 0 ORDER BY timestamp DESC LIMIT :number")
    List<BgReading> latestAny(int number);

    @Query("SELECT * FROM BgReadings WHERE sensor = :sensor_id AND calculated_value != 0 AND raw_data != 0 "
            + "ORDER BY timestamp DESC LIMIT :number")
    List<BgReading> latestForSensor(long sensor_id, int number);

    @Query("SELECT * FROM BgReadings WHERE timestamp >= :start AND timestamp <= :end "
            + "AND calculated_value != 0 AND raw_data != 0 ORDER BY timestamp DESC LIMIT :number")
    List<BgReading> latestForGraph(long start, long end, int number);

    @Query("SELECT * FROM BgReadings WHERE sensor = :sensor_id AND timestamp >= :start AND timestamp <= :end "
            + "AND calculated_value != 0 AND raw_data != 0 AND calibration_uuid != '' "
            + "ORDER BY timestamp DESC LIMIT :number")
    List<BgReading> latestForGraphSensor(long sensor_id, long start, long end, int number);

    @Query("SELECT * FROM BgReadings WHERE timestamp >= :start AND timestamp <= :end "
            + "AND calculated_value != 0 AND raw_data != 0 ORDER BY timestamp ASC LIMIT :number")
    List<BgReading> latestAscAny(long start, long end, int number);

    @Query("SELECT * FROM BgReadings WHERE sensor = :sensor_id AND timestamp >= :start AND timestamp <= :end "
            + "AND calculated_value != 0 AND raw_data != 0 ORDER BY timestamp ASC LIMIT :number")
    List<BgReading> latestAscForSensor(long sensor_id, long start, long end, int number);

    @Query("SELECT * FROM BgReadings WHERE timestamp >= :lower AND timestamp <= :upper "
            + "AND calculated_value != 0 AND raw_data != 0 LIMIT 1")
    BgReading readingNearTimestamp(double lower, double upper);

    @Query("SELECT * FROM BgReadings WHERE timestamp >= :since AND calculated_value != 0 AND raw_data != 0 "
            + "ORDER BY timestamp DESC")
    List<BgReading> since(double since);

    @Query("SELECT * FROM BgReadings WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    List<BgReading> future(double timestamp);

    @Query("SELECT * FROM BgReadings WHERE uuid = :uuid LIMIT 1")
    BgReading findByUuid(String uuid);

    @Query("SELECT * FROM BgReadings WHERE _id = :id LIMIT 1")
    BgReading byid(long id);

    @Query("SELECT * FROM BgReadings WHERE timestamp < :timestamp ORDER BY timestamp DESC")
    List<BgReading> olderThan(long timestamp);

    @Query("DELETE FROM BgReadings WHERE timestamp < :end AND timestamp > :start")
    int deleteRange(long start, long end);

    @Query("DELETE FROM BgReadings WHERE timestamp < :cutoff")
    int deleteOlderThan(long cutoff);

    @Query("DELETE FROM BgReadings WHERE timestamp > :cutoff AND calculated_value > :value")
    int deleteOutOfRange(long cutoff, double value);

    @Query("SELECT COUNT(*) FROM BgReadings WHERE timestamp >= :start AND timestamp <= :stop "
            + "AND calculated_value > :cutoff AND calculated_value > :high AND snyced = 0")
    int countAbove(long start, long stop, double cutoff, double high);

    @Query("SELECT COUNT(*) FROM BgReadings WHERE timestamp >= :start AND timestamp <= :stop "
            + "AND calculated_value > :cutoff AND calculated_value <= :high AND calculated_value >= :low AND snyced = 0")
    int countIn(long start, long stop, double cutoff, double high, double low);

    @Query("SELECT COUNT(*) FROM BgReadings WHERE timestamp >= :start AND timestamp <= :stop "
            + "AND calculated_value > :cutoff AND calculated_value < :low AND snyced = 0")
    int countBelow(long start, long stop, double cutoff, double low);

    @Query("SELECT * FROM BgReadings WHERE timestamp >= :start AND timestamp <= :stop "
            + "AND calculated_value > :cutoff AND snyced = 0 ORDER BY calculated_value DESC")
    List<BgReading> statsReadingsOrdered(long start, long stop, double cutoff);

    @Query("SELECT * FROM BgReadings WHERE timestamp >= :start AND timestamp <= :stop "
            + "AND calculated_value > :cutoff AND snyced = 0")
    List<BgReading> statsReadingsUnordered(long start, long stop, double cutoff);

    @Query("SELECT COUNT(*) FROM BgReadings WHERE timestamp >= :from AND timestamp <= :to "
            + "AND calculated_value >= :low AND calculated_value <= :high AND snyced = 0")
    int statsCountIn(long from, long to, double low, double high);

    @Query("SELECT COUNT(*) FROM BgReadings WHERE timestamp >= :from AND timestamp <= :to "
            + "AND calculated_value > :cutoff AND calculated_value < :low AND snyced = 0")
    int statsCountBelow(long from, long to, double cutoff, double low);

    @Query("SELECT COUNT(*) FROM BgReadings WHERE timestamp >= :from AND timestamp <= :to "
            + "AND calculated_value > :high AND snyced = 0")
    int statsCountAbove(long from, long to, double high);

    @Query("SELECT COUNT(*) FROM BgReadings WHERE timestamp >= :from AND timestamp <= :to "
            + "AND source_info LIKE '%Backfill' AND snyced = 0")
    int statsCountBackfill(long from, long to);

    @Query("SELECT avg(calculated_value) FROM BgReadings WHERE timestamp >= :from AND timestamp <= :to "
            + "AND calculated_value > :cutoff AND snyced = 0")
    Double statsAvg(long from, long to, double cutoff);

    @Query("SELECT ((count(*)*(sum(calculated_value * calculated_value)) - (sum(calculated_value)*sum(calculated_value)))"
            + "/((count(*)-1)*(count(*)))) FROM BgReadings WHERE timestamp >= :from AND timestamp <= :to "
            + "AND calculated_value > :cutoff AND snyced = 0")
    Double statsStdev(long from, long to, double cutoff);

    @Query("SELECT calculated_value FROM BgReadings WHERE timestamp >= :from AND timestamp <= :to "
            + "AND calculated_value > :cutoff AND snyced = 0")
    List<Double> statsValues(long from, long to, double cutoff);
}
