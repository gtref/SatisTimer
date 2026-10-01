package com.example.satistimer.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_timers")
public class TimerEntity {
    @PrimaryKey
    @NonNull
    public String packageName = "";

    public long endTimeMillis;
    public long cycleDurationMillis;
    public boolean bricked;
}
