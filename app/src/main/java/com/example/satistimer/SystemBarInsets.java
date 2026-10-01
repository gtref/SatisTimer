package com.example.satistimer;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;

final class SystemBarInsets {
    private SystemBarInsets() {
    }

    static void apply(Activity activity, View root) {
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        boolean darkTheme = (activity.getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(activity.getWindow(), root);
        controller.setAppearanceLightStatusBars(!darkTheme);
        controller.setAppearanceLightNavigationBars(!darkTheme);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(left + systemBars.left, top + systemBars.top,
                    right + systemBars.right, bottom + systemBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}