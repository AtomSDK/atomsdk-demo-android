/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.atom.sdk.android.AtomManager;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.base.BaseSampleActivity;
import com.atom.vpn.demo.fragment.HomeFragment;
import com.atom.vpn.demo.fragment.LocationsFragment;
import com.atom.vpn.demo.fragment.MainFragment;
import com.atom.vpn.demo.fragment.MoreFragment;

public class SampleMainActivity extends BaseSampleActivity {

    /** Sent by {@link ConnectActivity#openMainScreen}: which screen its bottom nav or top bar asked for. */
    public static final String EXTRA_OPEN_SCREEN = "open_screen";
    public static final String SCREEN_DASHBOARD = "dashboard";
    public static final String SCREEN_LOCATIONS = "locations";
    public static final String SCREEN_MORE = "more";
    public static final String SCREEN_LOGS = "logs";

    /** Same keys the connect screens are started with, so the tab screens keep the credentials. */
    public static final String EXTRA_VPN_USERNAME = HomeFragment.ARG_VPN_USERNAME;
    public static final String EXTRA_VPN_PASSWORD = HomeFragment.ARG_VPN_PASSWORD;

    /** Held until onResumeFragments: onNewIntent can arrive before fragment state is restored. */
    @Nullable
    private Intent pendingScreenRequest;

    // Denial is fine: the VPN still works, the connected notification just stays hidden.
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_main);

        if (savedInstanceState == null) {
            // The VPN permission is asked for by MainFragment's Initialize SDK button.
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.sample_content_fragment, new MainFragment())
                    .commit();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Back only tears the tunnel down when it would close the app. With fragments on the back
        // stack (Home, Locations, More, Logs) FragmentManager's own callback pops them instead.
        OnBackPressedCallback exitCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                stopVpn();
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, exitCallback);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(
                () -> exitCallback.setEnabled(fragmentManager.getBackStackEntryCount() == 0));
        exitCallback.setEnabled(fragmentManager.getBackStackEntryCount() == 0);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(EXTRA_OPEN_SCREEN)) {
            pendingScreenRequest = intent;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (pendingScreenRequest == null) {
            return;
        }
        Intent request = pendingScreenRequest;
        pendingScreenRequest = null;

        String username = request.getStringExtra(EXTRA_VPN_USERNAME);
        String password = request.getStringExtra(EXTRA_VPN_PASSWORD);
        FragmentManager fm = getSupportFragmentManager();
        String screen = String.valueOf(request.getStringExtra(EXTRA_OPEN_SCREEN));
        switch (screen) {
            case SCREEN_LOCATIONS:
                HomeFragment.showTab(fm, LocationsFragment.newInstance(username, password));
                break;
            case SCREEN_MORE:
                HomeFragment.showTab(fm, MoreFragment.newInstance(username, password));
                break;
            case SCREEN_LOGS:
                // Clears the tabs first so Logs sits right on the Dashboard, as it would have
                // when the connect screen was opened from there.
                HomeFragment.showDashboard(fm);
                HomeFragment.openLogs(fm, username, password);
                break;
            default:
                HomeFragment.showDashboard(fm);
        }
    }

    private void stopVpn() {
        @Nullable AtomManager atomManager = AtomDemoApplicationController.getInstance().getAtomManager();
        if (atomManager == null) {
            return;
        }
        String status = atomManager.getCurrentVpnStatus(this);
        if (AtomManager.VPNStatus.CONNECTED.equalsIgnoreCase(status)) {
            atomManager.disconnect(this);
        } else if (AtomManager.VPNStatus.CONNECTING.equalsIgnoreCase(status)) {
            atomManager.cancel(this);
        }
    }
}
