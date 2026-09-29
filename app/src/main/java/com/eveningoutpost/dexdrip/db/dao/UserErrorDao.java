package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.UserError;

import java.util.List;

@Dao
public interface UserErrorDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UserError userError);

    @Update
    void update(UserError userError);

    @Delete
    void delete(UserError userError);

    @Query("SELECT * FROM UserErrors ORDER BY timestamp DESC")
    List<UserError> all();

    @Query("SELECT * FROM UserErrors WHERE severity < 3 AND timestamp < :cutoff ORDER BY timestamp DESC")
    List<UserError> deletableLow(long cutoff);

    @Query("SELECT * FROM UserErrors WHERE severity = 3 AND timestamp < :cutoff ORDER BY timestamp DESC")
    List<UserError> deletableHigh(long cutoff);

    @Query("SELECT * FROM UserErrors WHERE severity > 3 AND timestamp < :cutoff ORDER BY timestamp DESC")
    List<UserError> deletableEvents(long cutoff);

    @Query("SELECT * FROM UserErrors WHERE severity IN (:levels) ORDER BY timestamp DESC LIMIT 10000")
    List<UserError> bySeverity(List<Integer> levels);

    @Query("SELECT * FROM UserErrors WHERE _id > :id AND severity IN (:levels) ORDER BY timestamp DESC LIMIT :limit")
    List<UserError> bySeverityNewerThanID(long id, List<Integer> levels, int limit);

    @Query("SELECT * FROM UserErrors WHERE _id > :id ORDER BY timestamp DESC LIMIT :limit")
    List<UserError> newerThanID(long id, int limit);

    @Query("SELECT * FROM UserErrors WHERE _id < :id ORDER BY timestamp DESC LIMIT :limit")
    List<UserError> olderThanID(long id, int limit);

    @Query("SELECT * FROM UserErrors WHERE _id < :id AND severity IN (:levels) ORDER BY timestamp DESC LIMIT :limit")
    List<UserError> bySeverityOlderThanID(long id, List<Integer> levels, int limit);

    @Query("SELECT * FROM UserErrors WHERE severity = :level ORDER BY timestamp DESC LIMIT 1")
    UserError newestBySeverity(int level);

    @Query("SELECT * FROM UserErrors WHERE timestamp = :timestamp AND shortError = :shortError AND message = :message LIMIT 1")
    UserError getForTimestamp(long timestamp, String shortError, String message);

    @Query("SELECT * FROM UserErrors WHERE timestamp < :timestamp ORDER BY timestamp DESC")
    List<UserError> olderThan(long timestamp);

    @Query("DELETE FROM UserErrors WHERE timestamp < :cutoff AND severity < 3")
    int deleteLow(long cutoff);

    @Query("DELETE FROM UserErrors WHERE timestamp < :cutoff AND severity = 3")
    int deleteHigh(long cutoff);

    @Query("DELETE FROM UserErrors WHERE timestamp < :cutoff AND severity > 3")
    int deleteEvents(long cutoff);
}
