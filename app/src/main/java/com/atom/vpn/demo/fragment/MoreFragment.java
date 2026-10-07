/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.atom.core.exceptions.AtomException;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.common.DemoSettings;
import com.atom.vpn.demo.common.logger.LogStore;

import de.blinkt.openvpn.BuildConfig;

import java.util.List;

/**
 * More tab: the SDK's configuration surface. Rows whose sub-screen does not exist yet are
 * disabled rather than left looking tappable, so the screen never promises a destination that is
 * not there.
 */
public class MoreFragment extends Fragment {

    public static final String ARG_VPN_USERNAME = "vpnUsername";
    public static final String ARG_VPN_PASSWORD = "vpnPassword";

    /** Bottom nav items, in display order. Kept in step with {@link HomeFragment}. */
    private static final int[] NAV_ITEM_IDS = {
            R.id.navDashboard, R.id.navConnection, R.id.navLocations, R.id.navMore
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String vpnUsername;
    private String vpnPassword;

    /** True while renderSplitTunneling moves the switch, so that is not taken as a user tap. */
    private boolean renderingSplitSwitch;

    public MoreFragment() {
        // Required empty public constructor
    }

    public static MoreFragment newInstance(String vpnUsername, String vpnPassword) {
        Bundle args = new Bundle();
        args.putString(ARG_VPN_USERNAME, vpnUsername);
        args.putString(ARG_VPN_PASSWORD, vpnPassword);

        MoreFragment fragment = new MoreFragment();
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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        renderProtocol(view);
        bindSdkInfo(view);

        view.findViewById(R.id.rowVpnProtocol).setOnClickListener(v -> pickProtocol());
        view.findViewById(R.id.rowSdkLogs).setOnClickListener(v -> openLogs());
        view.findViewById(R.id.rowConnectionFeatures).setOnClickListener(
                v -> push(new ConnectionFeaturesFragment()));
        view.findViewById(R.id.rowSplitTunneling).setOnClickListener(
                v -> push(new SplitTunnelingFragment()));
        bindSplitTunneling(view);
        view.findViewById(R.id.tvResetConfiguration).setOnClickListener(v -> confirmReset());

        // SDK Info stays at full opacity - the row *is* the information, there is just nothing
        // behind its arrow - so only the arrow goes.
        View sdkInfoRow = view.findViewById(R.id.rowSdkInfo);
        sdkInfoRow.setEnabled(false);
        sdkInfoRow.setClickable(false);
        view.findViewById(R.id.ivSdkInfoArrow).setVisibility(View.GONE);

        CoachMarks.set(view, R.string.coach_more_protocol, R.id.rowVpnProtocol);
        CoachMarks.set(view, R.string.coach_more_split_tunneling,
                R.id.rowSplitTunneling, R.id.splitTunnelingSwitch);
        CoachMarks.set(view, R.string.coach_more_connection_features, R.id.rowConnectionFeatures);
        CoachMarks.set(view, R.string.coach_more_sdk_logs, R.id.rowSdkLogs);
        CoachMarks.set(view, R.string.coach_more_sdk_info, R.id.rowSdkInfo);
        CoachMarks.set(view, R.string.coach_more_reset_configuration, R.id.tvResetConfiguration);
        CoachMarks.setBottomNav(view);

        view.findViewById(R.id.btnSdkLogs).setOnClickListener(
                v -> HomeFragment.openLogs(this, vpnUsername, vpnPassword));
        CoachMarks.set(view.findViewById(R.id.btnSdkLogs), R.string.coach_open_logs);

        setUpBottomNav(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-read on every return, since errors may have been logged while this was stopped.
        if (getView() != null) {
            bindLogErrorCount(getView());
            // Back from the Split Tunneling screen: mode or app count may have changed.
            renderSplitTunneling(getView());
        }
    }

    /** Logs sits above this screen on the back stack, so Back from there lands here again. */
    private void openLogs() {
        push(LogsFragment.newInstance(vpnUsername, vpnPassword));
    }

    /** Opens a sub-screen above this one; Back (or its own back arrow) returns here. */
    private void push(@NonNull Fragment screen) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.sample_content_fragment, screen)
                .addToBackStack(null)
                .commit();
    }

    /**
     * The switch turns split tunneling on (with the mode used last) or off; picking the mode and
     * the apps happens on the Split Tunneling screen the row opens.
     */
    private void bindSplitTunneling(@NonNull View view) {
        SwitchCompat toggle = view.findViewById(R.id.splitTunnelingSwitch);
        renderSplitTunneling(view);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            if (renderingSplitSwitch) {
                return;
            }
            DemoSettings settings = DemoSettings.get(requireContext());
            settings.setSplitTunnelMode(checked
                    ? settings.getLastSplitTunnelMode() : DemoSettings.SplitTunnelMode.OFF);
            renderSplitTunneling(view);
            if (checked && settings.getSplitTunnelApps().isEmpty()) {
                // On with nothing selected would do nothing; send the user to pick apps.
                Toast.makeText(requireContext(), R.string.split_select_apps_first,
                        Toast.LENGTH_SHORT).show();
                push(new SplitTunnelingFragment());
            }
        });
    }

    private void renderSplitTunneling(@NonNull View view) {
        DemoSettings settings = DemoSettings.get(requireContext());
        DemoSettings.SplitTunnelMode mode = settings.getSplitTunnelMode();
        int count = settings.getSplitTunnelApps().size();

        SwitchCompat toggle = view.findViewById(R.id.splitTunnelingSwitch);
        boolean on = mode != DemoSettings.SplitTunnelMode.OFF;
        if (toggle.isChecked() != on) {
            renderingSplitSwitch = true;
            toggle.setChecked(on);
            renderingSplitSwitch = false;
        }

        TextView summary = view.findViewById(R.id.tvSplitTunnelingSummary);
        if (!on || count == 0) {
            summary.setText(R.string.split_summary_off);
        } else {
            summary.setText(getString(mode == DemoSettings.SplitTunnelMode.EXCLUDE
                    ? R.string.split_summary_exclude : R.string.split_summary_only, count));
        }
    }

    /** The badge only appears once there is something to flag. */
    private void bindLogErrorCount(@NonNull View view) {
        int errors = LogStore.get().count(LogStore.Level.ERROR);
        int visibility = errors > 0 ? View.VISIBLE : View.GONE;
        view.findViewById(R.id.vStatusDot).setVisibility(visibility);
        TextView tvCount = view.findViewById(R.id.tvLogErrorCount);
        tvCount.setVisibility(visibility);
        tvCount.setText(getResources().getQuantityString(R.plurals.log_error_count, errors, errors));
    }

    /** Asks first, since it wipes the protocol, connection features and split tunneling choices. */
    private void confirmReset() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_sdk_configuration)
                .setMessage(R.string.reset_sdk_configuration_message)
                .setPositiveButton(R.string.reset, (dialog, which) -> resetConfiguration())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void resetConfiguration() {
        boolean persistWasOn = DemoSettings.get(requireContext()).reset();
        LogStore.get().add(LogStore.Level.INFO, "SDK configuration reset to defaults.");
        if (getView() != null) {
            renderProtocol(getView());
            renderSplitTunneling(getView());
        }
        Toast.makeText(requireContext(), persistWasOn
                        ? R.string.reset_done_restart : R.string.reset_done,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * The SDK has no global protocol setting - it takes one per connection - so the demo keeps a
     * preferred one that the connect screens start with as primary. "Default" clears it.
     */
    private void pickProtocol() {
        AtomManager atomManager = AtomDemoApplicationController.getInstance().getAtomManager();
        if (atomManager == null) {
            Toast.makeText(requireContext(), R.string.atom_manager_not_initialized,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        atomManager.getProtocols(new CollectionCallback<Protocol>() {
            @Override
            public void onSuccess(List<Protocol> list) {
                mainHandler.post(() -> showProtocolPicker(list));
            }

            @Override
            public void onError(AtomException atomException) {
                mainHandler.post(() -> onProtocolsFailed());
            }

            @Override
            public void onNetworkError(AtomException atomException) {
                mainHandler.post(() -> onProtocolsFailed());
            }
        });
    }

    private void showProtocolPicker(@Nullable List<Protocol> list) {
        if (!isAdded()) {
            return;
        }
        if (list == null || list.isEmpty()) {
            onProtocolsFailed();
            return;
        }

        String[] names = new String[list.size() + 1];
        names[0] = getString(R.string.protocol_default_first);
        for (int i = 0; i < list.size(); i++) {
            names[i + 1] = list.get(i).getName();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.vpn_protocol)
                .setItems(names, (dialog, which) -> {
                    DemoSettings.get(requireContext())
                            .setPreferredProtocol(which == 0 ? null : list.get(which - 1));
                    if (getView() != null) {
                        renderProtocol(getView());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onProtocolsFailed() {
        if (isAdded()) {
            Toast.makeText(requireContext(), R.string.protocols_unavailable,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void renderProtocol(@NonNull View view) {
        String name = DemoSettings.get(requireContext()).getPreferredProtocolName();
        ((TextView) view.findViewById(R.id.tvSelectedProtocol)).setText(
                name.isEmpty() ? getString(R.string.protocol_default) : name);
    }

    /** SDK version comes from the SDK's own BuildConfig; the build number is this app's. */
    private void bindSdkInfo(@NonNull View view) {
        ((TextView) view.findViewById(R.id.tvSdkVersion)).setText(
                getString(R.string.sdk_version_build_format, BuildConfig.SDKVersion, appBuild()));
    }

    @NonNull
    private String appBuild() {
        try {
            PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? info.getLongVersionCode()
                    : info.versionCode;
            return String.valueOf(code);
        } catch (PackageManager.NameNotFoundException e) {
            // Cannot happen for our own package.
            return "";
        }
    }

    /**
     * Dashboard pops back to {@link HomeFragment} and Locations swaps to that screen; More is this
     * one. Connection opens Connection With Params.
     */
    private void setUpBottomNav(@NonNull View root) {
        for (int id : NAV_ITEM_IDS) {
            root.findViewById(id).setOnClickListener(v -> {
                if (v.getId() == R.id.navConnection) {
                    HomeFragment.openSelectedConnectScreen(this, vpnUsername, vpnPassword);
                    return;
                }
                if (v.getId() == R.id.navDashboard) {
                    requireActivity().getSupportFragmentManager()
                            .popBackStack(HomeFragment.TAB_BACK_STACK,
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    return;
                }
                if (v.getId() == R.id.navLocations) {
                    HomeFragment.showTab(this,
                            LocationsFragment.newInstance(vpnUsername, vpnPassword));
                    return;
                }
                selectTab(root, v.getId());
            });
        }
        selectTab(root, R.id.navMore);
    }

    private void selectTab(@NonNull View root, @IdRes int tabId) {
        for (int id : NAV_ITEM_IDS) {
            // The item is one TextView with a compound drawable, so this switches label and icon together.
            root.findViewById(id).setSelected(id == tabId);
        }
    }
}
