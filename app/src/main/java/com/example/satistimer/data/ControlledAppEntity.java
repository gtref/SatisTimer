package com.example.satistimer.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "controlled_apps")
public class ControlledAppEntity {
    @PrimaryKey
    @NonNull
    public String packageName = "";

    @NonNull
    public String displayName = "";

    public boolean enabled;
}
