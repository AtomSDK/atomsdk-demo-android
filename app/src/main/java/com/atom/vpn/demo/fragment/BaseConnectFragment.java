/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.atom.core.exceptions.AtomAPIException;
import com.atom.core.exceptions.AtomException;
import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.AtomConfiguration;
import com.atom.core.models.Location;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.ConnectionDetails;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.PauseVPNTimer;
import com.atom.sdk.android.VPNCredentials;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.sdk.android.data.callbacks.Callback;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.sdk.android.shield.delegate.AtomShieldListener;
import com.atom.sdk.android.shield.model.AtomShieldData;
import com.atom.sdk.android.shield.states.AtomShieldStatus;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.activity.SampleMainActivity;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.common.DemoSettings;
import com.atom.vpn.demo.common.Utilities;
import com.atom.vpn.demo.common.logger.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared body of the three "Connection With ..." screens: the CURRENT STATE card
 * (view_connect_state), the optional protocol chips (view_connect_protocols) and the action rows
 * plus result card (view_connect_actions). A subclass supplies its layout, its own target picker
 * and {@link #newPropertiesBuilder}; everything from connect() onwards is handled here.
 */
public abstract class BaseConnectFragment extends Fragment implements VPNStateListener {

    /** VPN and inventory callbacks are not guaranteed to arrive on the main thread. */
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String uuid, vpnUsername, vpnPassword;

    /** From getProtocols(); null until it returns. */
    @Nullable protected List<Protocol> protocols;

    @Nullable protected Protocol primaryProtocol;

    /**
     * "Recommended (auto)" picked as the primary protocol: the SDK's recommended protocol for the
     * chosen target is dialled instead of {@link #primaryProtocol}. See {@link #primaryFor}.
     */
    protected boolean useRecommendedProtocol;
    @Nullable protected Protocol secondaryProtocol;
    @Nullable protected Protocol tertiaryProtocol;

    /** What was dialled last, shown in the card while that connection is up. */
    @Nullable private String dialedTarget;

    /**
     * Set while a Connection over Connection is under way, so the disconnect the SDK performs
     * first is not reported as the user's own disconnect().
     */
    private boolean cocInProgress;

    /** Whether tvLocation holds (or is fetching) the connected location, so it is asked for once per connection. */
    private boolean locationShown;
    private boolean ikevServiceBound;

    /** Set by onPaused, so the next onConnected is reported as a resume rather than a connect. */
    private boolean resumePending;

    /** Latest Tracker/Ad Blocker state and blocked count, rendered into the state card. */
    @Nullable private String shieldStatus;
    private int shieldBlocked;

    @Nullable private View layShield, layTraffic;
    @Nullable private TextView tvShieldStatus, tvShieldBlocked, tvTrafficDown, tvTrafficUp;

    /** SDK callbacks for the Tracker/Ad Blocker; they are not guaranteed to be on the main thread. */
    private final AtomShieldListener shieldListener = new AtomShieldListener() {
        @Override
        public void onAtomShieldStatusChange(AtomShieldStatus status) {
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                shieldStatus = describeShieldStatus(status);
                renderShield();
            });
        }

        @Override
        public void onAtomShieldDataReceived(@Nullable AtomShieldData data) {
            mainHandler.post(() -> {
                shieldBlocked = data == null ? 0 : data.getCounter();
                renderShield();
            });
        }
    };

    private View initialSetupHeaderStatus, vStatusDot;
    private TextView tvSetupStatus, tvLocation;
    @Nullable private View btnSelectPrimaryProtocol, btnSelectSecondaryProtocol,
            btnSelectTertiaryProtocol;
    @Nullable private TextView tvPrimaryProtocol, tvSecondaryProtocol, tvTertiaryProtocol;
    private Button btnConnect, btnDisconnect, btnPause;
    private View btnReconnect, btnResume, btnConnectionOverConnection, layPause, layResume;
    private View resultCard;
    private ImageView ivResultIcon;
    private TextView tvResultTitle, tvResultMessage;

    // region Subclass contract

    @LayoutRes
    protected abstract int layoutRes();

    /** Finds and wires the screen's own target picker, including its coach marks. */
    protected abstract void onBindTarget(@NonNull View root);

    /** Releases the views bound in {@link #onBindTarget}. */
    protected abstract void onUnbindTarget();

    /** Loads whatever inventory the target picker needs besides protocols. */
    protected abstract void onLoadInventory(@NonNull AtomManager atomManager);

    /** Views that pick the target; locked while a connection is changing state. */
    @NonNull
    protected abstract View[] targetSelectors();

    /**
     * Builder for the current selection, or null - after telling the user what is missing - when
     * the selection is incomplete.
     */
    @Nullable
    protected abstract VPNProperties.Builder newPropertiesBuilder() throws AtomValidationException;

    /** Human-readable name of the current selection, shown as the target once dialled. */
    @NonNull
    protected abstract String targetDescription();

    /** The protocol selection changed; drop any target that no longer supports it. */
    protected void onProtocolsChanged() {
    }

    /** Which connection type this screen dials, so {@link DemoSettings} applies what fits. */
    @NonNull
    protected abstract DemoSettings.Dialing dialing();

    /**
     * Whether the primary protocol list offers "Recommended (auto)". Only for targets that carry
     * a recommended protocol (country, city, channel); not for a dedicated IP.
     */
    protected boolean supportsRecommendedProtocol() {
        return false;
    }

    /** An inventory call (protocols here, or a subclass's own) failed. Runs on the main thread. */
    protected void onInventoryFailed() {
    }

    // endregion

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getArguments();
        if (extras != null) {
            uuid = extras.getString("uuid");
            vpnUsername = extras.getString("vpnUsername");
            vpnPassword = extras.getString("vpnPassword");
        }

        AtomManager.addVPNStateListener(this);

        mainHandler.postDelayed(() -> {
            AtomManager atomManager = atomManager();
            if (atomManager != null && getActivity() != null) {
                atomManager.bindIKEVStateService(getActivity());
                ikevServiceBound = true;
            }
        }, 500);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(layoutRes(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // The SDK Logs screen lives in SampleMainActivity, so the activity hands over to it.
        view.findViewById(R.id.btnSdkLogs).setOnClickListener(v ->
                ((ConnectActivity) requireActivity())
                        .openMainScreen(SampleMainActivity.SCREEN_LOGS));
        CoachMarks.set(view.findViewById(R.id.btnSdkLogs), R.string.coach_open_logs);

        initialSetupHeaderStatus = view.findViewById(R.id.initialSetupHeaderStatus);
        vStatusDot = view.findViewById(R.id.vStatusDot);
        tvSetupStatus = view.findViewById(R.id.tvSetupStatus);
        tvLocation = view.findViewById(R.id.tvLocation);

        // Protocol chips are optional: the dedicated IP screen has a fixed protocol instead.
        btnSelectPrimaryProtocol = view.findViewById(R.id.btnSelectPrimaryProtocol);
        btnSelectSecondaryProtocol = view.findViewById(R.id.btnSelectSecondaryProtocol);
        btnSelectTertiaryProtocol = view.findViewById(R.id.btnSelectTertiaryProtocol);
        tvPrimaryProtocol = view.findViewById(R.id.tvPrimaryProtocol);
        tvSecondaryProtocol = view.findViewById(R.id.tvSecondaryProtocol);
        tvTertiaryProtocol = view.findViewById(R.id.tvTertiaryProtocol);
        if (btnSelectPrimaryProtocol != null) {
            btnSelectPrimaryProtocol.setOnClickListener(v -> pickPrimaryProtocol());
            btnSelectSecondaryProtocol.setOnClickListener(v -> pickOptionalProtocol(true));
            btnSelectTertiaryProtocol.setOnClickListener(v -> pickOptionalProtocol(false));
            CoachMarks.set(btnSelectPrimaryProtocol, R.string.coach_primary_protocol);
            CoachMarks.set(btnSelectSecondaryProtocol, R.string.coach_secondary_protocol);
            CoachMarks.set(btnSelectTertiaryProtocol, R.string.coach_tertiary_protocol);
        }

        btnConnect = view.findViewById(R.id.btnConnect);
        btnDisconnect = view.findViewById(R.id.btnDisconnect);
        btnReconnect = view.findViewById(R.id.btnReconnect);
        btnPause = view.findViewById(R.id.btnPause);
        btnResume = view.findViewById(R.id.btnResume);
        btnConnectionOverConnection = view.findViewById(R.id.btnConnectionOverConnection);
        layPause = view.findViewById(R.id.layPause);
        layResume = view.findViewById(R.id.layResume);

        layShield = view.findViewById(R.id.layShield);
        layTraffic = view.findViewById(R.id.layTraffic);
        tvShieldStatus = view.findViewById(R.id.tvShieldStatus);
        tvShieldBlocked = view.findViewById(R.id.tvShieldBlocked);
        tvTrafficDown = view.findViewById(R.id.tvTrafficDown);
        tvTrafficUp = view.findViewById(R.id.tvTrafficUp);
        AtomManager shieldManager = atomManager();
        if (shieldManager != null) {
            shieldManager.addAtomShieldListener(shieldListener);
        }

        resultCard = view.findViewById(R.id.resultCard);
        ivResultIcon = view.findViewById(R.id.ivResultIcon);
        tvResultTitle = view.findViewById(R.id.tvResultTitle);
        tvResultMessage = view.findViewById(R.id.tvResultMessage);

        btnConnect.setOnClickListener(v -> onConnectClick());
        btnDisconnect.setOnClickListener(v -> onDisconnectClick());
        btnReconnect.setOnClickListener(v -> onReconnectClick());
        btnPause.setOnClickListener(v -> onPauseClick());
        btnResume.setOnClickListener(v -> onResumeClick());
        btnConnectionOverConnection.setOnClickListener(v -> onConnectionOverConnectionClick());

        CoachMarks.set(btnConnect, R.string.coach_connect);
        CoachMarks.set(btnDisconnect, R.string.coach_disconnect);
        CoachMarks.set(btnReconnect, R.string.coach_reconnect);
        CoachMarks.set(btnPause, R.string.coach_pause);
        CoachMarks.set(btnResume, R.string.coach_resume);
        CoachMarks.set(btnConnectionOverConnection, R.string.coach_connection_over_connection);

        onBindTarget(view);

        renderProtocols();
        renderShield();
        refreshStatus();
        loadInventory();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onUnbindTarget();
        AtomManager shieldManager = atomManager();
        if (shieldManager != null) {
            shieldManager.removeAtomShieldListener(shieldListener);
        }
        layShield = null;
        layTraffic = null;
        tvShieldStatus = null;
        tvShieldBlocked = null;
        tvTrafficDown = null;
        tvTrafficUp = null;
        initialSetupHeaderStatus = null;
        vStatusDot = null;
        tvSetupStatus = null;
        tvLocation = null;
        locationShown = false;
        btnSelectPrimaryProtocol = null;
        btnSelectSecondaryProtocol = null;
        btnSelectTertiaryProtocol = null;
        tvPrimaryProtocol = null;
        tvSecondaryProtocol = null;
        tvTertiaryProtocol = null;
        btnConnect = null;
        btnDisconnect = null;
        btnReconnect = null;
        btnPause = null;
        btnResume = null;
        btnConnectionOverConnection = null;
        layPause = null;
        layResume = null;
        resultCard = null;
        ivResultIcon = null;
        tvResultTitle = null;
        tvResultMessage = null;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        AtomManager.removeVPNStateListener(this);
        AtomManager atomManager = atomManager();
        // The bind is delayed, so a fragment destroyed within 500ms never bound anything.
        if (ikevServiceBound && atomManager != null && getActivity() != null) {
            atomManager.unBindIKEVStateService(getActivity());
        }
        ikevServiceBound = false;
        super.onDestroy();
    }

    @Nullable
    protected static AtomManager atomManager() {
        return AtomDemoApplicationController.getInstance().getAtomManager();
    }

    protected void logError(@NonNull AtomException atomException) {
        Log.e(getClass().getSimpleName(),
                atomException.getMessage() + " : " + atomException.getCode());
    }

    // region Protocols

    private void loadInventory() {
        AtomManager atomManager = atomManager();
        if (atomManager == null) {
            return;
        }

        if (btnSelectPrimaryProtocol != null) {
            atomManager.getProtocols(new CollectionCallback<Protocol>() {
                @Override
                public void onSuccess(List<Protocol> list) {
                    mainHandler.post(() -> {
                        protocols = list;
                        if (primaryProtocol == null && list != null && !list.isEmpty()) {
                            primaryProtocol = preferredProtocol(list);
                        }
                        onProtocolsChanged();
                        renderProtocols();
                    });
                }

                @Override
                public void onError(AtomException atomException) {
                    logError(atomException);
                    mainHandler.post(() -> onInventoryFailed());
                }

                @Override
                public void onNetworkError(AtomException atomException) {
                    logError(atomException);
                    mainHandler.post(() -> onInventoryFailed());
                }
            });
        }

        onLoadInventory(atomManager);
    }

    /**
     * The starting primary protocol: the one picked on the More screen, or the first in the list
     * when none was picked or the SDK no longer offers it.
     */
    @NonNull
    protected Protocol preferredProtocol(@NonNull List<Protocol> list) {
        // Posted from an SDK callback, so the fragment may already be detached.
        String slug = getContext() == null ? ""
                : DemoSettings.get(getContext()).getPreferredProtocol();
        for (Protocol protocol : list) {
            if (slug.equalsIgnoreCase(protocol.getProtocol())) {
                return protocol;
            }
        }
        return list.get(0);
    }

    @NonNull
    protected List<Protocol> selectedProtocols() {
        List<Protocol> selected = new ArrayList<>();
        // With "Recommended (auto)" the primary depends on the target, so it cannot narrow it.
        if (primaryProtocol != null && !useRecommendedProtocol) selected.add(primaryProtocol);
        if (secondaryProtocol != null) selected.add(secondaryProtocol);
        if (tertiaryProtocol != null) selected.add(tertiaryProtocol);
        return selected;
    }

    /** Whether an inventory item's protocol list covers every selected protocol. */
    protected boolean supportsSelectedProtocols(@Nullable List<Protocol> supported) {
        return supported != null && supported.containsAll(selectedProtocols());
    }

    private void pickPrimaryProtocol() {
        if (protocols == null) {
            toast(R.string.loading);
            return;
        }
        List<Protocol> list = protocols;
        int offset = supportsRecommendedProtocol() ? 1 : 0;
        String[] names = new String[list.size() + offset];
        if (offset == 1) {
            names[0] = getString(R.string.recommended_protocol_auto);
        }
        for (int i = 0; i < list.size(); i++) {
            names[i + offset] = list.get(i).getName();
        }
        showPicker(R.string.lbl_primary_protocol, names, which -> {
            useRecommendedProtocol = offset == 1 && which == 0;
            if (!useRecommendedProtocol) {
                primaryProtocol = list.get(which - offset);
            }
            onProtocolsChanged();
            renderProtocols();
        });
    }

    /**
     * The primary protocol to dial for a target: the picked one, or with "Recommended (auto)" the
     * target's recommended protocol (falling back to the first one it supports). The SDK's
     * VPNProperties.Builder requires a non-null protocol, so the demo resolves it here.
     */
    @Nullable
    protected Protocol primaryFor(@Nullable Protocol recommended,
                                  @Nullable List<Protocol> targetProtocols) {
        if (!useRecommendedProtocol) {
            return primaryProtocol;
        }
        if (recommended != null) {
            return recommended;
        }
        return targetProtocols == null || targetProtocols.isEmpty() ? null : targetProtocols.get(0);
    }

    /** Secondary and tertiary are optional, so their list starts with "None". */
    private void pickOptionalProtocol(boolean secondary) {
        if (protocols == null) {
            toast(R.string.loading);
            return;
        }
        List<Protocol> list = protocols;
        String[] names = new String[list.size() + 1];
        names[0] = getString(R.string.none);
        for (int i = 0; i < list.size(); i++) {
            names[i + 1] = list.get(i).getName();
        }
        showPicker(secondary ? R.string.lbl_secondary_protocol : R.string.lbl_tertiary_protocol,
                names, which -> {
                    Protocol protocol = which == 0 ? null : list.get(which - 1);
                    if (secondary) {
                        secondaryProtocol = protocol;
                    } else {
                        tertiaryProtocol = protocol;
                    }
                    onProtocolsChanged();
                    renderProtocols();
                });
    }

    protected void renderProtocols() {
        if (tvPrimaryProtocol == null) {
            return;
        }
        if (useRecommendedProtocol) {
            tvPrimaryProtocol.setText(R.string.recommended_protocol_short);
        } else {
            tvPrimaryProtocol.setText(primaryProtocol == null
                    ? getString(R.string.not_available) : primaryProtocol.getName());
        }
        tvSecondaryProtocol.setText(secondaryProtocol == null
                ? getString(R.string.none) : secondaryProtocol.getName());
        tvTertiaryProtocol.setText(tertiaryProtocol == null
                ? getString(R.string.none) : tertiaryProtocol.getName());
    }

    // endregion

    // region Helpers for subclasses

    protected interface OnPicked {
        void onPicked(int which);
    }

    protected void showPicker(@StringRes int title, @NonNull String[] items,
                              @NonNull OnPicked onPicked) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setItems(items, (dialog, which) -> onPicked.onPicked(which))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    protected void toast(@StringRes int message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    // endregion

    // region Actions

    private void onConnectClick() {
        AtomManager atomManager = atomManager();
        if (atomManager == null) {
            toast(R.string.atom_manager_not_initialized);
            return;
        }
        if (AtomManager.VPNStatus.CONNECTING.equalsIgnoreCase(
                atomManager.getCurrentVpnStatus(requireContext()))) {
            atomManager.cancel(requireActivity());
            return;
        }
        dial(atomManager, false);
    }

    /**
     * The SDK has no dedicated call for Connection over Connection: connect() while a tunnel is
     * connected or paused is it. The SDK keeps the new properties, flags the connection as COC,
     * disconnects the current tunnel and dials the new one once it is down.
     */
    private void onConnectionOverConnectionClick() {
        AtomManager atomManager = atomManager();
        if (atomManager == null) {
            toast(R.string.atom_manager_not_initialized);
            return;
        }
        dial(atomManager, true);
    }

    /** Starts a plain connect() with the current selection, as the Connect button does. */
    protected void connectWithSelection() {
        AtomManager atomManager = atomManager();
        if (atomManager != null) {
            dial(atomManager, false);
        }
    }

    /** Whether the VPN is fully down, so connect() would be a plain connect and not a COC. */
    protected boolean isVpnIdle() {
        AtomManager atomManager = atomManager();
        if (atomManager == null) {
            return false;
        }
        String status = atomManager.getCurrentVpnStatus(requireContext());
        return !AtomManager.VPNStatus.CONNECTED.equalsIgnoreCase(status)
                && !AtomManager.VPNStatus.CONNECTING.equalsIgnoreCase(status)
                && !AtomManager.VPNStatus.DISCONNECTING.equalsIgnoreCase(status)
                && !AtomManager.VPNStatus.PAUSED.equalsIgnoreCase(status)
                && !AtomManager.VPNStatus.PAUSING.equalsIgnoreCase(status);
    }

    /** Builds VPNProperties from the current selection and calls connect(). */
    private void dial(@NonNull AtomManager atomManager, boolean overConnection) {
        String call = getString(R.string.call_connect);
        try {
            VPNProperties.Builder builder = newPropertiesBuilder();
            if (builder == null) {
                return;
            }
            // Connection Features and Split Tunneling from the More screen.
            DemoSettings.get(requireContext()).applyTo(builder, dialing());

            if (!TextUtils.isEmpty(vpnUsername) && !TextUtils.isEmpty(vpnPassword)) {
                atomManager.setVPNCredentials(new VPNCredentials(vpnUsername, vpnPassword));
            } else if (!TextUtils.isEmpty(uuid)) {
                atomManager.setUUID(uuid);
            }

            dialedTarget = targetDescription();
            cocInProgress = overConnection;
            atomManager.connect(requireActivity(), builder.build());
            showResult(true, call, overConnection
                    ? getString(R.string.switching_location, dialedTarget)
                    : getString(R.string.connecting));
        } catch (AtomValidationException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
            showResult(false, call, e.getMessage());
        }
    }

    private void onDisconnectClick() {
        AtomManager atomManager = atomManager();
        if (atomManager == null) {
            return;
        }
        if (AtomManager.VPNStatus.CONNECTING.equalsIgnoreCase(
                atomManager.getCurrentVpnStatus(requireContext()))) {
            atomManager.cancel(requireActivity());
        } else {
            atomManager.disconnect(requireActivity());
        }
    }

    private void onReconnectClick() {
        AtomManager atomManager = atomManager();
        if (atomManager == null) {
            return;
        }
        atomManager.reconnect(requireActivity());
        showResult(true, getString(R.string.call_reconnect), getString(R.string.connecting));
    }

    private void onPauseClick() {
        AtomManager atomManager = atomManager();
        if (atomManager == null) {
            return;
        }
        Utilities.getPauseTimerList(requireActivity(), timer -> {
            atomManager.pause(timer);
            showResult(true, getString(R.string.call_pause), Utilities.pauseTimerLabel(requireContext(), timer));
        });
    }

    private void onResumeClick() {
        AtomManager atomManager = atomManager();
        if (atomManager == null) {
            return;
        }
        atomManager.resume();
        showResult(true, getString(R.string.call_resume), getString(R.string.connecting));
    }

    /**
     * For SDK callbacks, which arrive off the main thread and can outlive the fragment: the call
     * label is resolved inside the runnable, once it is known the fragment is still attached.
     */
    private void postResult(boolean success, @StringRes int call, @Nullable String message) {
        mainHandler.post(() -> {
            if (isAdded()) {
                showResult(success, getString(call), message);
            }
        });
    }

    private void showResult(boolean success, @NonNull String call, @Nullable String message) {
        if (resultCard == null) {
            return;
        }
        resultCard.setVisibility(View.VISIBLE);
        resultCard.setBackgroundResource(success ? R.drawable.bg_result_success : R.drawable.bg_result_error);
        ivResultIcon.setImageResource(success ? R.drawable.check_circle : R.drawable.info);
        // check_circle carries its own colours; info is a plain glyph that needs lifting off the red.
        ImageViewCompat.setImageTintList(ivResultIcon, success ? null
                : ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.result_error_title)));
        tvResultTitle.setText(getString(
                success ? R.string.result_success_format : R.string.result_error_format,
                call.toUpperCase(Locale.ROOT)));
        tvResultTitle.setTextColor(ContextCompat.getColor(requireContext(),
                success ? R.color.result_success_title : R.color.result_error_title));
        tvResultMessage.setText(getString(R.string.result_message_format,
                message == null ? "" : message));
    }

    // endregion

    // region Status

    private void refreshStatus() {
        if (tvSetupStatus == null) {
            // The view is gone; the next onResume re-reads the state.
            return;
        }
        AtomManager atomManager = atomManager();
        applyStatus(atomManager == null
                ? AtomManager.VPNStatus.DISCONNECTED
                : atomManager.getCurrentVpnStatus(requireContext()));
    }

    private void postStatus(final String status) {
        mainHandler.post(() -> applyStatus(status));
    }

    private void applyStatus(@Nullable String status) {
        if (tvSetupStatus == null) {
            // A callback outlived the view; the next onResume re-reads the state.
            return;
        }

        boolean connected = AtomManager.VPNStatus.CONNECTED.equalsIgnoreCase(status);
        boolean connecting = AtomManager.VPNStatus.CONNECTING.equalsIgnoreCase(status);
        boolean disconnecting = AtomManager.VPNStatus.DISCONNECTING.equalsIgnoreCase(status);
        boolean paused = AtomManager.VPNStatus.PAUSED.equalsIgnoreCase(status)
                || AtomManager.VPNStatus.PAUSING.equalsIgnoreCase(status);
        boolean idle = !connected && !connecting && !disconnecting && !paused;

        @StringRes int label;
        if (connected) {
            label = R.string.connected;
        } else if (connecting) {
            label = R.string.connecting;
        } else if (disconnecting) {
            label = R.string.disconnecting;
        } else if (paused) {
            label = R.string.paused;
        } else {
            label = R.string.disconnected;
        }

        tvSetupStatus.setText(label);
        initialSetupHeaderStatus.setBackgroundResource(
                connected ? R.drawable.bg_setup_step_done : R.drawable.bg_not_setup_step);
        vStatusDot.setBackgroundResource(
                connected ? R.drawable.bg_green_circle : R.drawable.bg_red_circle);
        tvSetupStatus.setTextColor(ContextCompat.getColor(requireContext(),
                connected ? R.color.ready_font_color : R.color.red_circle));

        if (!connected) {
            locationShown = false;
            tvLocation.setText(R.string.not_connected);
        } else if (!locationShown) {
            locationShown = true;
            loadConnectedLocation();
        }

        // Connect doubles as Cancel while dialling, the only time both it and a tunnel coexist.
        btnConnect.setText(connecting ? R.string.cancel : R.string.connect);
        btnConnect.setEnabled(idle || connecting);
        btnDisconnect.setEnabled(connected || connecting || paused);
        btnReconnect.setEnabled(!connecting && !disconnecting);

        layPause.setVisibility(paused ? View.GONE : View.VISIBLE);
        layResume.setVisibility(paused ? View.VISIBLE : View.GONE);
        btnPause.setEnabled(connected);
        btnResume.setEnabled(paused);

        // Traffic only flows over a live tunnel; the row reappears with the next packet update.
        if (layTraffic != null && !connected) {
            layTraffic.setVisibility(View.GONE);
        }

        // COC needs a live tunnel to replace; connect() on an idle VPN is a plain connect.
        boolean tunnelUp = connected || paused;
        btnConnectionOverConnection.setEnabled(tunnelUp);

        // Selectable while idle (for Connect) and while a tunnel is up (the target for COC);
        // locked only mid-transition.
        boolean selectable = idle || tunnelUp;
        List<View> selectors = new ArrayList<>();
        for (View selector : targetSelectors()) {
            selectors.add(selector);
        }
        if (btnSelectPrimaryProtocol != null) {
            selectors.add(btnSelectPrimaryProtocol);
            selectors.add(btnSelectSecondaryProtocol);
            selectors.add(btnSelectTertiaryProtocol);
        }
        for (View selector : selectors) {
            selector.setEnabled(selectable);
            selector.setAlpha(selectable ? 1f : 0.5f);
        }
    }

    /**
     * Asks getConnectedLocation() where the tunnel is and shows "City, Country", or whichever of
     * the two the SDK has. Falls back to what this screen dialled (e.g. a dedicated host).
     */
    private void loadConnectedLocation() {
        AtomManager atomManager = atomManager();
        if (atomManager == null) {
            showConnectedLocation(null);
            return;
        }
        tvLocation.setText(R.string.loading);
        atomManager.getConnectedLocation(new Callback<Location>() {
            @Override
            public void onSuccess(Location location) {
                mainHandler.post(() -> showConnectedLocation(location));
            }

            @Override
            public void onError(AtomException exception) {
                mainHandler.post(() -> showConnectedLocation(null));
            }

            @Override
            public void onNetworkError(AtomException exception) {
                mainHandler.post(() -> showConnectedLocation(null));
            }
        });
    }

    private void showConnectedLocation(@Nullable Location location) {
        if (tvLocation == null || !locationShown) {
            // The view is gone, or the tunnel dropped while the call was out.
            return;
        }
        String city = location == null || location.getCity() == null
                ? null : location.getCity().getName();
        String country = location == null || location.getCountry() == null
                ? null : location.getCountry().getName();
        if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(country)) {
            tvLocation.setText(city + ", " + country);
        } else if (!TextUtils.isEmpty(city) || !TextUtils.isEmpty(country)) {
            tvLocation.setText(TextUtils.isEmpty(city) ? country : city);
        } else {
            tvLocation.setText(dialedTarget != null ? dialedTarget : getString(R.string.not_available));
        }
    }

    // endregion

    // region VPNStateListener

    @Override
    public void onStateChange(String state) {
        Log.e(getClass().getSimpleName(), state);
        postStatus(state);
    }

    @Override
    public void onConnected() {
        postStatus(AtomManager.VPNStatus.CONNECTED);
    }

    @Override
    public void onConnected(ConnectionDetails connectionDetails) {
        Log.d(getClass().getSimpleName(), "Connected");
        mainHandler.post(() -> {
            cocInProgress = false;
            boolean resumed = resumePending;
            resumePending = false;
            if (!isAdded()) {
                return;
            }
            if (resumed) {
                // A resume also lands here; ConnectionDetails says whether the pause timer ran out.
                boolean auto = connectionDetails != null && connectionDetails.isVPNAutoResume();
                showResult(true, getString(R.string.call_resume),
                        getString(auto ? R.string.resumed_automatically : R.string.resumed_manually));
            } else if (connectionDetails != null && connectionDetails.isDialedWithRecommendedProtocol()
                    && connectionDetails.getProtocol() != null) {
                showResult(true, getString(R.string.call_connect), getString(
                        R.string.connected_with_recommended, connectionDetails.getProtocol().getName()));
            } else {
                showResult(true, getString(R.string.call_connect), getString(R.string.connected));
            }
        });
        postStatus(AtomManager.VPNStatus.CONNECTED);
    }

    @Override
    public void onConnecting() {
        postStatus(AtomManager.VPNStatus.CONNECTING);
    }

    @Override
    public void onConnecting(VPNProperties vpnProperties, AtomConfiguration atomConfiguration) {
        postStatus(AtomManager.VPNStatus.CONNECTING);
    }

    @Override
    public void onRedialing(AtomException atomException, ConnectionDetails connectionDetails) {
        Log.d(getClass().getSimpleName(), atomException.getMessage());
        postStatus(AtomManager.VPNStatus.CONNECTING);
    }

    @Override
    public void onDialError(AtomException atomException, ConnectionDetails connectionDetails) {
        String tag = getClass().getSimpleName();
        Log.d(tag, atomException.getCode() + "");
        Log.d(tag, atomException.getMessage());

        if (atomException.getException() instanceof AtomAPIException) {
            AtomAPIException atomAPIException = (AtomAPIException) atomException.getException();
            Log.d(tag, atomAPIException.getErrorMessage() + " - " + atomAPIException.getCode());
        }

        int code = atomException.getCode();
        if (code == Errors._5194 || code == Errors._5198) {
            // resume() failing is reported here rather than through onPaused.
            mainHandler.post(() -> resumePending = false);
            postResult(false, R.string.call_resume, pauseErrorMessage(atomException));
        } else if (code != Errors._5039) {
            // 5039 is the SDK reporting a user-initiated cancel, which onDisconnected covers.
            postResult(false, R.string.call_connect, atomException.getMessage());
        }

        mainHandler.post(() -> {
            cocInProgress = false;
            refreshStatus();
        });
    }

    @Override
    public void onPaused(AtomException e, ConnectionDetails connectionDetails) {
        if (e != null) {
            // Pause refused (not connected, Always-On active, feature not enabled): state unchanged.
            postResult(false, R.string.call_pause, pauseErrorMessage(e));
            mainHandler.post(this::refreshStatus);
            return;
        }
        postStatus(AtomManager.VPNStatus.PAUSED);
        PauseVPNTimer timer = connectionDetails == null ? null : connectionDetails.getPauseVPNTimer();
        mainHandler.post(() -> {
            resumePending = true;
            if (!isAdded()) {
                return;
            }
            showResult(true, getString(R.string.call_pause), timer == null || timer == PauseVPNTimer.MANUAL
                    ? getString(R.string.paused_until_resumed)
                    : getString(R.string.paused_for, Utilities.pauseTimerLabel(requireContext(), timer)));
        });
    }

    /** Readable text for the pause/resume error codes the SDK documents; else its own message. */
    @Nullable
    private String pauseErrorMessage(@NonNull AtomException e) {
        int code = e.getCode();
        int message;
        if (code == Errors._5194) {
            message = R.string.error_resume_not_paused;
        } else if (code == Errors._5195) {
            message = R.string.error_pause_not_connected;
        } else if (code == Errors._5196) {
            message = R.string.error_pause_always_on;
        } else if (code == Errors._5198) {
            message = R.string.error_pause_not_enabled;
        } else {
            return e.getMessage();
        }
        // Application context: this runs on the SDK thread, possibly after the screen is gone.
        return AtomDemoApplicationController.getInstance().getString(message) + " (" + code + ")";
    }

    @Override
    public void onDisconnecting(ConnectionDetails connectionDetails) {
        postStatus(AtomManager.VPNStatus.DISCONNECTING);
    }

    @Override
    public void onDisconnected(boolean isCancelled) {
        postStatus(AtomManager.VPNStatus.DISCONNECTED);
    }

    @Override
    public void onDisconnected(ConnectionDetails connectionDetails) {
        boolean cancelled = connectionDetails != null && connectionDetails.isCancelled();
        Log.d(getClass().getSimpleName(), cancelled ? "Cancelled" : "Disconnected");
        postStatus(AtomManager.VPNStatus.DISCONNECTED);
        mainHandler.post(() -> {
            // Disconnected while paused: the next onConnected is a fresh connect, not a resume.
            resumePending = false;
            // During COC this is the SDK tearing down the old tunnel, not a user disconnect(),
            // so the "Switching to ..." result stays up until the new tunnel reports.
            if (!cocInProgress && isAdded()) {
                showResult(true, getString(R.string.call_disconnect),
                        getString(cancelled ? R.string.cancel : R.string.disconnected));
            }
        });
    }

    @Override
    public void onUnableToAccessInternet(AtomException atomException,
                                         ConnectionDetails connectionDetails) {
        // Not a state transition.
    }

    @Override
    public void onPacketsTransmitted(String in, String out, String inSpeed, String outSpeed) {
        // Fires about once a second while connected over TCP/UDP: totals first, then speeds.
        mainHandler.post(() -> {
            if (layTraffic == null || !isAdded()) {
                return;
            }
            layTraffic.setVisibility(View.VISIBLE);
            tvTrafficDown.setText(getString(R.string.traffic_format, inSpeed, in));
            tvTrafficUp.setText(getString(R.string.traffic_format, outSpeed, out));
        });
    }

    // endregion

    // region Tracker / Ad Blocker

    /** The row only shows when the feature is on and this connection type supports it. */
    private void renderShield() {
        if (layShield == null || !isAdded()) {
            return;
        }
        boolean enabled = DemoSettings.get(requireContext()).isTrackerAdBlockerEnabled()
                && dialing() != DemoSettings.Dialing.CHANNEL;
        layShield.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) {
            return;
        }
        tvShieldStatus.setText(shieldStatus == null
                ? getString(R.string.shield_waiting) : shieldStatus);
        tvShieldBlocked.setText(getResources().getQuantityString(
                R.plurals.shield_blocked_count, shieldBlocked, shieldBlocked));
    }

    @NonNull
    private String describeShieldStatus(@Nullable AtomShieldStatus status) {
        if (status instanceof AtomShieldStatus.Establishing) {
            return getString(R.string.shield_establishing);
        } else if (status instanceof AtomShieldStatus.Established) {
            return getString(R.string.shield_established);
        } else if (status instanceof AtomShieldStatus.Disconnected) {
            return getString(R.string.shield_disconnected);
        } else if (status instanceof AtomShieldStatus.Error) {
            AtomException e = ((AtomShieldStatus.Error) status).getException();
            return e == null ? getString(R.string.shield_error)
                    : getString(R.string.shield_error_format, e.getMessage(), e.getCode());
        }
        return getString(R.string.shield_waiting);
    }

    // endregion
}
