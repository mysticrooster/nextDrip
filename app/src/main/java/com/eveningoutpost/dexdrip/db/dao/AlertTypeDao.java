package com.eveningoutpost.dexdrip.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.eveningoutpost.dexdrip.models.AlertType;

import java.util.List;

@Dao
public interface AlertTypeDao {

    @Insert
    long insert(AlertType alertType);

    @Update
    void update(AlertType alertType);

    @Delete
    void delete(AlertType alertType);

    @Query("DELETE FROM AlertType")
    void deleteAll();

    @Query("SELECT * FROM AlertType WHERE uuid = :uuid LIMIT 1")
    AlertType getByUuid(String uuid);

    @Query("SELECT * FROM AlertType WHERE threshold >= :threshold AND above = 0 ORDER BY threshold ASC")
    List<AlertType> lowAlerts(double threshold);

    @Query("SELECT * FROM AlertType WHERE threshold <= :threshold AND above = 1 ORDER BY threshold DESC")
    List<AlertType> highAlerts(double threshold);

    @Query("SELECT * FROM AlertType WHERE active = 1")
    List<AlertType> getAllActive();

    @Query("SELECT * FROM AlertType WHERE above = 1 ORDER BY threshold ASC")
    List<AlertType> getAllAbove();

    @Query("SELECT * FROM AlertType WHERE above = 0 ORDER BY threshold DESC")
    List<AlertType> getAllBelow();

    @Query("SELECT * FROM AlertType")
    List<AlertType> getAll();
}
