/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.DemoSettings;
import com.atom.vpn.demo.common.DemoSettings.SplitTunnelMode;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * More > Split Tunneling. The mode chooses between VPNProperties.Builder.withSplitTunneling
 * (only selected apps use the VPN) and withReverseSplitTunneling (selected apps bypass it); the
 * list chooses the package names. Saved to {@link DemoSettings} and applied on the next connect().
 */
public class SplitTunnelingFragment extends Fragment {

    private static final class AppEntry {
        final String packageName;
        final String label;
        final Drawable icon;

        AppEntry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService loader = Executors.newSingleThreadExecutor();

    private DemoSettings settings;
    private final Set<String> selected = new HashSet<>();

    @Nullable private AppAdapter adapter;
    @Nullable private ListView lvApps;
    @Nullable private TextView tvEmpty, tvCall, tvAppsTitle;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_split_tunneling, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = DemoSettings.get(requireContext());
        selected.clear();
        selected.addAll(settings.getSplitTunnelApps());

        ((TextView) view.findViewById(R.id.tvTitle)).setText(R.string.split_tunneling);
        view.findViewById(R.id.btnBack).setOnClickListener(
                v -> getParentFragmentManager().popBackStack());

        tvCall = view.findViewById(R.id.tvSplitCall);
        tvAppsTitle = view.findViewById(R.id.tvSplitAppsTitle);
        tvEmpty = view.findViewById(R.id.tvSplitAppsEmpty);
        lvApps = view.findViewById(R.id.lvSplitApps);
        lvApps.setEmptyView(tvEmpty);

        adapter = new AppAdapter(requireContext());
        lvApps.setAdapter(adapter);
        lvApps.setOnItemClickListener((parent, row, position, id) -> {
            AppEntry app = adapter.getItem(position);
            if (app == null || settings.getSplitTunnelMode() == SplitTunnelMode.OFF) {
                return;
            }
            if (!selected.remove(app.packageName)) {
                selected.add(app.packageName);
            }
            settings.setSplitTunnelApps(selected);
            adapter.notifyDataSetChanged();
            renderTitle();
        });

        RadioGroup rgMode = view.findViewById(R.id.rgSplitMode);
        rgMode.check(modeButton(settings.getSplitTunnelMode()));
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            settings.setSplitTunnelMode(checkedId == R.id.rbSplitExclude ? SplitTunnelMode.EXCLUDE
                    : checkedId == R.id.rbSplitOnly ? SplitTunnelMode.ONLY : SplitTunnelMode.OFF);
            renderMode();
        });

        renderMode();
        renderTitle();
        loadApps();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        adapter = null;
        lvApps = null;
        tvEmpty = null;
        tvCall = null;
        tvAppsTitle = null;
    }

    @Override
    public void onDestroy() {
        loader.shutdownNow();
        super.onDestroy();
    }

    private static int modeButton(@NonNull SplitTunnelMode mode) {
        switch (mode) {
            case EXCLUDE:
                return R.id.rbSplitExclude;
            case ONLY:
                return R.id.rbSplitOnly;
            default:
                return R.id.rbSplitOff;
        }
    }

    /** The app list only means something once a mode is picked, so it is dimmed while off. */
    private void renderMode() {
        if (tvCall == null || lvApps == null) {
            return;
        }
        SplitTunnelMode mode = settings.getSplitTunnelMode();
        tvCall.setText(mode == SplitTunnelMode.EXCLUDE ? R.string.call_split_exclude
                : mode == SplitTunnelMode.ONLY ? R.string.call_split_only : R.string.call_split_off);
        lvApps.setAlpha(mode == SplitTunnelMode.OFF ? 0.5f : 1f);
    }

    private void renderTitle() {
        if (tvAppsTitle != null) {
            tvAppsTitle.setText(getString(R.string.split_apps_title, selected.size()));
        }
    }

    /** Launchable apps only (what a user recognises), sorted by name; loaded off the main thread. */
    private void loadApps() {
        Context context = requireContext().getApplicationContext();
        loader.execute(() -> {
            PackageManager pm = context.getPackageManager();
            Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            Set<String> seen = new HashSet<>();
            List<AppEntry> apps = new ArrayList<>();
            for (ResolveInfo info : pm.queryIntentActivities(launcher, 0)) {
                String packageName = info.activityInfo.packageName;
                // One row per app, and never this app itself.
                if (packageName.equals(context.getPackageName()) || !seen.add(packageName)) {
                    continue;
                }
                apps.add(new AppEntry(packageName, String.valueOf(info.loadLabel(pm)),
                        info.loadIcon(pm)));
            }
            Collator collator = Collator.getInstance();
            apps.sort((a, b) -> collator.compare(a.label, b.label));

            mainHandler.post(() -> {
                if (adapter == null || tvEmpty == null) {
                    return;
                }
                tvEmpty.setText(R.string.no_apps_found);
                adapter.clear();
                adapter.addAll(apps);
            });
        });
    }

    private final class AppAdapter extends ArrayAdapter<AppEntry> {

        AppAdapter(@NonNull Context context) {
            super(context, R.layout.item_split_app, new ArrayList<>());
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View row = convertView != null ? convertView
                    : LayoutInflater.from(getContext()).inflate(R.layout.item_split_app, parent, false);
            AppEntry app = getItem(position);
            if (app == null) {
                return row;
            }
            ((ImageView) row.findViewById(R.id.ivAppIcon)).setImageDrawable(app.icon);
            ((TextView) row.findViewById(R.id.tvAppName)).setText(app.label);
            ((TextView) row.findViewById(R.id.tvAppPackage)).setText(app.packageName);
            boolean checked = selected.contains(app.packageName);
            ((CheckBox) row.findViewById(R.id.cbApp)).setChecked(checked);
            row.setContentDescription(app.label);
            return row;
        }
    }
}
