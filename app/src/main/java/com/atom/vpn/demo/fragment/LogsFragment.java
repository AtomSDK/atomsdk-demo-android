/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.adapter.LogEntryAdapter;
import com.atom.vpn.demo.common.logger.LogStore;

import java.util.ArrayList;
import java.util.List;

/**
 * SDK Logs, opened from the More tab. Shows {@link LogStore} as a console with a level filter.
 * Pause is {@link LogStore}'s app-wide switch: nothing is recorded while it is on, whichever
 * screen is showing, and it stays on after leaving this screen.
 */
public class LogsFragment extends Fragment implements LogStore.Listener {

    public static final String ARG_VPN_USERNAME = "vpnUsername";
    public static final String ARG_VPN_PASSWORD = "vpnPassword";

    /** Bottom nav items, in display order. Kept in step with {@link HomeFragment}. */
    private static final int[] NAV_ITEM_IDS = {
            R.id.navDashboard, R.id.navConnection, R.id.navLocations, R.id.navMore
    };

    private String vpnUsername;
    private String vpnPassword;

    private LogEntryAdapter adapter;
    private RadioGroup rgLogFilter;
    private TextView tvPause;
    private TextView tvEmptyLogs;

    public LogsFragment() {
        // Required empty public constructor
    }

    public static LogsFragment newInstance(String vpnUsername, String vpnPassword) {
        Bundle args = new Bundle();
        args.putString(ARG_VPN_USERNAME, vpnUsername);
        args.putString(ARG_VPN_PASSWORD, vpnPassword);

        LogsFragment fragment = new LogsFragment();
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
        return inflater.inflate(R.layout.fragment_logs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new LogEntryAdapter(requireContext());
        ((ListView) view.findViewById(R.id.lvLogs)).setAdapter(adapter);

        rgLogFilter = view.findViewById(R.id.rgLogFilter);
        tvPause = view.findViewById(R.id.tvPause);
        tvEmptyLogs = view.findViewById(R.id.tvEmptyLogs);

        rgLogFilter.setOnCheckedChangeListener((group, checkedId) -> render());

        view.findViewById(R.id.btnBack).setOnClickListener(
                v -> requireActivity().getSupportFragmentManager().popBackStack());
        view.findViewById(R.id.btnClear).setOnClickListener(v -> LogStore.get().clear());
        view.findViewById(R.id.btnPause).setOnClickListener(
                v -> LogStore.get().setPaused(!LogStore.get().isPaused()));
        view.findViewById(R.id.btnCopy).setOnClickListener(v -> copyVisibleLogs());

        CoachMarks.set(view, R.string.coach_logs_back, R.id.btnBack);
        CoachMarks.set(view, R.string.coach_logs_clear, R.id.btnClear);
        CoachMarks.set(view, R.string.coach_logs_pause, R.id.btnPause);
        CoachMarks.set(view, R.string.coach_logs_copy, R.id.btnCopy);
        CoachMarks.set(view, R.string.coach_logs_filter, R.id.rbLogAll, R.id.rbLogInfo,
                R.id.rbLogSuccess, R.id.rbLogWarning, R.id.rbLogError);
        CoachMarks.setBottomNav(view);

        setUpBottomNav(view);

        render();
        LogStore.get().addListener(this);
    }

    @Override
    public void onDestroyView() {
        LogStore.get().removeListener(this);
        adapter = null;
        rgLogFilter = null;
        tvPause = null;
        tvEmptyLogs = null;
        super.onDestroyView();
    }

    @Override
    public void onLogsChanged() {
        render();
    }

    private void render() {
        tvPause.setText(LogStore.get().isPaused() ? R.string.resume : R.string.pause);

        List<LogStore.Entry> visible = visibleEntries();
        adapter.submit(visible);
        tvEmptyLogs.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private List<LogStore.Entry> visibleEntries() {
        LogStore.Level level = selectedLevel();
        List<LogStore.Entry> all = LogStore.get().snapshot();
        if (level == null) {
            return all;
        }

        List<LogStore.Entry> filtered = new ArrayList<>();
        for (LogStore.Entry entry : all) {
            if (entry.level == level) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    /** Null means "All". */
    @Nullable
    private LogStore.Level selectedLevel() {
        int checked = rgLogFilter.getCheckedRadioButtonId();
        if (checked == R.id.rbLogInfo) return LogStore.Level.INFO;
        if (checked == R.id.rbLogSuccess) return LogStore.Level.SUCCESS;
        if (checked == R.id.rbLogWarning) return LogStore.Level.WARN;
        if (checked == R.id.rbLogError) return LogStore.Level.ERROR;
        return null;
    }

    /** Copies what the console is showing, filter applied. */
    private void copyVisibleLogs() {
        List<LogStore.Entry> shown = new ArrayList<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            shown.add(adapter.getItem(i));
        }
        if (shown.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_logs, Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(
                getString(R.string.sdk_logs), adapter.toPlainText(shown)));
        Toast.makeText(requireContext(), R.string.logs_copied, Toast.LENGTH_SHORT).show();
    }

    /**
     * Opened from More or from any screen's top bar icon, so More shows the More tab rather than
     * popping back to whatever was underneath. Dashboard and Locations clear the tab back stack,
     * which takes this screen with it.
     * Connection opens Connection With Params.
     */
    private void setUpBottomNav(@NonNull View root) {
        for (int id : NAV_ITEM_IDS) {
            root.findViewById(id).setOnClickListener(v -> {
                FragmentManager fm = requireActivity().getSupportFragmentManager();
                if (v.getId() == R.id.navDashboard) {
                    fm.popBackStack(HomeFragment.TAB_BACK_STACK,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } else if (v.getId() == R.id.navLocations) {
                    HomeFragment.showTab(this,
                            LocationsFragment.newInstance(vpnUsername, vpnPassword));
                } else if (v.getId() == R.id.navMore) {
                    HomeFragment.showTab(this, MoreFragment.newInstance(vpnUsername, vpnPassword));
                } else if (v.getId() == R.id.navConnection) {
                    HomeFragment.openSelectedConnectScreen(this, vpnUsername, vpnPassword);
                } else {
                    selectTab(root, v.getId());
                }
            });
        }
        selectTab(root, R.id.navMore);
    }

    private void selectTab(@NonNull View root, @IdRes int tabId) {
        for (int id : NAV_ITEM_IDS) {
            root.findViewById(id).setSelected(id == tabId);
        }
    }
}
