/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.DemoSettings;

/**
 * More > Connection Features: the VPNProperties / AtomConfiguration options from the SDK README
 * that are not tied to a single connect screen. Each change is saved straight to
 * {@link DemoSettings}; the connect screens apply them on their next connect().
 */
public class ConnectionFeaturesFragment extends Fragment {

    private interface Setter {
        void set(boolean value);
    }

    private DemoSettings settings;
    @Nullable private View layManualPort;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connection_features, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = DemoSettings.get(requireContext());

        ((TextView) view.findViewById(R.id.tvTitle)).setText(R.string.connection_features);
        view.findViewById(R.id.btnBack).setOnClickListener(
                v -> getParentFragmentManager().popBackStack());

        LinearLayout privacy = view.findViewById(R.id.containerPrivacy);
        addToggle(privacy, R.string.tracker_ad_blocker, R.string.tracker_ad_blocker_summary,
                R.string.call_tracker_ad_blocker, settings.isTrackerAdBlockerEnabled(),
                settings::setTrackerAdBlockerEnabled);
        addToggle(privacy, R.string.lan_access, R.string.lan_access_summary,
                R.string.call_lan_access, settings.isLanAccessEnabled(),
                settings::setLanAccessEnabled);

        LinearLayout dialing = view.findViewById(R.id.containerDialing);
        addToggle(dialing, R.string.protocol_switch, R.string.protocol_switch_summary,
                R.string.call_protocol_switch, settings.isProtocolSwitchEnabled(),
                settings::setProtocolSwitchEnabled);
        addToggle(dialing, R.string.failover, R.string.failover_summary,
                R.string.call_failover, settings.isFailoverEnabled(),
                settings::setFailoverEnabled);

        // The params card already holds the port and server filter inputs; switches go on top.
        LinearLayout params = view.findViewById(R.id.containerParams);
        layManualPort = view.findViewById(R.id.layManualPort);
        insertToggle(params, 0, R.string.automatic_port, R.string.automatic_port_summary,
                R.string.call_automatic_port, settings.isAutomaticPortEnabled(), enabled -> {
                    settings.setAutomaticPortEnabled(enabled);
                    renderPortMode();
                });
        bindManualPort(view.findViewById(R.id.etManualPort));
        bindServerFilter(view);
        renderPortMode();

        LinearLayout sdk = view.findViewById(R.id.containerSdk);
        addToggle(sdk, R.string.persist_vpn_details, R.string.persist_vpn_details_summary,
                R.string.call_persist_vpn_details, settings.isPersistVpnDetailsEnabled(), enabled -> {
                    settings.setPersistVpnDetailsEnabled(enabled);
                    // Only read at AtomManager.initialize(), in the Application class.
                    Toast.makeText(requireContext(), R.string.applies_next_launch,
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        layManualPort = null;
    }

    private void addToggle(@NonNull LinearLayout container, @StringRes int title,
                           @StringRes int summary, @StringRes int call, boolean checked,
                           @NonNull Setter setter) {
        insertToggle(container, container.getChildCount(), title, summary, call, checked, setter);
    }

    /**
     * Inflates a switch row at {@code index}, separated by a divider from whatever follows it (or,
     * when appended, from the row above). Returns the index after it.
     */
    private int insertToggle(@NonNull LinearLayout container, int index, @StringRes int title,
                             @StringRes int summary, @StringRes int call, boolean checked,
                             @NonNull Setter setter) {
        if (index > 0 && index == container.getChildCount()) {
            container.addView(divider(), index++);
        }

        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_feature_toggle, container, false);
        ((TextView) row.findViewById(R.id.tvFeatureTitle)).setText(title);
        ((TextView) row.findViewById(R.id.tvFeatureSummary)).setText(summary);
        ((TextView) row.findViewById(R.id.tvFeatureCall)).setText(call);

        SwitchCompat toggle = row.findViewById(R.id.swFeature);
        toggle.setChecked(checked);
        toggle.setContentDescription(getString(title));
        toggle.setOnCheckedChangeListener((CompoundButton button, boolean isChecked) ->
                setter.set(isChecked));
        row.setOnClickListener(v -> toggle.toggle());

        container.addView(row, index++);
        if (index < container.getChildCount()) {
            container.addView(divider(), index++);
        }
        return index;
    }

    @NonNull
    private View divider() {
        View divider = new View(requireContext());
        int height = Math.max(1, Math.round(getResources().getDisplayMetrics().density));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height));
        divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.borderColor));
        return divider;
    }

    private void renderPortMode() {
        if (layManualPort != null) {
            layManualPort.setVisibility(settings.isAutomaticPortEnabled() ? View.GONE : View.VISIBLE);
        }
    }

    /** Saved as it is typed; an out-of-range port is flagged and not saved. */
    private void bindManualPort(@NonNull EditText etPort) {
        etPort.setText(String.valueOf(settings.getManualPort()));
        etPort.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                int port;
                try {
                    port = Integer.parseInt(s.toString().trim());
                } catch (NumberFormatException e) {
                    port = -1;
                }
                if (port < 1 || port > 65535) {
                    etPort.setError(getString(R.string.invalid_port));
                    return;
                }
                etPort.setError(null);
                settings.setManualPort(port);
            }
        });
    }

    private void bindServerFilter(@NonNull View view) {
        EditText etNas = view.findViewById(R.id.etServerFilterNas);
        etNas.setText(settings.getServerFilterNas());
        etNas.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                settings.setServerFilterNas(s.toString());
            }
        });

        RadioGroup rgType = view.findViewById(R.id.rgServerFilterType);
        rgType.check(settings.isServerFilterInclude()
                ? R.id.rbServerFilterInclude : R.id.rbServerFilterExclude);
        rgType.setOnCheckedChangeListener((group, checkedId) ->
                settings.setServerFilterInclude(checkedId == R.id.rbServerFilterInclude));
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
