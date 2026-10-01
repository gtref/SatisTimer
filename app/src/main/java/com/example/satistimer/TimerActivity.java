package com.example.satistimer;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.satistimer.data.TimerStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TimerActivity extends AppCompatActivity {
    public static final String EXTRA_PACKAGE_NAME = "com.example.satistimer.extra.PACKAGE_NAME";
    public static final String EXTRA_DISPLAY_NAME = "com.example.satistimer.extra.DISPLAY_NAME";

    private static final long[] TIMER_DURATIONS = {
            1L * 60L * 1000L,
            5L * 60L * 1000L,
            15L * 60L * 1000L,
            30L * 60L * 1000L,
            60L * 60L * 1000L,
            3L * 60L * 60L * 1000L,
            6L * 60L * 60L * 1000L
    };
    private static final String[] TIMER_LABELS = {
            "1 minute", "5 minutes", "15 minutes", "30 minutes", "1 hour", "3 hours", "6 hours"
    };
    private static final long[] CYCLE_DURATIONS = {
            TimerStore.TEST_CYCLE,
            TimerStore.ONE_DAY, TimerStore.TWO_DAYS, TimerStore.ONE_WEEK
    };
        private static final String[] CYCLE_LABELS = {"1 minute (test)", "1 day", "2 days", "1 week"};

    private String packageName;
    private String displayName;
    private int selectedDuration;
    private int selectedCycle;
    private TextView durationValue;
    private TextView cycleValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        displayName = getIntent().getStringExtra(EXTRA_DISPLAY_NAME);
        if (packageName == null || displayName == null) {
            finish();
            return;
        }

        TimerStore.Entry existing = TimerStore.get(this, packageName);
        if (existing != null) {
            for (int index = 0; index < CYCLE_DURATIONS.length; index++) {
                if (existing.cycleDurationMillis == CYCLE_DURATIONS[index]) {
                    selectedCycle = index;
                    break;
                }
            }
        }
        View content = createContent();
        setContentView(content);
        SystemBarInsets.apply(this, content);
        updateValues();
    }

    private View createContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText(getString(R.string.timer_title, displayName));
        title.setTextSize(24);
        title.setTextColor(getColor(R.color.color_primary));
        page.addView(title);

        TextView durationLabel = new TextView(this);
        durationLabel.setText(R.string.timer_duration_label);
        durationLabel.setTextSize(15);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(28);
        page.addView(durationLabel, labelParams);

        durationValue = new TextView(this);
        durationValue.setTextSize(18);
        durationValue.setPadding(0, dp(8), 0, dp(8));
        page.addView(durationValue);

        MaterialButton durationButton = new MaterialButton(this);
        durationButton.setText(R.string.choose_duration);
        durationButton.setOnClickListener(view -> chooseDuration());
        page.addView(durationButton);

        TextView cycleLabel = new TextView(this);
        cycleLabel.setText(R.string.lock_cycle_label);
        cycleLabel.setTextSize(15);
        LinearLayout.LayoutParams cycleLabelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cycleLabelParams.topMargin = dp(24);
        page.addView(cycleLabel, cycleLabelParams);

        cycleValue = new TextView(this);
        cycleValue.setTextSize(18);
        cycleValue.setPadding(0, dp(8), 0, dp(8));
        page.addView(cycleValue);

        MaterialButton cycleButton = new MaterialButton(this);
        cycleButton.setText(R.string.choose_lock_cycle);
        cycleButton.setOnClickListener(view -> chooseCycle());
        page.addView(cycleButton);

        View spacer = new View(this);
        page.addView(spacer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        MaterialButton saveButton = new MaterialButton(this);
        saveButton.setText(R.string.save_timer);
        saveButton.setOnClickListener(view -> saveTimer());
        page.addView(saveButton);

        MaterialButton cancelButton = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        cancelButton.setText(R.string.cancel);
        cancelButton.setOnClickListener(view -> finish());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cancelParams.topMargin = dp(8);
        page.addView(cancelButton, cancelParams);
        return page;
    }

    private void chooseDuration() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.timer_duration_label)
                .setSingleChoiceItems(TIMER_LABELS, selectedDuration, (dialog, which) -> {
                    selectedDuration = which;
                    dialog.dismiss();
                    updateValues();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chooseCycle() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lock_cycle_label)
                .setSingleChoiceItems(CYCLE_LABELS, selectedCycle, (dialog, which) -> {
                    selectedCycle = which;
                    dialog.dismiss();
                    updateValues();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateValues() {
        durationValue.setText(TIMER_LABELS[selectedDuration]);
        cycleValue.setText(CYCLE_LABELS[selectedCycle]);
    }

    private void saveTimer() {
        TimerStore.setTimer(this, packageName, displayName,
                TIMER_DURATIONS[selectedDuration], CYCLE_DURATIONS[selectedCycle]);
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
