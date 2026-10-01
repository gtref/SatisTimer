package com.example.satistimer;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.view.accessibility.AccessibilityEvent;

import com.example.satistimer.data.TimerStore;

public class BlockService extends AccessibilityService {
    public static final String EXTRA_PACKAGE_NAME = "com.example.satistimer.blocked.PACKAGE_NAME";
    public static final String EXTRA_DISPLAY_NAME = "com.example.satistimer.blocked.DISPLAY_NAME";
    public static final String EXTRA_BLOCKED_UNTIL = "com.example.satistimer.blocked.UNTIL";

    private static final long WARNING_WINDOW_MILLIS = 60_000L;
    private static final long TICK_INTERVAL_MILLIS = 1000L;
    private static final String NOTIFICATION_CHANNEL_ID = "timer_alerts";
    private static final int NOTIFICATION_ID_BASE = 4100;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            updateActiveTimer();
        }
    };
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                pauseActiveTimer();
            }
        }
    };

    private WindowManager windowManager;
    private TextView timerOverlay;
    private WindowManager.LayoutParams overlayParams;
    private String activePackage;
    private String lastBlockedPackage;
    private boolean screenReceiverRegistered;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
        screenReceiverRegistered = true;
        createNotificationChannel();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName() == null) {
            return;
        }

        String packageName = event.getPackageName().toString();
        if (getPackageName().equals(packageName)) {
            pauseActiveTimer();
            lastBlockedPackage = null;
            return;
        }

        if (!packageName.equals(activePackage)) {
            pauseActiveTimer();
            lastBlockedPackage = null;
        }

        long blockedUntil = TimerStore.getOrStartLock(this, packageName,
                System.currentTimeMillis());
        if (blockedUntil > System.currentTimeMillis()) {
            showBlockedScreen(packageName, blockedUntil);
            return;
        }

        TimerStore.Entry entry = TimerStore.get(this, packageName);
        if (entry == null) {
            activePackage = null;
            hideTimerOverlay();
            return;
        }

        TimerStore.startTimer(this, packageName, System.currentTimeMillis());
        activePackage = packageName;
        updateActiveTimer();
    }

    @Override
    public void onInterrupt() {
        pauseActiveTimer();
    }

    @Override
    public void onDestroy() {
        pauseActiveTimer();
        handler.removeCallbacks(timerTick);
        hideTimerOverlay();
        if (screenReceiverRegistered) {
            unregisterReceiver(screenReceiver);
            screenReceiverRegistered = false;
        }
        super.onDestroy();
    }

    private void updateActiveTimer() {
        handler.removeCallbacks(timerTick);
        if (activePackage == null) {
            hideTimerOverlay();
            return;
        }

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager == null || !powerManager.isInteractive()) {
            pauseActiveTimer();
            return;
        }

        long now = System.currentTimeMillis();
        TimerStore.Entry entry = TimerStore.get(this, activePackage);
        if (entry == null) {
            activePackage = null;
            hideTimerOverlay();
            return;
        }

        if (entry.remainingMillis <= 0L) {
            String expiredPackage = activePackage;
            long blockedUntil = TimerStore.getOrStartLock(this, expiredPackage, now);
            activePackage = null;
            hideTimerOverlay();
            if (blockedUntil > now) {
                showBlockedScreen(expiredPackage, blockedUntil);
            }
            return;
        }

        if (entry.remainingMillis <= WARNING_WINDOW_MILLIS && !entry.warningSent
                && showTimerNotification(entry, entry.remainingMillis)) {
            TimerStore.markWarningSent(this, entry.packageName);
        }

        showTimerOverlay(entry, entry.remainingMillis);
        handler.postDelayed(timerTick, TICK_INTERVAL_MILLIS);
    }

    private void pauseActiveTimer() {
        handler.removeCallbacks(timerTick);
        if (activePackage != null) {
            String packageToPause = activePackage;
            TimerStore.pauseTimer(this, packageToPause, System.currentTimeMillis());
            activePackage = null;
        }
        hideTimerOverlay();
    }

    private void showBlockedScreen(String packageName, long blockedUntil) {
        if (packageName.equals(lastBlockedPackage)) {
            return;
        }
        TimerStore.Entry entry = TimerStore.get(this, packageName);
        if (entry == null) {
            return;
        }

        lastBlockedPackage = packageName;
        Intent intent = new Intent(this, BlockedScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(EXTRA_DISPLAY_NAME, entry.displayName);
        intent.putExtra(EXTRA_BLOCKED_UNTIL, blockedUntil);
        startActivity(intent);
    }

    private void showTimerOverlay(TimerStore.Entry entry, long remainingMillis) {
        if (windowManager == null) {
            return;
        }
        if (timerOverlay == null) {
            timerOverlay = new TextView(this);
            timerOverlay.setTextColor(0xFFFFFFFF);
            timerOverlay.setTextSize(14);
            int padding = dp(12);
            timerOverlay.setPadding(padding, dp(8), padding, dp(8));
            GradientDrawable background = new GradientDrawable();
            background.setColor(0xE6315B4C);
            background.setCornerRadius(dp(8));
            timerOverlay.setBackground(background);

            overlayParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            overlayParams.gravity = Gravity.TOP | Gravity.END;
            overlayParams.x = dp(12);
            overlayParams.y = dp(72);
            try {
                windowManager.addView(timerOverlay, overlayParams);
            } catch (WindowManager.BadTokenException | IllegalStateException exception) {
                timerOverlay = null;
                overlayParams = null;
                return;
            }
        }

        long seconds = Math.max(0L, (remainingMillis + 999L) / 1000L);
        String timeLeft = android.text.format.DateUtils.formatElapsedTime(seconds);
        timerOverlay.setText(getString(R.string.overlay_timer, entry.displayName, timeLeft));
    }

    private void hideTimerOverlay() {
        if (timerOverlay != null && windowManager != null) {
            try {
                windowManager.removeView(timerOverlay);
            } catch (IllegalArgumentException ignored) {
            }
        }
        timerOverlay = null;
        overlayParams = null;
    }

    private boolean showTimerNotification(TimerStore.Entry entry, long remainingMillis) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return false;
        }

        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                entry.packageName.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long seconds = Math.max(0L, (remainingMillis + 999L) / 1000L);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_content,
                        entry.displayName, android.text.format.DateUtils.formatElapsedTime(seconds)))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
        notificationManager.notify(NOTIFICATION_ID_BASE + Math.abs(entry.packageName.hashCode() % 1000),
                notification);
        return true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(getString(R.string.notification_channel_description));
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
