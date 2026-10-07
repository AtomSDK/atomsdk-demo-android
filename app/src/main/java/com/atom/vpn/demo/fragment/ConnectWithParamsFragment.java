/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atom.core.exceptions.AtomException;
import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.City;
import com.atom.core.models.Country;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.common.DemoSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Connect by location: pick up to three protocols and a country (optionally a city). The country
 * list only offers countries that support every selected protocol, and the city list only offers
 * cities of the selected country that do too.
 */
public class ConnectWithParamsFragment extends BaseConnectFragment {

    /**
     * Optional preselection, used by the Locations screen: the country code
     * ({@link Country#getCountry()}), the protocol slug ({@link Protocol#getProtocol()}) to use as
     * primary, and whether to dial straight away when the VPN is idle.
     */
    public static final String ARG_COUNTRY = "preselect_country";
    /** Optional city name within {@link #ARG_COUNTRY}, e.g. from the recommended location. */
    public static final String ARG_CITY = "preselect_city";
    public static final String ARG_PROTOCOL = "preselect_protocol";
    public static final String ARG_AUTO_CONNECT = "auto_connect";

    /** Pending preselection; cleared once applied so a recreated screen does not redo it. */
    @Nullable private String pendingCountry;
    @Nullable private String pendingCity;
    @Nullable private String pendingProtocol;
    private boolean pendingAutoConnect;

    // Inventory from the SDK; null until the matching call returns.
    @Nullable private List<Country> countries;
    @Nullable private List<City> cities;

    @Nullable private Country selectedCountry;
    @Nullable private City selectedCity;

    private View btnSelectCountry, btnSelectCity;
    private TextView tvSelectedCountry, tvSelectedCity;

    public ConnectWithParamsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            pendingCountry = args.getString(ARG_COUNTRY);
            pendingCity = args.getString(ARG_CITY);
            pendingProtocol = args.getString(ARG_PROTOCOL);
            pendingAutoConnect = args.getBoolean(ARG_AUTO_CONNECT, false);
            // Consumed: the arguments are restored on recreation, the selection is not re-applied.
            args.remove(ARG_COUNTRY);
            args.remove(ARG_CITY);
            args.remove(ARG_PROTOCOL);
            args.remove(ARG_AUTO_CONNECT);
        }
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_connect_with_params;
    }

    @Override
    protected void onBindTarget(@NonNull View root) {
        btnSelectCountry = root.findViewById(R.id.btnSelectCountry);
        btnSelectCity = root.findViewById(R.id.btnSelectCity);
        tvSelectedCountry = root.findViewById(R.id.tvSelectedCountry);
        tvSelectedCity = root.findViewById(R.id.tvSelectedCity);

        btnSelectCountry.setOnClickListener(v -> pickCountry());
        btnSelectCity.setOnClickListener(v -> pickCity());

        CoachMarks.set(btnSelectCountry, R.string.coach_select_country);
        CoachMarks.set(btnSelectCity, R.string.coach_select_city);

        renderSelection();
    }

    @Override
    protected void onUnbindTarget() {
        btnSelectCountry = null;
        btnSelectCity = null;
        tvSelectedCountry = null;
        tvSelectedCity = null;
    }

    @NonNull
    @Override
    protected View[] targetSelectors() {
        return new View[]{btnSelectCountry, btnSelectCity};
    }

    @NonNull
    @Override
    protected DemoSettings.Dialing dialing() {
        return DemoSettings.Dialing.PARAMS;
    }

    @Override
    protected boolean supportsRecommendedProtocol() {
        return true;
    }

    @Override
    protected void onLoadInventory(@NonNull AtomManager atomManager) {
        atomManager.getCountries(new CollectionCallback<Country>() {
            @Override
            public void onSuccess(List<Country> list) {
                mainHandler.post(() -> {
                    countries = list;
                    applyPreselection();
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

        atomManager.getCities(new CollectionCallback<City>() {
            @Override
            public void onSuccess(List<City> list) {
                mainHandler.post(() -> cities = list);
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

    /** A protocol change can leave the chosen country or city unable to dial it; drop those. */
    @Override
    protected void onProtocolsChanged() {
        if (selectedCountry != null && !supportsSelectedProtocols(selectedCountry.getProtocols())) {
            selectedCountry = null;
            selectedCity = null;
        }
        if (selectedCity != null && !supportsSelectedProtocols(selectedCity.getProtocols())) {
            selectedCity = null;
        }
        renderSelection();
        applyPreselection();
    }

    /**
     * Without protocols or countries the Locations screen's choice can never be applied; say so
     * rather than leaving the user on an empty screen wondering why nothing dialled.
     */
    @Override
    protected void onInventoryFailed() {
        if (pendingCountry == null || !isAdded()) {
            return;
        }
        pendingCountry = null;
        pendingProtocol = null;
        pendingAutoConnect = false;
        toast(R.string.preselection_inventory_failed);
    }

    /**
     * Applies the Locations screen's choice once both protocols and countries are in, then
     * behaves exactly as if the user had picked them and tapped Connect.
     */
    private void applyPreselection() {
        if (pendingCountry == null || protocols == null || countries == null
                || tvSelectedCountry == null) {
            return;
        }
        String countryCode = pendingCountry;
        String cityName = pendingCity;
        pendingCountry = null;
        pendingCity = null;

        if (pendingProtocol != null) {
            boolean found = false;
            for (Protocol protocol : protocols) {
                if (pendingProtocol.equalsIgnoreCase(protocol.getProtocol())) {
                    primaryProtocol = protocol;
                    useRecommendedProtocol = false;
                    found = true;
                    break;
                }
            }
            if (!found && primaryProtocol != null) {
                // Keeps the default protocol, but tells the user it is not the recommended one.
                Toast.makeText(getActivity(), getString(R.string.preselection_protocol_unavailable,
                        pendingProtocol, primaryProtocol.getName()), Toast.LENGTH_LONG).show();
            }
            pendingProtocol = null;
            renderProtocols();
        }

        Country match = null;
        for (Country country : countries) {
            if (countryCode.equalsIgnoreCase(country.getCountry())) {
                match = country;
                break;
            }
        }
        selectedCountry = match != null && supportsSelectedProtocols(match.getProtocols()) ? match : null;
        selectedCity = null;
        if (selectedCountry != null && cityName != null) {
            // Unknown or unsupported city: dial the country, as "Any city" would.
            for (City city : availableCities(selectedCountry)) {
                if (cityName.equalsIgnoreCase(city.getName())) {
                    selectedCity = city;
                    break;
                }
            }
        }
        renderSelection();

        if (selectedCountry == null) {
            pendingAutoConnect = false;
            toast(match == null ? R.string.preselection_country_unavailable
                    : R.string.no_countries_for_protocols);
            return;
        }
        // Over a live tunnel this would be a Connection over Connection; leave that to the user.
        if (pendingAutoConnect && isVpnIdle()) {
            connectWithSelection();
        }
        pendingAutoConnect = false;
    }

    @Nullable
    @Override
    protected VPNProperties.Builder newPropertiesBuilder() throws AtomValidationException {
        if (selectedCountry == null) {
            toast(R.string.select_protocol_and_country);
            return null;
        }
        Protocol primary = selectedCity != null
                ? primaryFor(selectedCity.getRecommendedProtocol(), selectedCity.getProtocols())
                : primaryFor(selectedCountry.getRecommendedProtocol(), selectedCountry.getProtocols());
        if (primary == null) {
            toast(R.string.select_protocol_and_country);
            return null;
        }

        VPNProperties.Builder builder = selectedCity != null
                ? new VPNProperties.Builder(selectedCity, primary)
                : new VPNProperties.Builder(selectedCountry, primary);

        if (secondaryProtocol != null) {
            builder.withSecondaryProtocol(secondaryProtocol);
        }
        if (tertiaryProtocol != null) {
            builder.withTertiaryProtocol(tertiaryProtocol);
        }

        // Port (multiport) and the server filter come from
        // DemoSettings, applied by BaseConnectFragment.dial().
        return builder;
    }

    @NonNull
    @Override
    protected String targetDescription() {
        if (selectedCountry == null) {
            return getString(R.string.not_available);
        }
        return selectedCity != null
                ? selectedCity.getName() + ", " + selectedCountry.getName()
                : selectedCountry.getName();
    }

    // region Pickers

    @NonNull
    private List<Country> availableCountries() {
        List<Country> available = new ArrayList<>();
        if (countries != null) {
            for (Country country : countries) {
                if (supportsSelectedProtocols(country.getProtocols())) {
                    available.add(country);
                }
            }
        }
        return available;
    }

    @NonNull
    private List<City> availableCities(@NonNull Country country) {
        List<City> available = new ArrayList<>();
        if (cities != null) {
            for (City city : cities) {
                if (country.getCountry().equalsIgnoreCase(city.getCountry())
                        && supportsSelectedProtocols(city.getProtocols())) {
                    available.add(city);
                }
            }
        }
        return available;
    }

    private void pickCountry() {
        if (countries == null) {
            toast(R.string.loading);
            return;
        }
        List<Country> available = availableCountries();
        if (available.isEmpty()) {
            toast(R.string.no_countries_for_protocols);
            return;
        }
        String[] names = new String[available.size()];
        for (int i = 0; i < available.size(); i++) {
            names[i] = available.get(i).getName();
        }
        showPicker(R.string.lbl_select_country, names, which -> {
            Country country = available.get(which);
            if (selectedCountry == null
                    || !selectedCountry.getCountry().equalsIgnoreCase(country.getCountry())) {
                selectedCity = null;
            }
            selectedCountry = country;
            renderSelection();
        });
    }

    /** The city is optional: "Any city" dials the country itself. */
    private void pickCity() {
        if (selectedCountry == null) {
            toast(R.string.select_country_first);
            return;
        }
        if (cities == null) {
            toast(R.string.loading);
            return;
        }
        List<City> available = availableCities(selectedCountry);
        String[] names = new String[available.size() + 1];
        names[0] = getString(R.string.any_city);
        for (int i = 0; i < available.size(); i++) {
            names[i + 1] = available.get(i).getName();
        }
        showPicker(R.string.lbl_select_city, names, which -> {
            selectedCity = which == 0 ? null : available.get(which - 1);
            renderSelection();
        });
    }

    private void renderSelection() {
        if (tvSelectedCountry == null) {
            return;
        }
        tvSelectedCountry.setText(selectedCountry == null
                ? getString(R.string.lbl_select_country) : selectedCountry.getName());
        tvSelectedCity.setText(selectedCity == null
                ? getString(R.string.any_city) : selectedCity.getName());
    }

    // endregion
}
