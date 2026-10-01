package com.example.satistimer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.satistimer.data.TimerStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.card.MaterialCardView;

import java.util.Collections;
import java.util.List;

public class AppSelectorActivity extends AppCompatActivity {
    private static final int REQUEST_POST_NOTIFICATIONS = 42;
    private static final long REFRESH_INTERVAL_MILLIS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            refreshContent();
            handler.postDelayed(this, REFRESH_INTERVAL_MILLIS);
        }
    };

    private LinearLayout appList;
    private TextView serviceStatus;
    private MaterialButton serviceButton;
    private MaterialButton notificationButton;
    private boolean openAccessibilityAfterPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View content = createContent();
        setContentView(content);
        SystemBarInsets.apply(this, content);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshContent();
        handler.removeCallbacks(refreshTask);
        handler.postDelayed(refreshTask, REFRESH_INTERVAL_MILLIS);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshTask);
        super.onPause();
    }

    private View createContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(20), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        title.setTextColor(getColor(R.color.color_primary));
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.dashboard_subtitle);
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        page.addView(subtitle);

        serviceStatus = new TextView(this);
        serviceStatus.setTextSize(14);
        serviceStatus.setPadding(0, 0, 0, dp(8));
        page.addView(serviceStatus);

        serviceButton = new MaterialButton(this);
        serviceButton.setText(R.string.enable_blocking);
        serviceButton.setOnClickListener(view -> requestBlockingSetup());
        page.addView(serviceButton);

        notificationButton = new MaterialButton(this, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle);
        notificationButton.setText(R.string.enable_timer_alerts);
        notificationButton.setOnClickListener(view -> requestNotificationPermission(false));
        LinearLayout.LayoutParams notificationParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        notificationParams.topMargin = dp(8);
        page.addView(notificationButton, notificationParams);

        MaterialButton addAppsButton = new MaterialButton(this);
        addAppsButton.setText(R.string.add_apps);
        addAppsButton.setOnClickListener(view ->
                startActivity(new Intent(this, ParentalControlActivity.class)));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        addParams.topMargin = dp(8);
        page.addView(addAppsButton, addParams);

        ScrollView scrollView = new ScrollView(this);
        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        listParams.topMargin = dp(16);
        scrollView.addView(appList);
        page.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        TextView versionLabel = new TextView(this);
        versionLabel.setText(getString(R.string.version_label, BuildConfig.VERSION_NAME));
        versionLabel.setTextSize(12);
        versionLabel.setTextColor(getColor(R.color.color_secondary));
        versionLabel.setGravity(Gravity.END);
        versionLabel.setPadding(0, dp(8), 0, 0);
        page.addView(versionLabel);
        return page;
    }

    private void requestBlockingSetup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            openAccessibilitySettings();
            return;
        }
        requestNotificationPermission(true);
    }

    private void requestNotificationPermission(boolean continueToAccessibility) {
        openAccessibilityAfterPermission = continueToAccessibility;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            if (openAccessibilityAfterPermission) {
                openAccessibilitySettings();
            }
            return;
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.notification_permission_title)
                    .setMessage(R.string.notification_permission_message)
                    .setPositiveButton(R.string.continue_button, (dialog, which) ->
                            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    REQUEST_POST_NOTIFICATIONS))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_POST_NOTIFICATIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (openAccessibilityAfterPermission) {
                openAccessibilityAfterPermission = false;
                openAccessibilitySettings();
            }
            refreshContent();
        }
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void refreshContent() {
        boolean enabled = isBlockServiceEnabled();
        serviceStatus.setText(getString(
            enabled ? R.string.blocking_enabled : R.string.blocking_disabled));
        serviceButton.setVisibility(enabled ? View.GONE : View.VISIBLE);
        boolean notificationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
        notificationButton.setVisibility(notificationsEnabled ? View.GONE : View.VISIBLE);

        appList.removeAllViews();
        List<TimerStore.Entry> entries = TimerStore.getAll(this);
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_apps_yet);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(36), 0, dp(36));
            appList.addView(empty);
            return;
        }

        Collections.sort(entries, (left, right) ->
            left.displayName.compareToIgnoreCase(right.displayName));
        for (TimerStore.Entry entry : entries) {
            appList.addView(createAppRow(entry));
        }
    }

    private View createAppRow(TimerStore.Entry entry) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(dp(1));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(12));

        ImageView appIcon = new ImageView(this);
        appIcon.setContentDescription(entry.displayName);
        try {
            appIcon.setImageDrawable(getPackageManager().getApplicationIcon(entry.packageName));
        } catch (PackageManager.NameNotFoundException exception) {
            appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        iconParams.bottomMargin = dp(8);
        content.addView(appIcon, iconParams);

        TextView appName = new TextView(this);
        appName.setText(entry.displayName);
        appName.setTextSize(18);
        appName.setTextColor(getColor(R.color.color_primary));
        content.addView(appName);

        TextView timerStatus = new TextView(this);
        timerStatus.setText(getTimerStatus(entry));
        timerStatus.setTextSize(14);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(3);
        content.addView(timerStatus, statusParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        MaterialButton timerButton = new MaterialButton(this);
        timerButton.setText(R.string.set_timer);
        timerButton.setOnClickListener(view -> openTimer(entry));
        actions.addView(timerButton);

        MaterialButton removeButton = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        removeButton.setText(R.string.remove_app);
        removeButton.setOnClickListener(view -> {
            TimerStore.remove(this, entry.packageName);
            refreshContent();
        });
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        removeParams.leftMargin = dp(8);
        actions.addView(removeButton, removeParams);
        content.addView(actions);

        card.addView(content);
        return card;
    }

    private String getTimerStatus(TimerStore.Entry entry) {
        long now = System.currentTimeMillis();
        if (entry.blockedUntilMillis > now) {
            return getString(R.string.blocked_for, DateUtils.getRelativeTimeSpanString(
                    entry.blockedUntilMillis, now, DateUtils.MINUTE_IN_MILLIS));
        }
        if (entry.remainingMillis > 0L) {
            String remaining = DateUtils.formatElapsedTime(entry.remainingMillis / 1000L);
            return getString(entry.activeSinceMillis > 0L
                    ? R.string.timer_left : R.string.timer_ready, remaining);
        }
        if (entry.timerDurationMillis > 0L) {
            return getString(R.string.timer_expired);
        }
        return getString(R.string.no_timer_set);
    }

    private void openTimer(TimerStore.Entry entry) {
        Intent intent = new Intent(this, TimerActivity.class);
        intent.putExtra(TimerActivity.EXTRA_PACKAGE_NAME, entry.packageName);
        intent.putExtra(TimerActivity.EXTRA_DISPLAY_NAME, entry.displayName);
        startActivity(intent);
    }

    private boolean isBlockServiceEnabled() {
        String enabledServices = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String target = new ComponentName(this, BlockService.class).flattenToString();
        return enabledServices != null && enabledServices.contains(target);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
