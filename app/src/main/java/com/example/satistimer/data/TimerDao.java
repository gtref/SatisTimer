package com.example.satistimer.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TimerDao {
    @Query("SELECT * FROM app_timers")
    List<TimerEntity> getAll();

    @Query("SELECT * FROM app_timers WHERE packageName = :packageName LIMIT 1")
    TimerEntity findByPackageName(String packageName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(TimerEntity timer);
}
