/*
 * Copyright (c) 2018 Atom SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.common;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.atom.sdk.android.AtomManager;
import com.atom.vpn.demo.AtomDemoApplicationController;

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

        button.postDelayed(() -> {
            try {
                if (button != null) {
                    button.setText(text);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1000);
    }

    public static void changeButtonText(Context context, Button button) {
        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(context).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
                if (button != null) {
                    button.setText("Disconnect");
                }
            } else if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(context).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING)) {
                if (button != null) {
                    button.setText("Cancel");
                }
            } else {
                if (button != null) {
                    button.setText("Connect");
                }
            }

        }
    }

}
