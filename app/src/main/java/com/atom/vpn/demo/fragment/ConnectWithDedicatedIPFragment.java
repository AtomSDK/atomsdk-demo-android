/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.ProtocolName;
import com.atom.sdk.android.ProtocolType;
import com.atom.sdk.android.VPNProperties;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.DemoSettings;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.common.Constants;

/**
 * Connect to a dedicated IP or host reserved for the user. The protocol is fixed to IKEv2, the
 * only one this demo dials dedicated hosts with, so the screen has no protocol chips.
 */
public class ConnectWithDedicatedIPFragment extends BaseConnectFragment {

    private EditText etDedicatedIP;

    public ConnectWithDedicatedIPFragment() {
        // Required empty public constructor
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_connect_with_dedicatedip;
    }

    @Override
    protected void onBindTarget(@NonNull View root) {
        etDedicatedIP = root.findViewById(R.id.etDedicatedIP);
        CoachMarks.set(root.findViewById(R.id.dedicatedProtocolChip),
                R.string.coach_dedicated_protocol);
    }

    @Override
    protected void onUnbindTarget() {
        etDedicatedIP = null;
    }

    @NonNull
    @Override
    protected View[] targetSelectors() {
        return new View[]{etDedicatedIP};
    }

    @NonNull
    @Override
    protected DemoSettings.Dialing dialing() {
        return DemoSettings.Dialing.DEDICATED_IP;
    }

    @Override
    protected void onLoadInventory(@NonNull AtomManager atomManager) {
        // Nothing to fetch: the host is typed in and the protocol is fixed.
    }

    @Nullable
    @Override
    protected VPNProperties.Builder newPropertiesBuilder() throws AtomValidationException {
        String host = host();
        if (host.isEmpty()) {
            etDedicatedIP.setError(Constants.HostRequired);
            return null;
        }
        etDedicatedIP.setError(null);
        return new VPNProperties.Builder(host, dedicatedProtocol());
    }

    @NonNull
    @Override
    protected String targetDescription() {
        return host();
    }

    @NonNull
    private String host() {
        return etDedicatedIP == null ? "" : etDedicatedIP.getText().toString().trim();
    }

    @NonNull
    private static Protocol dedicatedProtocol() {
        Protocol protocol = new Protocol();
        protocol.setName(ProtocolName.IKEV);
        protocol.setProtocol(ProtocolType.IKEV);
        return protocol;
    }
}
