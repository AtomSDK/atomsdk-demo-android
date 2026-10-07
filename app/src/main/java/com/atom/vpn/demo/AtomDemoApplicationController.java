/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo;

import com.atom.vpn.demo.common.ThemeToggle;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.TextUtils;
import android.widget.Toast;


import androidx.multidex.MultiDexApplication;

import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.AtomConfiguration;
import com.atom.core.models.AtomNotification;
import com.atom.sdk.android.AtomManager;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.DemoSettings;
import com.atom.vpn.demo.common.logger.LogStore;
import com.atom.vpn.demo.common.logger.SdkLogRecorder;
import de.blinkt.openvpn.BuildConfig;

import java.util.ArrayList;
import java.util.List;


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

    /**
     * AtomManager.initialize() is asynchronous, so a screen that shows the SDK state must hear
     * when it finishes rather than only reading getAtomManager() when it is created. Called on
     * the main thread; if the SDK is already initialized it is called straight away.
     */
    public interface OnInitializedListener {
        void onAtomManagerInitialized(AtomManager atomManager);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<OnInitializedListener> initializedListeners = new ArrayList<>();

    public void addOnInitializedListener(OnInitializedListener listener) {
        if (atomManager != null) {
            listener.onAtomManagerInitialized(atomManager);
            return;
        }
        initializedListeners.add(listener);
    }

    public void removeOnInitializedListener(OnInitializedListener listener) {
        initializedListeners.remove(listener);
    }

    private void notifyInitialized(AtomManager manager) {
        mainHandler.post(() -> {
            atomManager = manager;
            List<OnInitializedListener> listeners = new ArrayList<>(initializedListeners);
            initializedListeners.clear();
            for (OnInitializedListener listener : listeners) {
                listener.onAtomManagerInitialized(manager);
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        ThemeToggle.apply(this);
        //put ATOM Application Secret here
        String ATOM_SECRET_KEY = getString(R.string.atom_secret_key);

        if (!TextUtils.isEmpty(ATOM_SECRET_KEY)) {

            // configure the ATOM SDK
            AtomConfiguration.Builder atomConfigurationBuilder = new AtomConfiguration.Builder(ATOM_SECRET_KEY);
            atomConfigurationBuilder.setVpnInterfaceName("Atom SDK Demo");
            AtomNotification.Builder atomNotificationBuilder = new AtomNotification.Builder(NOTIFICATION_ID,"Atom SDK Demo","You are now secured with Atom",R.drawable.ic_stat_icn_connected, Color.BLUE);
            atomConfigurationBuilder.setNotification(atomNotificationBuilder.build());
            atomConfigurationBuilder.enableVPNPause();
            // Persist VPN Details, from More > Connection Features. Read here only, so a change
            // takes effect from the next app launch.
            atomConfigurationBuilder.persistVPNDetails(
                    DemoSettings.get(this).isPersistVpnDetailsEnabled());
            AtomConfiguration atomConfiguration = atomConfigurationBuilder.build();
            try {
                AtomManager.initialize(this, atomConfiguration, new AtomManager.InitializeCallback() {
                    @Override
                    public void onInitialized(AtomManager mAtomManager) {
                        atomManager = mAtomManager;
                        notifyInitialized(mAtomManager);
                        LogStore.get().add(LogStore.Level.SUCCESS,
                                "SDK initialized successfully. Configuration loaded.");
                    }
                });
            } catch (AtomValidationException e) {
                e.printStackTrace();
                LogStore.get().add(LogStore.Level.ERROR,
                        "SDK configuration invalid: " + e.getMessage());
            }catch (Exception e) {
                e.printStackTrace();
                LogStore.get().add(LogStore.Level.ERROR,
                        "SDK initialization failed: " + e.getMessage());
            }
            // Registered once for the app's lifetime so the Logs screen has history when opened.
            AtomManager.addVPNStateListener(new SdkLogRecorder());
        } else {
            Toast.makeText(this, Constants.SecretKeyRequired, Toast.LENGTH_SHORT).show();
        }

    }

}
