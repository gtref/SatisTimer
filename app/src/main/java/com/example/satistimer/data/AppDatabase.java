package com.example.satistimer.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ControlledAppEntity.class, TimerEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AppDao appDao();

    public abstract TimerDao timerDao();
}
