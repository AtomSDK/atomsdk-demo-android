/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.common.logger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atom.core.exceptions.AtomException;
import com.atom.core.models.AtomConfiguration;
import com.atom.sdk.android.ConnectionDetails;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.vpn.demo.common.logger.LogStore.Level;

/**
 * Turns VPN state callbacks into {@link LogStore} entries. Registered once for the life of the
 * app, so events are recorded whichever screen is showing.
 */
public class SdkLogRecorder implements VPNStateListener {

    private final LogStore store = LogStore.get();

    @Override
    public void onStateChange(String state) {
        if (state != null) {
            store.add(Level.INFO, "State changed: " + state);
        }
    }

    @Override
    public void onConnecting() {
        store.add(Level.INFO, "Attempting connection.");
    }

    @Override
    public void onConnecting(VPNProperties vpnProperties, AtomConfiguration atomConfiguration) {
        store.add(Level.INFO, "Attempting connection.");
    }

    @Override
    public void onConnected() {
        store.add(Level.SUCCESS, "VPN connected.");
    }

    @Override
    public void onConnected(ConnectionDetails connectionDetails) {
        String server = connectionDetails == null ? null : connectionDetails.getServerAddress();
        store.add(Level.SUCCESS, server == null || server.isEmpty()
                ? "VPN connected."
                : "VPN connected to " + server + ".");
    }

    @Override
    public void onRedialing(AtomException atomException, ConnectionDetails connectionDetails) {
        store.add(Level.WARN, "Redialing. " + describe(atomException));
    }

    @Override
    public void onDialError(AtomException atomException, ConnectionDetails connectionDetails) {
        store.add(Level.ERROR, "Dial error. " + describe(atomException));
    }

    @Override
    public void onPaused(AtomException atomException, ConnectionDetails connectionDetails) {
        store.add(Level.WARN, "VPN paused.");
    }

    @Override
    public void onDisconnecting(ConnectionDetails connectionDetails) {
        store.add(Level.INFO, "Initiating graceful disconnect.");
    }

    @Override
    public void onDisconnected(boolean isCancelled) {
        store.add(Level.INFO, isCancelled ? "Connection cancelled." : "VPN disconnected.");
    }

    @Override
    public void onDisconnected(ConnectionDetails connectionDetails) {
        store.add(Level.INFO, connectionDetails != null && connectionDetails.isCancelled()
                ? "Connection cancelled."
                : "VPN disconnected.");
    }

    @Override
    public void onUnableToAccessInternet(AtomException atomException,
                                         ConnectionDetails connectionDetails) {
        store.add(Level.WARN, "Unable to access the internet. " + describe(atomException));
    }

    @Override
    public void onPacketsTransmitted(String in, String out, String inTotal, String outTotal) {
        // Fires every second while connected; logging it would drown out everything else.
    }

    @NonNull
    private static String describe(@Nullable AtomException e) {
        if (e == null) {
            return "";
        }
        String message = e.getMessage() == null ? "" : e.getMessage();
        return message + " Code: " + e.getCode();
    }
}
