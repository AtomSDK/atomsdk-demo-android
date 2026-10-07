/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atom.core.exceptions.AtomException;
import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.DedicatedDNS;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.DemoSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Connect to one of several dedicated hosts reserved for the user. Each host is a
 * {@link DedicatedDNS} (host name, IP, port and its own protocol); the SDK dials them in turn,
 * optionally preferring one protocol across all of them.
 */
public class ConnectWithMultipleDedicatedIPFragment extends BaseConnectFragment {

    /** The reference demo offers four hosts; enough to show the fallback without an endless form. */
    private static final int MAX_HOSTS = 4;

    /** One host card and the protocol picked on it. */
    private static final class HostRow {
        final View view;
        final EditText etName;
        final EditText etIp;
        final EditText etPort;
        final TextView tvProtocol;
        @Nullable Protocol protocol;

        HostRow(@NonNull View view) {
            this.view = view;
            etName = view.findViewById(R.id.etHostName);
            etIp = view.findViewById(R.id.etHostIp);
            etPort = view.findViewById(R.id.etHostPort);
            tvProtocol = view.findViewById(R.id.tvHostProtocol);
        }
    }

    private final List<HostRow> hostRows = new ArrayList<>();

    /** Null is "Automatic": no preferred protocol, so each host dials with its own. */
    @Nullable private Protocol sharedProtocol;

    private LinearLayout llHosts;
    private View btnAddHost;
    private View btnPreferredProtocol;
    private TextView tvPreferredProtocol;

    public ConnectWithMultipleDedicatedIPFragment() {
        // Required empty public constructor
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_connect_with_multiple_dedicatedip;
    }

    @Override
    protected void onBindTarget(@NonNull View root) {
        llHosts = root.findViewById(R.id.llHosts);
        btnAddHost = root.findViewById(R.id.btnAddHost);
        btnPreferredProtocol = root.findViewById(R.id.btnPreferredProtocol);
        tvPreferredProtocol = root.findViewById(R.id.tvPreferredProtocol);

        btnAddHost.setOnClickListener(v -> addHost());
        btnPreferredProtocol.setOnClickListener(v -> pickPreferredProtocol());
        CoachMarks.set(btnAddHost, R.string.coach_add_dedicated_host);
        CoachMarks.set(btnPreferredProtocol, R.string.coach_multiple_dedicated_preferred_protocol);

        hostRows.clear();
        addHost();
        renderPreferredProtocol();
    }

    @Override
    protected void onUnbindTarget() {
        hostRows.clear();
        llHosts = null;
        btnAddHost = null;
        btnPreferredProtocol = null;
        tvPreferredProtocol = null;
    }

    @NonNull
    @Override
    protected View[] targetSelectors() {
        List<View> views = new ArrayList<>();
        views.add(btnAddHost);
        views.add(btnPreferredProtocol);
        for (HostRow row : hostRows) {
            views.add(row.etName);
            views.add(row.etIp);
            views.add(row.etPort);
            views.add(row.view.findViewById(R.id.btnHostProtocol));
            views.add(row.view.findViewById(R.id.btnRemoveHost));
        }
        return views.toArray(new View[0]);
    }

    @NonNull
    @Override
    protected DemoSettings.Dialing dialing() {
        return DemoSettings.Dialing.MULTIPLE_DEDICATED_IP;
    }

    /** No protocol chips on this screen, so the base class leaves getProtocols() to it. */
    @Override
    protected void onLoadInventory(@NonNull AtomManager atomManager) {
        atomManager.getProtocols(new CollectionCallback<Protocol>() {
            @Override
            public void onSuccess(List<Protocol> list) {
                mainHandler.post(() -> {
                    protocols = list;
                    if (list == null || list.isEmpty() || llHosts == null) {
                        return;
                    }
                    for (HostRow row : hostRows) {
                        if (row.protocol == null) {
                            row.protocol = preferredProtocol(list);
                        }
                    }
                    renderHosts();
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

    @Nullable
    @Override
    protected VPNProperties.Builder newPropertiesBuilder() throws AtomValidationException {
        List<DedicatedDNS> hosts = new ArrayList<>();
        for (HostRow row : hostRows) {
            String name = row.etName.getText().toString().trim();
            String ip = row.etIp.getText().toString().trim();
            if (name.isEmpty()) {
                row.etName.setError(Constants.HostRequired);
                row.etName.requestFocus();
                return null;
            }
            row.etName.setError(null);
            if (row.protocol == null) {
                toast(R.string.loading);
                return null;
            }

            DedicatedDNS host = new DedicatedDNS();
            host.setDedicatedHostName(name);
            host.setDedicatedHostIP(ip);
            host.setProtocol(row.protocol);
            host.setPortNumber(port(row));
            hosts.add(host);
        }
        return new VPNProperties.Builder(hosts, sharedProtocol);
    }

    @NonNull
    @Override
    protected String targetDescription() {
        List<String> names = new ArrayList<>();
        for (HostRow row : hostRows) {
            String name = row.etName.getText().toString().trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return TextUtils.join(", ", names);
    }

    /** 0 - the SDK's "use the protocol's default" - when blank or out of range. */
    private static int port(@NonNull HostRow row) {
        try {
            int port = Integer.parseInt(row.etPort.getText().toString().trim());
            return port > 0 && port <= 65535 ? port : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // region Hosts

    private void addHost() {
        if (hostRows.size() >= MAX_HOSTS) {
            return;
        }
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_dedicated_host, llHosts, false);
        HostRow row = new HostRow(view);
        if (protocols != null && !protocols.isEmpty()) {
            row.protocol = preferredProtocol(protocols);
        }

        view.findViewById(R.id.btnHostProtocol).setOnClickListener(v -> pickHostProtocol(row));
        view.findViewById(R.id.btnRemoveHost).setOnClickListener(v -> removeHost(row));
        CoachMarks.set(view.findViewById(R.id.btnHostProtocol), R.string.coach_dedicated_host_protocol);

        hostRows.add(row);
        llHosts.addView(view);
        renderHosts();
    }

    private void removeHost(@NonNull HostRow row) {
        if (hostRows.size() <= 1) {
            return;
        }
        hostRows.remove(row);
        llHosts.removeView(row.view);
        renderHosts();
    }

    /** Numbers the cards, keeps the first one unremovable and hides Add once the list is full. */
    private void renderHosts() {
        if (llHosts == null) {
            return;
        }
        for (int i = 0; i < hostRows.size(); i++) {
            HostRow row = hostRows.get(i);
            ((TextView) row.view.findViewById(R.id.tvHostTitle))
                    .setText(getString(R.string.host_number, i + 1));
            row.view.findViewById(R.id.btnRemoveHost)
                    .setVisibility(hostRows.size() > 1 ? View.VISIBLE : View.INVISIBLE);
            row.tvProtocol.setText(row.protocol != null ? row.protocol.getName()
                    : getString(protocols == null ? R.string.loading : R.string.not_available));
        }
        btnAddHost.setVisibility(hostRows.size() < MAX_HOSTS ? View.VISIBLE : View.GONE);
    }

    private void pickHostProtocol(@NonNull HostRow row) {
        if (protocols == null) {
            toast(R.string.loading);
            return;
        }
        List<Protocol> list = protocols;
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            names[i] = list.get(i).getName();
        }
        showPicker(R.string.protocol, names, which -> {
            row.protocol = list.get(which);
            renderHosts();
        });
    }

    // endregion

    // region Preferred protocol

    /** "Automatic" first, then every protocol the SDK offers. */
    private void pickPreferredProtocol() {
        if (protocols == null) {
            toast(R.string.loading);
            return;
        }
        List<Protocol> list = protocols;
        String[] names = new String[list.size() + 1];
        names[0] = getString(R.string.automatic);
        for (int i = 0; i < list.size(); i++) {
            names[i + 1] = list.get(i).getName();
        }
        showPicker(R.string.preferred_protocol, names, which -> {
            sharedProtocol = which == 0 ? null : list.get(which - 1);
            renderPreferredProtocol();
        });
    }

    private void renderPreferredProtocol() {
        if (tvPreferredProtocol == null) {
            return;
        }
        tvPreferredProtocol.setText(sharedProtocol == null
                ? getString(R.string.automatic) : sharedProtocol.getName());
    }

    // endregion
}
