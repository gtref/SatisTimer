package com.example.satistimer.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TimerStore {
    public static final long TEST_CYCLE = 60L * 1000L;
    public static final long ONE_DAY = 24L * 60L * 60L * 1000L;
    public static final long TWO_DAYS = 2L * ONE_DAY;
    public static final long ONE_WEEK = 7L * ONE_DAY;

    private static final String PREFERENCES_NAME = "satis_timer_v001";
    private static final String ENTRY_PREFIX = "app.";
    private static final String LABEL_SUFFIX = ".label";
    private static final String LEGACY_END_SUFFIX = ".end";
    private static final String DURATION_SUFFIX = ".duration";
    private static final String REMAINING_SUFFIX = ".remaining";
    private static final String ACTIVE_FROM_SUFFIX = ".active_from";
    private static final String LOCK_SUFFIX = ".lock";
    private static final String CYCLE_SUFFIX = ".cycle";
    private static final String WARNING_SUFFIX = ".warning_sent";

    private TimerStore() {
    }

    public static final class Entry {
        public final String packageName;
        public final String displayName;
        public final long timerDurationMillis;
        public final long remainingMillis;
        public final long activeSinceMillis;
        public final long blockedUntilMillis;
        public final long cycleDurationMillis;
        public final boolean warningSent;

        private Entry(String packageName, String displayName, long timerDurationMillis,
                long remainingMillis, long activeSinceMillis, long blockedUntilMillis,
                long cycleDurationMillis, boolean warningSent) {
            this.packageName = packageName;
            this.displayName = displayName;
            this.timerDurationMillis = timerDurationMillis;
            this.remainingMillis = remainingMillis;
            this.activeSinceMillis = activeSinceMillis;
            this.blockedUntilMillis = blockedUntilMillis;
            this.cycleDurationMillis = cycleDurationMillis;
            this.warningSent = warningSent;
        }
    }

    public static List<Entry> getAll(Context context) {
        SharedPreferences preferences = getPreferences(context);
        Map<String, ?> values = preferences.getAll();
        List<Entry> entries = new ArrayList<>();
        for (String key : values.keySet()) {
            if (key.startsWith(ENTRY_PREFIX) && key.endsWith(LABEL_SUFFIX)) {
                String packageName = key.substring(ENTRY_PREFIX.length(),
                        key.length() - LABEL_SUFFIX.length());
                Entry entry = get(context, packageName);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    public static Entry get(Context context, String packageName) {
        return get(context, packageName, System.currentTimeMillis());
    }

    private static Entry get(Context context, String packageName, long nowMillis) {
        SharedPreferences preferences = getPreferences(context);
        String prefix = keyPrefix(packageName);
        String label = preferences.getString(prefix + LABEL_SUFFIX, null);
        if (label == null) {
            return null;
        }

        long blockedUntilMillis = preferences.getLong(prefix + LOCK_SUFFIX, 0L);
        long timerDurationMillis = preferences.getLong(prefix + DURATION_SUFFIX, -1L);
        long remainingMillis = preferences.getLong(prefix + REMAINING_SUFFIX, -1L);
        long activeSinceMillis = preferences.getLong(prefix + ACTIVE_FROM_SUFFIX, 0L);
        if (timerDurationMillis < 0L || remainingMillis < 0L) {
            long legacyEnd = preferences.getLong(prefix + LEGACY_END_SUFFIX, 0L);
            long migratedRemaining = Math.max(0L, legacyEnd - nowMillis);
            timerDurationMillis = migratedRemaining;
            remainingMillis = migratedRemaining;
            activeSinceMillis = 0L;
            preferences.edit()
                    .putLong(prefix + DURATION_SUFFIX, timerDurationMillis)
                    .putLong(prefix + REMAINING_SUFFIX, remainingMillis)
                    .putLong(prefix + ACTIVE_FROM_SUFFIX, activeSinceMillis)
                    .remove(prefix + LEGACY_END_SUFFIX)
                    .apply();
        }

        if (blockedUntilMillis != 0L && blockedUntilMillis <= nowMillis) {
            blockedUntilMillis = 0L;
            remainingMillis = timerDurationMillis;
            activeSinceMillis = 0L;
            preferences.edit()
                    .putLong(prefix + LOCK_SUFFIX, 0L)
                    .putLong(prefix + REMAINING_SUFFIX, remainingMillis)
                    .putLong(prefix + ACTIVE_FROM_SUFFIX, activeSinceMillis)
                    .putBoolean(prefix + WARNING_SUFFIX, false)
                    .apply();
        }

        if (activeSinceMillis > 0L) {
            remainingMillis = Math.max(0L,
                    remainingMillis - Math.max(0L, nowMillis - activeSinceMillis));
        }
        return new Entry(packageName, label, timerDurationMillis, remainingMillis,
                activeSinceMillis, blockedUntilMillis,
                preferences.getLong(prefix + CYCLE_SUFFIX, ONE_DAY),
                preferences.getBoolean(prefix + WARNING_SUFFIX, false));
    }

    public static void add(Context context, String packageName, String displayName) {
        String prefix = keyPrefix(packageName);
        getPreferences(context).edit()
                .putString(prefix + LABEL_SUFFIX, displayName)
            .putLong(prefix + DURATION_SUFFIX, 0L)
            .putLong(prefix + REMAINING_SUFFIX, 0L)
            .putLong(prefix + ACTIVE_FROM_SUFFIX, 0L)
                .putLong(prefix + LOCK_SUFFIX, 0L)
                .putLong(prefix + CYCLE_SUFFIX, ONE_DAY)
            .putBoolean(prefix + WARNING_SUFFIX, false)
                .apply();
    }

    public static void setTimer(Context context, String packageName, String displayName,
            long durationMillis, long cycleDurationMillis) {
        String prefix = keyPrefix(packageName);
        getPreferences(context).edit()
                .putString(prefix + LABEL_SUFFIX, displayName)
                .putLong(prefix + DURATION_SUFFIX, durationMillis)
                .putLong(prefix + REMAINING_SUFFIX, durationMillis)
                .putLong(prefix + ACTIVE_FROM_SUFFIX, 0L)
                .putLong(prefix + LOCK_SUFFIX, 0L)
                .putLong(prefix + CYCLE_SUFFIX, cycleDurationMillis)
                .putBoolean(prefix + WARNING_SUFFIX, false)
                .apply();
    }

    public static void startTimer(Context context, String packageName, long nowMillis) {
        Entry entry = get(context, packageName, nowMillis);
        if (entry == null || entry.timerDurationMillis <= 0L || entry.remainingMillis <= 0L
                || entry.activeSinceMillis > 0L || entry.blockedUntilMillis > 0L) {
            return;
        }
        getPreferences(context).edit()
                .putLong(keyPrefix(packageName) + ACTIVE_FROM_SUFFIX, nowMillis)
                .apply();
    }

    public static Entry pauseTimer(Context context, String packageName, long nowMillis) {
        Entry entry = get(context, packageName, nowMillis);
        if (entry == null || entry.activeSinceMillis <= 0L) {
            return entry;
        }
        getPreferences(context).edit()
                .putLong(keyPrefix(packageName) + REMAINING_SUFFIX, entry.remainingMillis)
                .putLong(keyPrefix(packageName) + ACTIVE_FROM_SUFFIX, 0L)
                .apply();
        return get(context, packageName, nowMillis);
    }

    public static void markWarningSent(Context context, String packageName) {
        getPreferences(context).edit()
                .putBoolean(keyPrefix(packageName) + WARNING_SUFFIX, true)
                .apply();
    }

    public static long getOrStartLock(Context context, String packageName, long nowMillis) {
        Entry entry = get(context, packageName, nowMillis);
        if (entry == null || entry.timerDurationMillis == 0L || entry.remainingMillis > 0L) {
            return 0L;
        }

        if (entry.blockedUntilMillis > nowMillis) {
            return entry.blockedUntilMillis;
        }
        if (entry.blockedUntilMillis != 0L) {
            return 0L;
        }

        long blockedUntilMillis = nowMillis + entry.cycleDurationMillis;
        getPreferences(context).edit()
            .putLong(keyPrefix(packageName) + REMAINING_SUFFIX, 0L)
            .putLong(keyPrefix(packageName) + ACTIVE_FROM_SUFFIX, 0L)
                .putLong(keyPrefix(packageName) + LOCK_SUFFIX, blockedUntilMillis)
                .apply();
        return blockedUntilMillis;
    }

    public static void remove(Context context, String packageName) {
        String prefix = keyPrefix(packageName);
        getPreferences(context).edit()
                .remove(prefix + LABEL_SUFFIX)
            .remove(prefix + LEGACY_END_SUFFIX)
            .remove(prefix + DURATION_SUFFIX)
            .remove(prefix + REMAINING_SUFFIX)
            .remove(prefix + ACTIVE_FROM_SUFFIX)
                .remove(prefix + LOCK_SUFFIX)
                .remove(prefix + CYCLE_SUFFIX)
            .remove(prefix + WARNING_SUFFIX)
                .apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static String keyPrefix(String packageName) {
        return ENTRY_PREFIX + packageName;
    }
}