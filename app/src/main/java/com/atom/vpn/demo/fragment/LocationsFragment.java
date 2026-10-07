/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.atom.core.exceptions.AtomException;
import com.atom.core.models.City;
import com.atom.core.models.Country;
import com.atom.core.models.Location;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.data.callbacks.Callback;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.adapter.LocationAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Locations tab: the SDK inventory rendered as a searchable server list, with the SDK's own
 * recommended location (country and city) pinned above it. Connect hands the country to the normal Connection With
 * Params screen, which dials it with connect() and shows the result there.
 */
public class LocationsFragment extends Fragment implements LocationAdapter.OnConnectClickListener {

    public static final String ARG_VPN_USERNAME = "vpnUsername";
    public static final String ARG_VPN_PASSWORD = "vpnPassword";

    /** Bottom nav items, in display order. Kept in step with {@link HomeFragment}. */
    private static final int[] NAV_ITEM_IDS = {
            R.id.navDashboard, R.id.navConnection, R.id.navLocations, R.id.navMore
    };

    /** Inventory callbacks are not guaranteed to arrive on the main thread. */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String vpnUsername;
    private String vpnPassword;

    private LocationAdapter adapter;
    private TextView tvEmpty;
    private View llRecommended;
    private ImageView ivRecommendedFlag;
    private TextView tvRecommendedCountry;
    private TextView tvRecommendedCity;
    private TextView tvRecommendedPing;
    private View vRecommendedPingDot;
    private TextView btnConnectRecommended;
    private View btnReload;
    private View btnPing;

    /** Full inventory; the list view shows whatever the search box narrows it down to. */
    private final List<Country> allCountries = new ArrayList<>();

    @Nullable
    private Country recommendedCountry;

    /** City of the recommended location, when the SDK names one. */
    @Nullable
    private City recommendedCity;

    /**
     * Latest getOptimizedCountries result by {@link LocationAdapter#keyOf}. The three inventory
     * calls return in any order, so latencies are kept here and applied to whichever of the list
     * and the recommended card arrives later, not only to what happened to be loaded already.
     */
    private final Map<String, Integer> latencies = new HashMap<>();

    /**
     * getCities result grouped by {@link #cityKey}. getCountries leaves Country.getCities empty,
     * so the cities come from their own call and, like latencies, are attached to whichever
     * countries are loaded when either call returns.
     */
    private final Map<String, List<City>> citiesByCountry = new HashMap<>();

    private String searchQuery = "";

    public LocationsFragment() {
        // Required empty public constructor
    }

    public static LocationsFragment newInstance(String vpnUsername, String vpnPassword) {
        Bundle args = new Bundle();
        args.putString(ARG_VPN_USERNAME, vpnUsername);
        args.putString(ARG_VPN_PASSWORD, vpnPassword);

        LocationsFragment fragment = new LocationsFragment();
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
        return inflater.inflate(R.layout.fragment_locations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        llRecommended = view.findViewById(R.id.llRecommended);
        ivRecommendedFlag = view.findViewById(R.id.ivRecommendedFlag);
        tvRecommendedCountry = view.findViewById(R.id.tvRecommendedCountry);
        tvRecommendedCity = view.findViewById(R.id.tvRecommendedCity);
        tvRecommendedPing = view.findViewById(R.id.tvRecommendedPing);
        vRecommendedPingDot = view.findViewById(R.id.vRecommendedPingDot);
        btnConnectRecommended = view.findViewById(R.id.btnConnectRecommended);
        tvEmpty = view.findViewById(R.id.tvLocationsEmpty);

        adapter = new LocationAdapter(requireContext(), this);

        ListView lvLocations = view.findViewById(R.id.lvLocations);
        lvLocations.setAdapter(adapter);
        lvLocations.setEmptyView(tvEmpty);
        // Tapping the card (anywhere but Connect) lists or hides that country's cities.
        lvLocations.setOnItemClickListener((parent, itemView, position, id) -> {
            Country country = adapter.getItem(position);
            if (country != null) {
                adapter.toggleCities(country);
            }
        });

        btnConnectRecommended.setOnClickListener(v -> {
            if (recommendedCountry != null) {
                connect(recommendedCountry, recommendedCity);
            }
        });

        btnReload = view.findViewById(R.id.btnReload);
        btnReload.setOnClickListener(v -> loadLocations());

        btnPing = view.findViewById(R.id.btnPing);
        btnPing.setOnClickListener(v -> loadLatencies());

        CoachMarks.set(btnReload, R.string.coach_reload_locations);
        CoachMarks.set(btnPing, R.string.coach_ping_locations);
        CoachMarks.set(btnConnectRecommended, R.string.coach_connect_recommended);
        CoachMarks.setBottomNav(view);

        view.findViewById(R.id.btnSdkLogs).setOnClickListener(
                v -> HomeFragment.openLogs(this, vpnUsername, vpnPassword));
        CoachMarks.set(view.findViewById(R.id.btnSdkLogs), R.string.coach_open_logs);

        ((TextView) view.findViewById(R.id.etSearch)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applySearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setUpBottomNav(view);

        showRecommended(null, null);
        loadLocations();
        loadLatencies();
        loadRecommended();
    }

    /**
     * Dashboard pops back to {@link HomeFragment} and More swaps to that screen; Locations is this
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
                if (v.getId() == R.id.navMore) {
                    HomeFragment.showTab(this, MoreFragment.newInstance(vpnUsername, vpnPassword));
                    return;
                }
                selectTab(root, v.getId());
            });
        }
        selectTab(root, R.id.navLocations);
    }

    private void selectTab(@NonNull View root, int tabId) {
        for (int id : NAV_ITEM_IDS) {
            // The item is one TextView with a compound drawable, so this switches label and icon together.
            root.findViewById(id).setSelected(id == tabId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter = null;
        tvEmpty = null;
        llRecommended = null;
        ivRecommendedFlag = null;
        tvRecommendedCountry = null;
        tvRecommendedCity = null;
        tvRecommendedPing = null;
        vRecommendedPingDot = null;
        btnConnectRecommended = null;
        btnReload = null;
        btnPing = null;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // region Inventory

    /**
     * The reload button: countries and their cities only. Latencies are kept and re-applied to the
     * fresh list; the recommended card is left as it is.
     */
    private void loadLocations() {
        AtomManager atomManager = AtomDemoApplicationController.getInstance().getAtomManager();
        if (atomManager == null) {
            showEmptyMessage(R.string.atom_manager_not_initialized);
            return;
        }

        // Emptying the list first is what makes a reload visible: the loading message takes over
        // until the inventory comes back, instead of the stale rows sitting there unchanged.
        setReloading(true);
        allCountries.clear();
        citiesByCountry.clear();
        if (adapter != null) {
            adapter.submit(allCountries);
        }
        showEmptyMessage(R.string.loading_locations);

        atomManager.getCountries(new CollectionCallback<Country>() {
            @Override
            public void onSuccess(List<Country> countries) {
                mainHandler.post(() -> {
                    if (adapter == null) {
                        return;
                    }
                    allCountries.clear();
                    if (countries != null) {
                        allCountries.addAll(countries);
                    }
                    copyLatencies(allCountries);
                    copyCities(allCountries);
                    showEmptyMessage(R.string.no_locations_found);
                    applySearch();
                    setReloading(false);
                });
            }

            @Override
            public void onError(AtomException exception) {
                postLoadFailure();
            }

            @Override
            public void onNetworkError(AtomException exception) {
                postLoadFailure();
            }
        });

        // Best-effort: without it countries just have no cities to list.
        atomManager.getCities(new CollectionCallback<City>() {
            @Override
            public void onSuccess(List<City> cities) {
                mainHandler.post(() -> applyCities(cities));
            }

            @Override
            public void onError(AtomException exception) {
                // Countries stay without cities.
            }

            @Override
            public void onNetworkError(AtomException exception) {
                // Countries stay without cities.
            }
        });

    }

    /**
     * The ping button, and once on open. Latency is a separate, best-effort call: it needs pings
     * to get through, so the list is never held back waiting for it.
     */
    private void loadLatencies() {
        AtomManager atomManager = AtomDemoApplicationController.getInstance().getAtomManager();
        if (atomManager == null) {
            return;
        }
        setPinging(true);
        atomManager.getOptimizedCountries(new CollectionCallback<Country>() {
            @Override
            public void onSuccess(List<Country> countries) {
                mainHandler.post(() -> {
                    applyLatencies(countries);
                    setPinging(false);
                });
            }

            @Override
            public void onError(AtomException exception) {
                // Leaves the ping readings as they were.
                mainHandler.post(() -> setPinging(false));
            }

            @Override
            public void onNetworkError(AtomException exception) {
                mainHandler.post(() -> setPinging(false));
            }
        });
    }

    /** Only on open; neither button refreshes the recommended card. */
    private void loadRecommended() {
        AtomManager atomManager = AtomDemoApplicationController.getInstance().getAtomManager();
        if (atomManager == null) {
            return;
        }
        // getRecommendedLocation rather than getRecommendedCountry: it also names the city.
        atomManager.getRecommendedLocation(new Callback<Location>() {
            @Override
            public void onSuccess(Location location) {
                mainHandler.post(() -> showRecommended(
                        location == null ? null : location.getCountry(),
                        location == null ? null : location.getCity()));
            }

            @Override
            public void onError(AtomException exception) {
                mainHandler.post(() -> showRecommended(null, null));
            }

            @Override
            public void onNetworkError(AtomException exception) {
                mainHandler.post(() -> showRecommended(null, null));
            }
        });

    }

    private void postLoadFailure() {
        mainHandler.post(() -> {
            if (adapter == null) {
                return;
            }
            allCountries.clear();
            adapter.submit(allCountries);
            showEmptyMessage(R.string.locations_load_failed);
            setReloading(false);
        });
    }

    /** As {@link #setReloading}, for the ping button. */
    private void setPinging(boolean pinging) {
        if (btnPing == null) {
            return;
        }
        btnPing.setEnabled(!pinging);
        btnPing.setAlpha(pinging ? 0.5f : 1f);
    }

    /** Greys out the reload button while a load is in flight so taps cannot stack up. */
    private void setReloading(boolean reloading) {
        if (btnReload == null) {
            return;
        }
        btnReload.setEnabled(!reloading);
        btnReload.setAlpha(reloading ? 0.5f : 1f);
    }

    /** Records the measured latencies and copies them onto everything already on screen. */
    private void applyLatencies(@Nullable List<Country> optimizedCountries) {
        if (optimizedCountries == null || adapter == null) {
            return;
        }

        for (Country country : optimizedCountries) {
            latencies.put(LocationAdapter.keyOf(country), country.getLatency());
        }

        copyLatencies(allCountries);
        adapter.notifyDataSetChanged();

        if (recommendedCountry != null) {
            showRecommended(recommendedCountry, recommendedCity);
        }
    }

    private void copyLatencies(@NonNull List<Country> countries) {
        for (Country country : countries) {
            Integer latency = latencies.get(LocationAdapter.keyOf(country));
            if (latency != null) {
                country.setLatency(latency);
            }
        }
    }

    /** Groups the cities by country and attaches them to the countries already loaded. */
    private void applyCities(@Nullable List<City> cities) {
        if (cities == null || adapter == null) {
            return;
        }

        for (City city : cities) {
            if (city.getCountry() == null) {
                continue;
            }
            List<City> group = citiesByCountry.get(cityKey(city.getCountry()));
            if (group == null) {
                group = new ArrayList<>();
                citiesByCountry.put(cityKey(city.getCountry()), group);
            }
            group.add(city);
        }

        copyCities(allCountries);
        // A search may name a city, so the filtered list can change now that cities are known.
        applySearch();
    }

    private void copyCities(@NonNull List<Country> countries) {
        for (Country country : countries) {
            List<City> cities = citiesByCountry.get(cityKey(LocationAdapter.keyOf(country)));
            if (cities != null) {
                country.setCities(cities);
            }
        }
    }

    /** City.getCountry() is the country code; matched case-insensitively against keyOf. */
    @NonNull
    private static String cityKey(@NonNull String countryCode) {
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private void applySearch() {
        if (adapter == null) {
            return;
        }

        if (TextUtils.isEmpty(searchQuery)) {
            adapter.submit(allCountries);
            return;
        }

        List<Country> matches = new ArrayList<>();
        for (Country country : allCountries) {
            if (matchesQuery(country)) {
                matches.add(country);
            }
        }
        adapter.submit(matches);
    }

    private boolean matchesQuery(@NonNull Country country) {
        if (contains(country.getName()) || contains(LocationAdapter.keyOf(country))) {
            return true;
        }

        List<City> cities = country.getCities();
        if (cities != null) {
            for (City city : cities) {
                if (contains(city.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean contains(@Nullable String value) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(searchQuery);
    }

    private void showEmptyMessage(@StringRes int message) {
        if (tvEmpty != null) {
            tvEmpty.setText(message);
        }
    }

    private void showRecommended(@Nullable Country country, @Nullable City city) {
        if (llRecommended == null) {
            return;
        }

        recommendedCountry = country;
        recommendedCity = country == null ? null : city;

        if (country == null) {
            ivRecommendedFlag.setImageResource(R.drawable.bg_main_container);
            tvRecommendedCountry.setText(R.string.no_recommended_location);
            tvRecommendedCity.setVisibility(View.GONE);
            vRecommendedPingDot.setVisibility(View.GONE);
            tvRecommendedPing.setVisibility(View.GONE);
            btnConnectRecommended.setVisibility(View.GONE);
            return;
        }

        Integer latency = latencies.get(LocationAdapter.keyOf(country));
        if (latency != null) {
            country.setLatency(latency);
        }

        ivRecommendedFlag.setImageResource(LocationAdapter.flagOf(requireContext(), country));
        tvRecommendedCountry.setText(country.getName());
        tvRecommendedCity.setVisibility(View.VISIBLE);
        tvRecommendedCity.setText(city != null && city.getName() != null
                ? city.getName() : LocationAdapter.secondaryLabel(requireContext(), country));
        btnConnectRecommended.setVisibility(View.VISIBLE);
        LocationAdapter.bindPing(vRecommendedPingDot, tvRecommendedPing, country.getLatency());
    }

    // endregion

    // region Connecting

    /**
     * Opens the normal Connection With Params screen with this country selected and its
     * recommended protocol as the primary one. If the VPN is idle that screen dials it straight
     * away; if a tunnel is already up it only preselects, leaving Disconnect or Connection over
     * Connection to the user.
     */
    @Override
    public void onConnectClick(@NonNull Country country) {
        connect(country, null);
    }

    /** A city row from an expanded country: as {@link #onConnectClick}, with that city preselected. */
    @Override
    public void onCityConnectClick(@NonNull Country country, @NonNull City city) {
        connect(country, city);
    }

    /** As {@link #onConnectClick}, with a city preselected when one is given. */
    private void connect(@NonNull Country country, @Nullable City city) {
        if (AtomDemoApplicationController.getInstance().getAtomManager() == null) {
            Toast.makeText(getActivity(), R.string.atom_manager_not_initialized, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        Intent intent = new Intent(getActivity(), ConnectActivity.class);
        intent.putExtra("connection_type", HomeFragment.CONNECTION_METHOD_PARAMS);
        intent.putExtra(ARG_VPN_USERNAME, vpnUsername);
        intent.putExtra(ARG_VPN_PASSWORD, vpnPassword);
        intent.putExtra(ConnectWithParamsFragment.ARG_COUNTRY, country.getCountry());
        if (city != null) {
            intent.putExtra(ConnectWithParamsFragment.ARG_CITY, city.getName());
        }
        Protocol recommended = city != null && city.getRecommendedProtocol() != null
                ? city.getRecommendedProtocol() : country.getRecommendedProtocol();
        if (recommended != null) {
            intent.putExtra(ConnectWithParamsFragment.ARG_PROTOCOL, recommended.getProtocol());
        }
        intent.putExtra(ConnectWithParamsFragment.ARG_AUTO_CONNECT, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    // endregion
}
