package com.example.satistimer.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AppDao {
    @Query("SELECT * FROM controlled_apps ORDER BY displayName")
    List<ControlledAppEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ControlledAppEntity app);
}
