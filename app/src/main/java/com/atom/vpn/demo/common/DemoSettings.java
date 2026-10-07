/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.atom.core.models.Protocol;
import com.atom.sdk.android.ServerFilter;
import com.atom.sdk.android.ServerFilterType;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.shield.states.AtomShieldFeature;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The connection features chosen on the More screen (VPN Protocol, Connection Features and Split
 * Tunneling), kept in SharedPreferences and applied to every VPNProperties the connect screens
 * build.
 */
public final class DemoSettings {

    public enum SplitTunnelMode {
        /** Every app goes through the VPN. */
        OFF,
        /** withReverseSplitTunneling: the selected apps bypass the VPN. */
        EXCLUDE,
        /** withSplitTunneling: only the selected apps (plus this app) use the VPN. */
        ONLY
    }

    /** Which connect screen is dialling; decides which features the SDK accepts for it. */
    public enum Dialing {
        PARAMS, DEDICATED_IP, CHANNEL, MULTIPLE_DEDICATED_IP
    }

    public static final int DEFAULT_MANUAL_PORT = 5500;

    private static final String PREFS = "demo_settings";
    private static final String KEY_TRACKER_AD_BLOCKER = "tracker_ad_blocker";
    private static final String KEY_LAN_ACCESS = "lan_access";
    private static final String KEY_CONNECTION_METHOD = "connection_method";
    private static final String KEY_PREFERRED_PROTOCOL = "preferred_protocol";
    private static final String KEY_PREFERRED_PROTOCOL_NAME = "preferred_protocol_name";
    private static final String KEY_PROTOCOL_SWITCH = "protocol_switch";
    private static final String KEY_FAILOVER = "failover";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_AUTOMATIC_PORT = "automatic_port";
    private static final String KEY_MANUAL_PORT = "manual_port";
    private static final String KEY_SERVER_FILTER_NAS = "server_filter_nas";
    private static final String KEY_SERVER_FILTER_INCLUDE = "server_filter_include";
    private static final String KEY_PERSIST_VPN_DETAILS = "persist_vpn_details";
    private static final String KEY_SPLIT_MODE = "split_mode";
    private static final String KEY_SPLIT_LAST_MODE = "split_last_mode";
    private static final String KEY_SPLIT_APPS = "split_apps";

    private final SharedPreferences prefs;

    private DemoSettings(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public static DemoSettings get(@NonNull Context context) {
        return new DemoSettings(context);
    }

    /**
     * More > Reset SDK Configuration: every SDK option goes back to its default. The theme and
     * the Dashboard's connect screen are app preferences, not SDK configuration, so they stay.
     * Returns true if Persist VPN Details was on, which only clears at the next launch.
     */
    public boolean reset() {
        boolean persistWasOn = isPersistVpnDetailsEnabled();
        int nightMode = getNightMode();
        Dialing connectionMethod = getConnectionMethod();
        prefs.edit()
                .clear()
                .putInt(KEY_NIGHT_MODE, nightMode)
                .putString(KEY_CONNECTION_METHOD, connectionMethod.name())
                .apply();
        return persistWasOn;
    }

    // region Privacy

    public boolean isTrackerAdBlockerEnabled() {
        return prefs.getBoolean(KEY_TRACKER_AD_BLOCKER, false);
    }

    public void setTrackerAdBlockerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TRACKER_AD_BLOCKER, enabled).apply();
    }

    public boolean isLanAccessEnabled() {
        return prefs.getBoolean(KEY_LAN_ACCESS, false);
    }

    public void setLanAccessEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LAN_ACCESS, enabled).apply();
    }

    // endregion

    // region Dialing

    /** The Dashboard's selected connect screen, which the bottom nav's Connection item opens. */
    @NonNull
    public Dialing getConnectionMethod() {
        try {
            return Dialing.valueOf(prefs.getString(KEY_CONNECTION_METHOD, Dialing.PARAMS.name()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Dialing.PARAMS;
        }
    }

    public void setConnectionMethod(@NonNull Dialing method) {
        prefs.edit().putString(KEY_CONNECTION_METHOD, method.name()).apply();
    }

    /**
     * Slug ({@link Protocol#getProtocol()}) of the protocol the connect screens start with as
     * primary; empty means none, so they fall back to the first one getProtocols() returns.
     */
    @NonNull
    public String getPreferredProtocol() {
        String slug = prefs.getString(KEY_PREFERRED_PROTOCOL, "");
        return slug == null ? "" : slug;
    }

    /** Display name of the preferred protocol, kept so the More screen needs no fetch; may be empty. */
    @NonNull
    public String getPreferredProtocolName() {
        String name = prefs.getString(KEY_PREFERRED_PROTOCOL_NAME, "");
        return name == null ? "" : name;
    }

    /** Null clears the preference. */
    public void setPreferredProtocol(@Nullable Protocol protocol) {
        prefs.edit()
                .putString(KEY_PREFERRED_PROTOCOL, protocol == null ? "" : protocol.getProtocol())
                .putString(KEY_PREFERRED_PROTOCOL_NAME, protocol == null ? "" : protocol.getName())
                .apply();
    }

    /** SDK default is on. */
    public boolean isProtocolSwitchEnabled() {
        return prefs.getBoolean(KEY_PROTOCOL_SWITCH, true);
    }

    public void setProtocolSwitchEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PROTOCOL_SWITCH, enabled).apply();
    }

    /** SDK default is on. */
    public boolean isFailoverEnabled() {
        return prefs.getBoolean(KEY_FAILOVER, true);
    }

    public void setFailoverEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FAILOVER, enabled).apply();
    }

    /** An AppCompatDelegate MODE_NIGHT_* value; follows the system until the toggle is used. */
    public int getNightMode() {
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setNightMode(int mode) {
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
    }

    /**
     * Multiport. Off by default, so Connect with Params keeps dialling the manual port it always
     * used ({@link #DEFAULT_MANUAL_PORT}).
     */
    public boolean isAutomaticPortEnabled() {
        return prefs.getBoolean(KEY_AUTOMATIC_PORT, false);
    }

    public void setAutomaticPortEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTOMATIC_PORT, enabled).apply();
    }

    public int getManualPort() {
        return prefs.getInt(KEY_MANUAL_PORT, DEFAULT_MANUAL_PORT);
    }

    public void setManualPort(int port) {
        prefs.edit().putInt(KEY_MANUAL_PORT, port).apply();
    }

    /** NAS identifier of the server to include or exclude; empty means no filter. */
    @NonNull
    public String getServerFilterNas() {
        String nas = prefs.getString(KEY_SERVER_FILTER_NAS, "");
        return nas == null ? "" : nas;
    }

    public void setServerFilterNas(@NonNull String nas) {
        prefs.edit().putString(KEY_SERVER_FILTER_NAS, nas.trim()).apply();
    }

    public boolean isServerFilterInclude() {
        return prefs.getBoolean(KEY_SERVER_FILTER_INCLUDE, true);
    }

    public void setServerFilterInclude(boolean include) {
        prefs.edit().putBoolean(KEY_SERVER_FILTER_INCLUDE, include).apply();
    }

    // endregion

    // region SDK configuration

    /** Read once, when the SDK is initialised; a change applies from the next app launch. */
    public boolean isPersistVpnDetailsEnabled() {
        return prefs.getBoolean(KEY_PERSIST_VPN_DETAILS, false);
    }

    public void setPersistVpnDetailsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PERSIST_VPN_DETAILS, enabled).apply();
    }

    // endregion

    // region Split tunneling

    @NonNull
    public SplitTunnelMode getSplitTunnelMode() {
        return readMode(KEY_SPLIT_MODE, SplitTunnelMode.OFF);
    }

    public void setSplitTunnelMode(@NonNull SplitTunnelMode mode) {
        SharedPreferences.Editor editor = prefs.edit().putString(KEY_SPLIT_MODE, mode.name());
        if (mode != SplitTunnelMode.OFF) {
            editor.putString(KEY_SPLIT_LAST_MODE, mode.name());
        }
        editor.apply();
    }

    /** The mode the More screen switch turns back on: whichever was used last. */
    @NonNull
    public SplitTunnelMode getLastSplitTunnelMode() {
        return readMode(KEY_SPLIT_LAST_MODE, SplitTunnelMode.EXCLUDE);
    }

    @NonNull
    public Set<String> getSplitTunnelApps() {
        Set<String> apps = prefs.getStringSet(KEY_SPLIT_APPS, Collections.emptySet());
        // getStringSet's result must not be modified, so hand out a copy.
        return new HashSet<>(apps == null ? Collections.emptySet() : apps);
    }

    public void setSplitTunnelApps(@NonNull Set<String> packageNames) {
        prefs.edit().putStringSet(KEY_SPLIT_APPS, new HashSet<>(packageNames)).apply();
    }

    @NonNull
    private SplitTunnelMode readMode(@NonNull String key, @NonNull SplitTunnelMode fallback) {
        try {
            return SplitTunnelMode.valueOf(prefs.getString(key, fallback.name()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return fallback;
        }
    }

    // endregion

    /**
     * Applies every feature the SDK supports for this kind of dialling. Per the SDK: the
     * Tracker/Ad Blocker works with params and dedicated IP but not channels (error 5179), and
     * ports and server filters belong to connecting with params.
     */
    public void applyTo(@NonNull VPNProperties.Builder builder, @NonNull Dialing dialing) {
        builder.enableProtocolSwitch(isProtocolSwitchEnabled());
        builder.withUseFailoverEnabled(isFailoverEnabled());

        if (isLanAccessEnabled()) {
            builder.allowLocalNetworkTraffic();
        }

        if (isTrackerAdBlockerEnabled() && dialing != Dialing.CHANNEL) {
            builder.withAtomShield(
                    Collections.singletonList(AtomShieldFeature.TRACKER_AND_AD_BLOCKER));
        }

        Set<String> apps = getSplitTunnelApps();
        if (!apps.isEmpty()) {
            String[] packages = apps.toArray(new String[0]);
            SplitTunnelMode mode = getSplitTunnelMode();
            if (mode == SplitTunnelMode.EXCLUDE) {
                builder.withReverseSplitTunneling(packages);
            } else if (mode == SplitTunnelMode.ONLY) {
                builder.withSplitTunneling(packages);
            }
        }

        if (dialing != Dialing.PARAMS) {
            return;
        }

        if (isAutomaticPortEnabled()) {
            builder.withAutomaticPort();
        } else {
            builder.withManualPort(getManualPort());
        }

        String nas = getServerFilterNas();
        if (!TextUtils.isEmpty(nas)) {
            builder.withServerFilter(Collections.singletonList(new ServerFilter(nas,
                    isServerFilterInclude() ? ServerFilterType.INCLUDE : ServerFilterType.EXCLUDE)));
        }
    }
}
