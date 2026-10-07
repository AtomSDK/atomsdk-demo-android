/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atom.core.exceptions.AtomException;
import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.Channel;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.DemoSettings;
import com.atom.vpn.demo.common.CoachMarks;

import java.util.ArrayList;
import java.util.List;

/**
 * Connect by channel: pick up to three protocols and a streaming channel. The channel list only
 * offers channels that support every selected protocol.
 */
public class ConnectWithChannelFragment extends BaseConnectFragment {

    /** From getChannels(); null until it returns. */
    @Nullable private List<Channel> channels;

    @Nullable private Channel selectedChannel;

    private View btnSelectChannel;
    private TextView tvSelectedChannel;

    public ConnectWithChannelFragment() {
        // Required empty public constructor
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_connect_with_channels;
    }

    @Override
    protected void onBindTarget(@NonNull View root) {
        btnSelectChannel = root.findViewById(R.id.btnSelectChannel);
        tvSelectedChannel = root.findViewById(R.id.tvSelectedChannel);
        btnSelectChannel.setOnClickListener(v -> pickChannel());
        CoachMarks.set(btnSelectChannel, R.string.coach_select_channel);
        renderSelection();
    }

    @Override
    protected void onUnbindTarget() {
        btnSelectChannel = null;
        tvSelectedChannel = null;
    }

    @NonNull
    @Override
    protected View[] targetSelectors() {
        return new View[]{btnSelectChannel};
    }

    @Override
    protected void onLoadInventory(@NonNull AtomManager atomManager) {
        atomManager.getChannels(new CollectionCallback<Channel>() {
            @Override
            public void onSuccess(List<Channel> list) {
                mainHandler.post(() -> channels = list);
            }

            @Override
            public void onError(AtomException atomException) {
                logError(atomException);
            }

            @Override
            public void onNetworkError(AtomException atomException) {
                logError(atomException);
            }
        });
    }

    /** A protocol change can leave the chosen channel unable to dial it; drop it then. */
    @Override
    protected void onProtocolsChanged() {
        if (selectedChannel != null && !supportsSelectedProtocols(selectedChannel.getProtocols())) {
            selectedChannel = null;
        }
        renderSelection();
    }

    @NonNull
    @Override
    protected DemoSettings.Dialing dialing() {
        return DemoSettings.Dialing.CHANNEL;
    }

    @Override
    protected boolean supportsRecommendedProtocol() {
        return true;
    }

    @Nullable
    @Override
    protected VPNProperties.Builder newPropertiesBuilder() throws AtomValidationException {
        Protocol primary = selectedChannel == null ? null
                : primaryFor(selectedChannel.getRecommendedProtocol(), selectedChannel.getProtocols());
        if (primary == null) {
            toast(R.string.select_protocol_and_channel);
            return null;
        }

        VPNProperties.Builder builder = new VPNProperties.Builder(selectedChannel, primary);
        if (secondaryProtocol != null) {
            builder.withSecondaryProtocol(secondaryProtocol);
        }
        if (tertiaryProtocol != null) {
            builder.withTertiaryProtocol(tertiaryProtocol);
        }
        return builder;
    }

    @NonNull
    @Override
    protected String targetDescription() {
        return selectedChannel == null
                ? getString(R.string.not_available) : selectedChannel.getName();
    }

    private void pickChannel() {
        if (channels == null) {
            toast(R.string.loading);
            return;
        }
        List<Channel> available = new ArrayList<>();
        for (Channel channel : channels) {
            if (supportsSelectedProtocols(channel.getProtocols())) {
                available.add(channel);
            }
        }
        if (available.isEmpty()) {
            toast(R.string.no_channels_for_protocols);
            return;
        }
        String[] names = new String[available.size()];
        for (int i = 0; i < available.size(); i++) {
            names[i] = available.get(i).getName();
        }
        showPicker(R.string.lbl_select_channel, names, which -> {
            selectedChannel = available.get(which);
            renderSelection();
        });
    }

    private void renderSelection() {
        if (tvSelectedChannel == null) {
            return;
        }
        tvSelectedChannel.setText(selectedChannel == null
                ? getString(R.string.lbl_select_channel) : selectedChannel.getName());
    }
}
