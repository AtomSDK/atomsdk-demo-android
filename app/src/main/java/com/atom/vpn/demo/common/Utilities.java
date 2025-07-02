/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.common;

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

    public static void getPauseTimerList(Activity activity, AtomDemoAppCallback<PauseVPNTimer> callback) {
        List<PauseVPNTimer> l = Arrays.asList(
                PauseVPNTimer.MINUTES_5, PauseVPNTimer.MINUTES_10, PauseVPNTimer.MINUTES_15,
                PauseVPNTimer.MINUTES_30, PauseVPNTimer.MINUTES_60, PauseVPNTimer.MANUAL
        );
        String[] times = l.stream().map(Enum::toString).toArray(String[]::new);
        UIHelper.showListDialogBox(activity, "Pause VPN for:", times, (dialog, which) -> {
            callback.invoke(l.get(which));
        });
    }

    public static void changeButtonText(Context context, Button button) {
        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            if(AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(context).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)){
                if (button != null) {
                    button.setText("Disconnect");
                }
            }else if(AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(context).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING)){
                if (button != null) {
                    button.setText("Cancel");
                }
            }else{
                if (button != null) {
                    button.setText("Connect");
                }
            }

        }
    }

}
