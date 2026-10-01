package com.example.satistimer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.satistimer.data.TimerStore;
import com.google.android.material.button.MaterialButton;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ParentalControlActivity extends AppCompatActivity {
    private final List<ResolveInfo> launchableApps = new ArrayList<>();
    private final List<CheckBox> appChecks = new ArrayList<>();
    private final List<TextView> appNames = new ArrayList<>();
    private LinearLayout appList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View content = createContent();
        setContentView(content);
        SystemBarInsets.apply(this, content);
        loadApps();
        renderApps();
    }

    private View createContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(20), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText(R.string.add_apps);
        title.setTextSize(24);
        title.setTextColor(getColor(R.color.color_primary));
        page.addView(title);

        TextView description = new TextView(this);
        description.setText(R.string.app_picker_description);
        description.setTextSize(14);
        description.setPadding(0, dp(6), 0, dp(12));
        page.addView(description);

        ScrollView scrollView = new ScrollView(this);
        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(appList);
        page.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        MaterialButton saveButton = new MaterialButton(this);
        saveButton.setText(R.string.save_apps);
        saveButton.setOnClickListener(view -> saveSelection());
        page.addView(saveButton);
        return page;
    }

    private void loadApps() {
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager packageManager = getPackageManager();
        launchableApps.addAll(packageManager.queryIntentActivities(launcherIntent, 0));
        Iterator<ResolveInfo> iterator = launchableApps.iterator();
        while (iterator.hasNext()) {
            if (getPackageName().equals(iterator.next().activityInfo.packageName)) {
                iterator.remove();
            }
        }
        Collator collator = Collator.getInstance();
        Collections.sort(launchableApps, (left, right) -> collator.compare(
                left.loadLabel(packageManager).toString(), right.loadLabel(packageManager).toString()));
    }

    private void renderApps() {
        for (ResolveInfo info : launchableApps) {
            String packageName = info.activityInfo.packageName;
            TimerStore.Entry entry = TimerStore.get(this, packageName);

            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            CheckBox checkBox = new CheckBox(this);
            checkBox.setChecked(entry != null);
            appChecks.add(checkBox);
            row.addView(checkBox);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(info.loadIcon(getPackageManager()));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            iconParams.leftMargin = dp(8);
            row.addView(icon, iconParams);

            TextView name = new TextView(this);
            name.setText(info.loadLabel(getPackageManager()));
            name.setTextSize(16);
            name.setMaxLines(2);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            nameParams.leftMargin = dp(12);
            row.addView(name, nameParams);
            appNames.add(name);
            row.setOnClickListener(view -> checkBox.setChecked(!checkBox.isChecked()));
            appList.addView(row);
        }
    }

    private void saveSelection() {
        Set<String> selectedPackages = new HashSet<>();
        for (int index = 0; index < launchableApps.size(); index++) {
            if (appChecks.get(index).isChecked()) {
                selectedPackages.add(launchableApps.get(index).activityInfo.packageName);
                String packageName = launchableApps.get(index).activityInfo.packageName;
                if (TimerStore.get(this, packageName) == null) {
                    TimerStore.add(this, packageName, appNames.get(index).getText().toString());
                }
            }
        }
        for (TimerStore.Entry entry : TimerStore.getAll(this)) {
            if (!selectedPackages.contains(entry.packageName)) {
                TimerStore.remove(this, entry.packageName);
            }
        }
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
