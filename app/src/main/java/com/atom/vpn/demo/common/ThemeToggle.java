/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.common;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.atom.vpn.demo.R;

/**
 * The sun / moon button in the top bars (view_theme_toggle.xml). The theme is AppCompat DayNight
 * with values-night colours, so switching is only a night mode change: AppCompat recreates the
 * running activities itself.
 */
public final class ThemeToggle {

    private ThemeToggle() {
    }

    /** Applies the saved choice; call before any activity is created. */
    public static void apply(@NonNull Context context) {
        AppCompatDelegate.setDefaultNightMode(DemoSettings.get(context).getNightMode());
    }

    /** Wires the toggle inside {@code root}, if it has one. */
    public static void bind(@NonNull View root) {
        ImageView button = root.findViewById(R.id.btnThemeToggle);
        if (button == null) {
            return;
        }

        boolean dark = isDark(button.getContext());
        // The icon shows where a tap goes: the moon while light, the sun while dark.
        button.setImageResource(dark ? R.drawable.sun : R.drawable.moon);
        button.setContentDescription(button.getContext().getString(
                dark ? R.string.switch_to_light_theme : R.string.switch_to_dark_theme));
        button.setOnClickListener(v -> {
            int mode = dark ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
            DemoSettings.get(v.getContext()).setNightMode(mode);
            AppCompatDelegate.setDefaultNightMode(mode);
        });
        CoachMarks.set(button, R.string.coach_theme_toggle);
    }

    private static boolean isDark(@NonNull Context context) {
        int night = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }
}
