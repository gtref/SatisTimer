package com.example.satistimer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class BlockedScreenActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable countdownUpdater = new Runnable() {
        @Override
        public void run() {
            updateCountdown();
            if (blockedUntilMillis > System.currentTimeMillis()) {
                handler.postDelayed(this, 1000L);
            } else {
                finish();
            }
        }
    };

    private String displayName;
    private long blockedUntilMillis;
    private TextView countdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readIntent(getIntent());
        View content = createContent();
        setContentView(content);
        SystemBarInsets.apply(this, content);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readIntent(intent);
        updateCountdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(countdownUpdater);
        handler.post(countdownUpdater);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(countdownUpdater);
        super.onPause();
    }

    private void readIntent(Intent intent) {
        displayName = intent.getStringExtra(BlockService.EXTRA_DISPLAY_NAME);
        blockedUntilMillis = intent.getLongExtra(BlockService.EXTRA_BLOCKED_UNTIL, 0L);
    }

    private View createContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(28), dp(28), dp(28), dp(28));

        TextView title = new TextView(this);
        title.setText(R.string.app_blocked_title);
        title.setTextSize(28);
        title.setTextColor(getColor(R.color.color_primary));
        title.setGravity(Gravity.CENTER);
        page.addView(title);

        TextView app = new TextView(this);
        app.setText(getString(R.string.app_blocked_message, displayName));
        app.setTextSize(17);
        app.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams appParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        appParams.topMargin = dp(16);
        page.addView(app, appParams);

        countdown = new TextView(this);
        countdown.setTextSize(20);
        countdown.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams countdownParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        countdownParams.topMargin = dp(16);
        page.addView(countdown, countdownParams);

        TextView note = new TextView(this);
        note.setText(R.string.prototype_warning);
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(28);
        page.addView(note, noteParams);

        MaterialButton dashboardButton = new MaterialButton(this);
        dashboardButton.setText(R.string.open_dashboard);
        dashboardButton.setOnClickListener(view -> openDashboard());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(24);
        page.addView(dashboardButton, buttonParams);
        return page;
    }

    private void updateCountdown() {
        if (countdown == null) {
            return;
        }
        long remaining = Math.max(0L, blockedUntilMillis - System.currentTimeMillis());
        long totalSeconds = remaining / 1000L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (days > 0L) {
            countdown.setText(getString(R.string.blocked_countdown_days,
                    days, hours, minutes, seconds));
        } else {
            countdown.setText(getString(R.string.blocked_countdown,
                    hours, minutes, seconds));
        }
    }

    private void openDashboard() {
        Intent intent = new Intent(this, AppSelectorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
