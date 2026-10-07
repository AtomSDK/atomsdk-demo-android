/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.common;

import com.atom.vpn.demo.R;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.PauseVPNTimer;
import com.atom.vpn.demo.AtomDemoAppCallback;
import com.atom.vpn.demo.AtomDemoApplicationController;

import java.util.Arrays;
import java.util.List;

public class Utilities {

    public static void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }


    public static void changeButtonState(Button button, String text) {

        if(button!=null) {
            button.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (button != null) {
                            button.setText(text);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 1000);
        }
    }

    /** Pause durations offered to the user, in the order of R.array.pause_timer_labels. */
    private static final List<PauseVPNTimer> PAUSE_TIMERS = Arrays.asList(
            PauseVPNTimer.MINUTES_5, PauseVPNTimer.MINUTES_10, PauseVPNTimer.MINUTES_15,
            PauseVPNTimer.MINUTES_30, PauseVPNTimer.MINUTES_60, PauseVPNTimer.MANUAL
    );

    /** Human-readable label for a pause duration, e.g. "5 minutes" rather than MINUTES_5. */
    public static String pauseTimerLabel(Context context, PauseVPNTimer timer) {
        int index = PAUSE_TIMERS.indexOf(timer);
        return index < 0 ? timer.toString()
                : context.getResources().getStringArray(R.array.pause_timer_labels)[index];
    }

    public static void getPauseTimerList(Activity activity, AtomDemoAppCallback<PauseVPNTimer> callback) {
        List<PauseVPNTimer> l = PAUSE_TIMERS;
        String[] times = activity.getResources().getStringArray(R.array.pause_timer_labels);
        UIHelper.showListDialogBox(activity, activity.getString(R.string.pause_vpn_for), times, (dialog, which) -> {
            callback.invoke(l.get(which));
        });
    }

}
