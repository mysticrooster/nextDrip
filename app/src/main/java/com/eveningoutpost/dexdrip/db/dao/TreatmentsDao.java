package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.Treatments;

import java.util.List;

@Dao
public interface TreatmentsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Treatments treatment);

    @Update
    void update(Treatments treatment);

    @Delete
    void delete(Treatments treatment);

    @Query("DELETE FROM Treatments")
    void deleteAll();

    @Query("SELECT * FROM Treatments ORDER BY _id DESC LIMIT 1")
    Treatments last();

    @Query("SELECT * FROM Treatments WHERE enteredBy NOT LIKE :xdripPrefix ORDER BY _id DESC LIMIT 1")
    Treatments lastNotFromXdrip(String xdripPrefix);

    @Query("SELECT * FROM Treatments WHERE enteredBy LIKE :xdripPrefix AND eventType = :eventType ORDER BY _id DESC LIMIT 1")
    Treatments lastEventTypeFromXdrip(String xdripPrefix, String eventType);

    @Query("SELECT * FROM Treatments ORDER BY timestamp DESC LIMIT :num")
    List<Treatments> latest(int num);

    @Query("SELECT * FROM Treatments WHERE uuid = :uuid ORDER BY _id DESC LIMIT 1")
    Treatments byuuid(String uuid);

    @Query("SELECT * FROM Treatments WHERE _id = :id LIMIT 1")
    Treatments byid(long id);

    @Query("SELECT * FROM Treatments WHERE timestamp <= :upper AND timestamp >= :lower "
            + "ORDER BY abs(timestamp - :timestamp) ASC LIMIT 1")
    Treatments byTimestamp(long upper, long lower, long timestamp);

    @Query("SELECT * FROM Treatments WHERE timestamp = :timestamp ORDER BY timestamp DESC")
    List<Treatments> listByTimestamp(long timestamp);

    @Query("SELECT * FROM Treatments WHERE timestamp >= :start AND timestamp <= :end "
            + "ORDER BY timestamp ASC LIMIT :limit")
    List<Treatments> latestForGraph(double start, double end, int limit);

    @Query("DELETE FROM Treatments WHERE timestamp < :cutoff")
    int cleanup(long cutoff);

    @Query("SELECT SUM(carbs) FROM Treatments WHERE timestamp >= :from AND timestamp <= :to")
    Double sumCarbs(long from, long to);

    @Query("SELECT SUM(insulin) FROM Treatments WHERE timestamp >= :from AND timestamp <= :to")
    Double sumInsulin(long from, long to);
}
