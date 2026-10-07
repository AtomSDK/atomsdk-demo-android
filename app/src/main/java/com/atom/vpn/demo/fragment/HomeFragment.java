/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.atom.core.exceptions.AtomException;
import com.atom.core.models.AtomConfiguration;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.ConnectionDetails;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.common.DemoSettings;
import com.atom.vpn.demo.activity.ConnectActivity;

/**
 * Step 2 of the revamped flow: the credentials collected by {@link MainFragment} are handed here,
 * and this screen picks which "connect with ..." route to open in {@link ConnectActivity}.
 */
public class HomeFragment extends Fragment implements VPNStateListener {

    public static final String ARG_VPN_USERNAME = "vpnUsername";
    public static final String ARG_VPN_PASSWORD = "vpnPassword";

    /** Must match the connection_type values ConnectActivity switches on. */
    static final int CONNECTION_METHOD_PARAMS = 1;
    private static final int CONNECTION_METHOD_DEDICATED_IP = 2;
    private static final int CONNECTION_METHOD_CHANNEL = 3;
    private static final int CONNECTION_METHOD_MULTIPLE_DEDICATED_IP = 4;

    private static final String STATE_SELECTED_TAB = "selected_tab";

    /** How much of the secret key the dashboard shows before masking the rest. */
    private static final int SECRET_KEY_VISIBLE_CHARS = 5;

    /**
     * SystemClock.elapsedRealtime() when the tunnel was first seen connected, or 0 when it is not.
     * Static so the timer keeps counting when this fragment is recreated.
     */
    private static long connectedSinceElapsed;

    /**
     * Back stack name shared by every tab screen pushed from here, so at most one of them is ever
     * stacked above this fragment. See {@link #showTab}.
     */
    static final String TAB_BACK_STACK = "tab";

    /** Bottom nav items, in display order. */
    private static final int[] NAV_ITEM_IDS = {
            R.id.navDashboard, R.id.navConnection, R.id.navLocations, R.id.navMore
    };

    /** VPN callbacks are not guaranteed to arrive on the main thread. */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String vpnUsername;
    private String vpnPassword;

    private TextView tvConnectionStatus;
    private View vStatusDot;
    private TextView tvConnectedLocation;
    private Chronometer chConnectedTimer;

    private int selectedTabId = R.id.navDashboard;

    /** Redraws the SDK state once the asynchronous AtomManager.initialize() finishes. */
    private final AtomDemoApplicationController.OnInitializedListener onSdkInitialized =
            atomManager -> {
                if (getView() != null) {
                    updateSdkState(getView());
                    refreshConnectionStatus();
                }
            };

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String vpnUsername, String vpnPassword) {
        Bundle args = new Bundle();
        args.putString(ARG_VPN_USERNAME, vpnUsername);
        args.putString(ARG_VPN_PASSWORD, vpnPassword);

        HomeFragment fragment = new HomeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            vpnUsername = args.getString(ARG_VPN_USERNAME);
            vpnPassword = args.getString(ARG_VPN_PASSWORD);
        }

        AtomManager.addVPNStateListener(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus);
        vStatusDot = view.findViewById(R.id.vStatusDot);
        tvConnectedLocation = view.findViewById(R.id.tvConnectedLocation);
        chConnectedTimer = view.findViewById(R.id.chConnectedTimer);

        ((TextView) view.findViewById(R.id.tvSecretKey)).setText(redactedSecretKey());
        ((TextView) view.findViewById(R.id.versionPlaceholder)).setText(appVersionName());

        view.findViewById(R.id.btnConnectWithParams).setOnClickListener(
                v -> selectConnectionMethod(view, DemoSettings.Dialing.PARAMS));
        view.findViewById(R.id.btnConnectWithChannel).setOnClickListener(
                v -> selectConnectionMethod(view, DemoSettings.Dialing.CHANNEL));
        view.findViewById(R.id.btnConnectWithDedicatedIp).setOnClickListener(
                v -> selectConnectionMethod(view, DemoSettings.Dialing.DEDICATED_IP));
        view.findViewById(R.id.btnConnectWithMultipleDedicatedIp).setOnClickListener(
                v -> selectConnectionMethod(view, DemoSettings.Dialing.MULTIPLE_DEDICATED_IP));
        renderConnectionMethod(view);

        CoachMarks.set(view.findViewById(R.id.btnConnectWithParams),
                R.string.coach_open_connect_with_params);
        CoachMarks.set(view.findViewById(R.id.btnConnectWithChannel),
                R.string.coach_open_connect_with_channel);
        CoachMarks.set(view.findViewById(R.id.btnConnectWithDedicatedIp),
                R.string.coach_open_connect_with_dedicated_ip);
        CoachMarks.set(view.findViewById(R.id.btnConnectWithMultipleDedicatedIp),
                R.string.coach_open_connect_with_multiple_dedicated_ip);
        CoachMarks.setBottomNav(view);

        view.findViewById(R.id.btnSdkLogs).setOnClickListener(
                v -> openLogs(this, vpnUsername, vpnPassword));
        CoachMarks.set(view.findViewById(R.id.btnSdkLogs), R.string.coach_open_logs);

        if (savedInstanceState != null) {
            selectedTabId = savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.navDashboard);
        }
        setUpBottomNav(view);

        updateSdkState(view);
        refreshConnectionStatus();
        AtomDemoApplicationController.getInstance().addOnInitializedListener(onSdkInitialized);
    }

    /** A button opens its connect screen and becomes the one the Connection item opens next. */
    private void selectConnectionMethod(@NonNull View root, @NonNull DemoSettings.Dialing method) {
        DemoSettings.get(requireContext()).setConnectionMethod(method);
        renderConnectionMethod(root);
        openSelectedConnectScreen(this, vpnUsername, vpnPassword);
    }

    private void renderConnectionMethod(@NonNull View root) {
        DemoSettings.Dialing method = DemoSettings.get(requireContext()).getConnectionMethod();
        root.findViewById(R.id.btnConnectWithParams)
                .setSelected(method == DemoSettings.Dialing.PARAMS);
        root.findViewById(R.id.btnConnectWithChannel)
                .setSelected(method == DemoSettings.Dialing.CHANNEL);
        root.findViewById(R.id.btnConnectWithDedicatedIp)
                .setSelected(method == DemoSettings.Dialing.DEDICATED_IP);
        root.findViewById(R.id.btnConnectWithMultipleDedicatedIp)
                .setSelected(method == DemoSettings.Dialing.MULTIPLE_DEDICATED_IP);
    }

    /**
     * Locations and More swap to their screens; Connection opens the connect screen selected on
     * the Dashboard, a separate activity, so the selection stays on Dashboard.
     */
    private void setUpBottomNav(@NonNull View root) {
        for (int id : NAV_ITEM_IDS) {
            root.findViewById(id).setOnClickListener(v -> {
                if (v.getId() == R.id.navConnection) {
                    openSelectedConnectScreen(this, vpnUsername, vpnPassword);
                    return;
                }
                if (v.getId() == R.id.navLocations) {
                    showTab(this, LocationsFragment.newInstance(vpnUsername, vpnPassword));
                    return;
                }
                if (v.getId() == R.id.navMore) {
                    showTab(this, MoreFragment.newInstance(vpnUsername, vpnPassword));
                    return;
                }
                selectTab(root, v.getId());
            });
        }
        selectTab(root, selectedTabId);
    }

    /**
     * Locations and More are screens of their own rather than tab swaps. They all share one named
     * back stack entry, so switching between them replaces rather than stacks and this fragment
     * always stays directly underneath - popping {@link #TAB_BACK_STACK} lands back here whichever
     * tab screen is showing. HomeFragment itself sits below on the unnamed entry MainFragment
     * pushed, so it is never popped by this.
     */
    static void showTab(@NonNull Fragment from, @NonNull Fragment tab) {
        showTab(from.requireActivity().getSupportFragmentManager(), tab);
    }

    /** As {@link #showTab(Fragment, Fragment)}, for SampleMainActivity acting on ConnectActivity's nav. */
    public static void showTab(@NonNull FragmentManager fm, @NonNull Fragment tab) {
        fm.popBackStack(TAB_BACK_STACK, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.sample_content_fragment, tab)
                .addToBackStack(TAB_BACK_STACK)
                .commit();
    }

    /** Pops every tab screen, leaving this fragment showing. */
    public static void showDashboard(@NonNull FragmentManager fm) {
        fm.popBackStack(TAB_BACK_STACK, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    /**
     * The top bar's terminal icon: SDK Logs on top of whatever is showing, so Back returns there.
     * It goes on {@link #TAB_BACK_STACK} too - an inclusive pop takes consecutive entries of the
     * name together, so Dashboard still clears both it and the tab screen under it.
     */
    static void openLogs(@NonNull Fragment from, String vpnUsername, String vpnPassword) {
        openLogs(from.requireActivity().getSupportFragmentManager(), vpnUsername, vpnPassword);
    }

    public static void openLogs(@NonNull FragmentManager fm, String vpnUsername,
                                String vpnPassword) {
        fm.beginTransaction()
                .replace(R.id.sample_content_fragment,
                        LogsFragment.newInstance(vpnUsername, vpnPassword))
                .addToBackStack(TAB_BACK_STACK)
                .commit();
    }

    private void selectTab(@NonNull View root, int tabId) {
        selectedTabId = tabId;
        for (int id : NAV_ITEM_IDS) {
            // The item is one TextView with a compound drawable, so this switches label and icon together.
            root.findViewById(id).setSelected(id == tabId);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTabId);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Coming back from ConnectActivity the VPN may have changed state while this screen was
        // stopped, so the card is re-read rather than trusting the last callback.
        if (getView() != null) {
            updateSdkState(getView());
        }
        refreshConnectionStatus();
    }

    @Override
    public void onDestroyView() {
        AtomDemoApplicationController.getInstance().removeOnInitializedListener(onSdkInitialized);
        chConnectedTimer.stop();
        super.onDestroyView();
        tvConnectionStatus = null;
        vStatusDot = null;
        tvConnectedLocation = null;
        chConnectedTimer = null;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        AtomManager.removeVPNStateListener(this);
        super.onDestroy();
    }

    /**
     * The versionName declared in app/build.gradle. Read through PackageManager because this module
     * does not generate a BuildConfig class - AGP 8 only emits one when buildFeatures.buildConfig is
     * turned on, and the SDK's de.blinkt.openvpn.BuildConfig carries the SDK version, not the app's.
     */
    @NonNull
    private String appVersionName() {
        try {
            PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Cannot happen for our own package.
            return "";
        }
    }

    /** The first few characters of the secret key, the rest masked. */
    @NonNull
    private String redactedSecretKey() {
        String secretKey = getString(R.string.atom_secret_key);
        if (TextUtils.isEmpty(secretKey)) {
            return getString(R.string.empty_secret_key);
        }
        if (secretKey.length() <= SECRET_KEY_VISIBLE_CHARS) {
            return secretKey;
        }
        return secretKey.substring(0, SECRET_KEY_VISIBLE_CHARS) + "••••••••";
    }

    private void updateSdkState(@NonNull View view) {
        boolean initialized = AtomDemoApplicationController.getInstance().getAtomManager() != null;
        ((TextView) view.findViewById(R.id.tvSdkState))
                .setText(initialized ? R.string.initialized : R.string.not_initialized);
    }

    private void refreshConnectionStatus() {
        AtomManager atomManager = AtomDemoApplicationController.getInstance().getAtomManager();
        applyConnectionStatus(atomManager == null
                ? AtomManager.VPNStatus.DISCONNECTED
                : atomManager.getCurrentVpnStatus(requireContext()));
    }

    private void postConnectionStatus(final String status) {
        mainHandler.post(() -> applyConnectionStatus(status));
    }

    private void applyConnectionStatus(@Nullable String status) {
        if (tvConnectionStatus == null || vStatusDot == null) {
            // A callback outlived the view; the next onResume will re-read the state.
            return;
        }

        @StringRes int label;
        boolean connected = false;

        if (AtomManager.VPNStatus.CONNECTED.equalsIgnoreCase(status)) {
            label = R.string.connected;
            connected = true;
        } else if (AtomManager.VPNStatus.CONNECTING.equalsIgnoreCase(status)) {
            label = R.string.connecting;
        } else if (AtomManager.VPNStatus.DISCONNECTING.equalsIgnoreCase(status)) {
            label = R.string.disconnecting;
        } else if (AtomManager.VPNStatus.PAUSED.equalsIgnoreCase(status)
                || AtomManager.VPNStatus.PAUSING.equalsIgnoreCase(status)) {
            label = R.string.paused;
        } else {
            label = R.string.disconnected;
        }

        tvConnectionStatus.setText(label);
        vStatusDot.setBackgroundResource(
                connected ? R.drawable.bg_green_circle : R.drawable.bg_red_circle);
        applyConnectedDetails(connected);
    }

    /** Location and running timer under the status, while connected. */
    private void applyConnectedDetails(boolean connected) {
        if (!connected) {
            connectedSinceElapsed = 0;
            chConnectedTimer.stop();
            chConnectedTimer.setVisibility(View.GONE);
            tvConnectedLocation.setVisibility(View.GONE);
            return;
        }
        if (connectedSinceElapsed == 0) {
            connectedSinceElapsed = SystemClock.elapsedRealtime();
        }
        tvConnectedLocation.setText(connectedLocation());
        tvConnectedLocation.setVisibility(View.VISIBLE);
        chConnectedTimer.setBase(connectedSinceElapsed);
        chConnectedTimer.setVisibility(View.VISIBLE);
        chConnectedTimer.start();
    }

    /** City (with country) the SDK reports for the current tunnel. */
    @NonNull
    private String connectedLocation() {
        AtomManager atomManager = AtomDemoApplicationController.getInstance().getAtomManager();
        ConnectionDetails details = atomManager == null ? null : atomManager.getConnectionDetails();
        if (details == null) {
            return getString(R.string.not_available);
        }
        String city = details.getCity() == null ? null : details.getCity().getName();
        String country = details.getCountry();
        if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(country)) {
            return city + ", " + country;
        }
        if (!TextUtils.isEmpty(city)) {
            return city;
        }
        return TextUtils.isEmpty(country) ? getString(R.string.not_available) : country;
    }

    /** The bottom nav's Connection item, on every tab screen: opens the Dashboard's selection. */
    static void openSelectedConnectScreen(@NonNull Fragment from, String vpnUsername,
                                          String vpnPassword) {
        int type;
        switch (DemoSettings.get(from.requireContext()).getConnectionMethod()) {
            case CHANNEL:
                type = CONNECTION_METHOD_CHANNEL;
                break;
            case DEDICATED_IP:
                type = CONNECTION_METHOD_DEDICATED_IP;
                break;
            case MULTIPLE_DEDICATED_IP:
                type = CONNECTION_METHOD_MULTIPLE_DEDICATED_IP;
                break;
            default:
                type = CONNECTION_METHOD_PARAMS;
        }
        openConnectScreen(from, type, vpnUsername, vpnPassword);
    }

    /** Opens one of the connect screens. */
    private static void openConnectScreen(@NonNull Fragment from, int connectionMethodType,
                                          String vpnUsername, String vpnPassword) {
        if (AtomDemoApplicationController.getInstance().getAtomManager() == null) {
            Toast.makeText(from.getActivity(), R.string.atom_manager_not_initialized, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        Intent intent = new Intent(from.getActivity(), ConnectActivity.class);
        intent.putExtra("connection_type", connectionMethodType);
        intent.putExtra(ARG_VPN_USERNAME, vpnUsername);
        intent.putExtra(ARG_VPN_PASSWORD, vpnPassword);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        from.startActivity(intent);
    }

    // region VPNStateListener - every callback funnels into the same status renderer.

    @Override
    public void onStateChange(String state) {
        postConnectionStatus(state);
    }

    @Override
    public void onConnected() {
        postConnectionStatus(AtomManager.VPNStatus.CONNECTED);
    }

    @Override
    public void onConnected(ConnectionDetails connectionDetails) {
        postConnectionStatus(AtomManager.VPNStatus.CONNECTED);
    }

    @Override
    public void onConnecting() {
        postConnectionStatus(AtomManager.VPNStatus.CONNECTING);
    }

    @Override
    public void onConnecting(VPNProperties vpnProperties, AtomConfiguration atomConfiguration) {
        postConnectionStatus(AtomManager.VPNStatus.CONNECTING);
    }

    @Override
    public void onDisconnecting(ConnectionDetails connectionDetails) {
        postConnectionStatus(AtomManager.VPNStatus.DISCONNECTING);
    }

    @Override
    public void onDisconnected(boolean isCancelled) {
        postConnectionStatus(AtomManager.VPNStatus.DISCONNECTED);
    }

    @Override
    public void onDisconnected(ConnectionDetails connectionDetails) {
        postConnectionStatus(AtomManager.VPNStatus.DISCONNECTED);
    }

    @Override
    public void onPaused(AtomException e, ConnectionDetails connectionDetails) {
        postConnectionStatus(AtomManager.VPNStatus.PAUSED);
    }

    @Override
    public void onDialError(AtomException atomException, ConnectionDetails connectionDetails) {
        postConnectionStatus(AtomManager.VPNStatus.DISCONNECTED);
    }

    @Override
    public void onRedialing(AtomException atomException, ConnectionDetails connectionDetails) {
        postConnectionStatus(AtomManager.VPNStatus.CONNECTING);
    }

    @Override
    public void onUnableToAccessInternet(AtomException atomException,
                                         ConnectionDetails connectionDetails) {
        // Not a state transition - the banner keeps showing whatever the VPN state already is.
    }

    @Override
    public void onPacketsTransmitted(String in, String out, String inTotal, String outTotal) {
        // This screen does not show throughput.
    }

    // endregion
}
