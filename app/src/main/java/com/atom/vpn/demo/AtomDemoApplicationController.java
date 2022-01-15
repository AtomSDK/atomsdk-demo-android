/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo;

import android.graphics.Color;
import android.os.StrictMode;
import android.text.TextUtils;
import android.widget.Toast;


import androidx.multidex.MultiDexApplication;

import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.AtomConfiguration;
import com.atom.core.models.AtomNotification;
import com.atom.sdk.android.AtomManager;
import com.atom.vpn.demo.common.Constants;
import de.blinkt.openvpn.BuildConfig;


/**
 * AtomDemoApplicationController
 */

public class AtomDemoApplicationController extends MultiDexApplication {

    private static AtomDemoApplicationController mInstance;
    private AtomManager atomManager;

    private final int NOTIFICATION_ID = com.atom.sdk.android.common.Constants.Notification.DEFAULT_ID;

    public static AtomDemoApplicationController getInstance() {
        return mInstance;
    }

    public AtomManager getAtomManager() {
        return atomManager;
    }

    public void setAtomManager(AtomManager atomManager) {
        this.atomManager = atomManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        //put ATOM Application Secret here
        String ATOM_SECRET_KEY = getString(R.string.atom_secret_key);

        if (!TextUtils.isEmpty(ATOM_SECRET_KEY)) {

            // configure the ATOM SDK
            AtomConfiguration.Builder atomConfigurationBuilder = new AtomConfiguration.Builder(ATOM_SECRET_KEY);
            atomConfigurationBuilder.setVpnInterfaceName("Atom SDK Demo");
            AtomNotification.Builder atomNotificationBuilder = new AtomNotification.Builder(NOTIFICATION_ID,"Atom SDK Demo","You are now secured with Atom",R.drawable.ic_stat_icn_connected, Color.BLUE);
            atomConfigurationBuilder.setNotification(atomNotificationBuilder.build());
            AtomConfiguration atomConfiguration = atomConfigurationBuilder.build();
            try {
                AtomManager.initialize(this, atomConfiguration, new AtomManager.InitializeCallback() {
                    @Override
                    public void onInitialized(AtomManager mAtomManager) {
                        atomManager = mAtomManager;
                    }
                });
            } catch (AtomValidationException e) {
                e.printStackTrace();
            }catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, Constants.SecretKeyRequired, Toast.LENGTH_SHORT).show();
        }

    }

}
