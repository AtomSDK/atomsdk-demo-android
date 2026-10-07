/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.atom.core.models.City;
import com.atom.core.models.Country;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.CoachMarks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Backs the available-servers list on the Locations screen. Rows are {@code item_location.xml},
 * the same card the recommended location uses - which is why the ping helpers below are static:
 * the fragment reuses them to fill its recommended card. Tapping a country's card lists its cities
 * underneath as {@code item_city_location.xml} rows, each with its own Connect.
 */
public class LocationAdapter extends ArrayAdapter<Country> {

    public interface OnConnectClickListener {
        void onConnectClick(@NonNull Country country);

        void onCityConnectClick(@NonNull Country country, @NonNull City city);
    }

    /** Latency cut-offs for the three dot shades; anything slower gets the lightest green. */
    private static final int PING_GOOD_MS = 80;
    private static final int PING_FAIR_MS = 160;

    /** Space above each city card, between it and the card before. */
    private static final int CITY_ROW_GAP_DP = 8;

    private final LayoutInflater inflater;
    private final OnConnectClickListener connectClickListener;

    /** {@link #keyOf} of the country the VPN is busy with, or null when idle. */
    @Nullable
    private String activeCountryKey;

    /** {@link #keyOf} of every country whose cities are listed; survives reloads and searches. */
    private final Set<String> expandedCountryKeys = new HashSet<>();

    /** Label for that one row - "Disconnect" while connected, "Cancel" while dialling. */
    @StringRes
    private int activeActionLabel = R.string.connect;

    public LocationAdapter(@NonNull Context context,
                           @NonNull OnConnectClickListener connectClickListener) {
        super(context, R.layout.item_location, new ArrayList<>());
        this.inflater = LayoutInflater.from(context);
        this.connectClickListener = connectClickListener;
    }

    /** Identifies a country across list reloads; ISO code where the API gives one. */
    @NonNull
    public static String keyOf(@NonNull Country country) {
        String iso = country.getCountry();
        if (iso == null || iso.trim().isEmpty()) {
            iso = country.getIsoCode();
        }
        return iso == null || iso.trim().isEmpty() ? String.valueOf(country.getName()) : iso;
    }

    public void submit(@NonNull List<Country> countries) {
        setNotifyOnChange(false);
        clear();
        addAll(countries);
        notifyDataSetChanged();
    }

    /**
     * Marks the row the VPN is currently working on. Pass a null key to put every row back to
     * plain "Connect".
     */
    public void setActiveCountry(@Nullable String countryKey, @StringRes int actionLabel) {
        this.activeCountryKey = countryKey;
        this.activeActionLabel = countryKey == null ? R.string.connect : actionLabel;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row = convertView != null
                ? convertView
                : inflater.inflate(R.layout.item_location, parent, false);

        Country country = getItem(position);
        if (country == null) {
            return row;
        }

        ((ImageView) row.findViewById(R.id.ivFlag)).setImageResource(flagOf(getContext(), country));
        ((TextView) row.findViewById(R.id.tvCountry)).setText(country.getName());
        ((TextView) row.findViewById(R.id.tvCity)).setText(secondaryLabel(getContext(), country));

        bindPing(row.findViewById(R.id.vPingDot), row.findViewById(R.id.tvPing),
                country.getLatency());

        TextView btnConnect = row.findViewById(R.id.btnConnect);
        boolean active = keyOf(country).equals(activeCountryKey);
        btnConnect.setText(active ? activeActionLabel : R.string.connect);
        btnConnect.setOnClickListener(v -> connectClickListener.onConnectClick(country));
        CoachMarks.set(btnConnect, R.string.coach_connect_location);
        // The rest of the card (the ListView's item click) lists the cities instead.
        CoachMarks.set(row, R.string.coach_show_cities);

        bindCities(row, country);

        return row;
    }

    /** Shows or hides the country's cities; called for a tap on its card. */
    public void toggleCities(@NonNull Country country) {
        String key = keyOf(country);
        if (!expandedCountryKeys.remove(key)) {
            expandedCountryKeys.add(key);
        }
        notifyDataSetChanged();
    }

    /** When expanded, one item_city_location row per city. */
    private void bindCities(@NonNull View row, @NonNull Country country) {
        LinearLayout llCities = row.findViewById(R.id.llCities);
        // Recycled rows may still hold another country's cities.
        llCities.removeAllViews();

        List<City> cities = country.getCities();
        boolean hasCities = cities != null && !cities.isEmpty();
        boolean expanded = hasCities && expandedCountryKeys.contains(keyOf(country));
        llCities.setVisibility(expanded ? View.VISIBLE : View.GONE);

        // The chevron points down to expand, up to collapse; no cities, nothing to expand.
        row.findViewById(R.id.expandStrip).setVisibility(hasCities ? View.VISIBLE : View.GONE);
        row.findViewById(R.id.ivExpand).setRotation(expanded ? -90 : 90);
        if (!expanded) {
            return;
        }

        int gap = Math.round(CITY_ROW_GAP_DP * getContext().getResources().getDisplayMetrics().density);
        for (City city : cities) {
            View cityRow = inflater.inflate(R.layout.item_city_location, llCities, false);
            ((LinearLayout.LayoutParams) cityRow.getLayoutParams()).topMargin = gap;

            ((TextView) cityRow.findViewById(R.id.tvCountry)).setText(city.getName());
            ((TextView) cityRow.findViewById(R.id.tvCity)).setText(country.getName());
            bindPing(cityRow.findViewById(R.id.vPingDot), cityRow.findViewById(R.id.tvPing),
                    city.getLatency());

            TextView btnConnect = cityRow.findViewById(R.id.btnConnect);
            btnConnect.setOnClickListener(v -> connectClickListener.onCityConnectClick(country, city));
            // Clickable, so a tap on the city card stays with the city instead of reaching the
            // ListView's item click, which would collapse the country.
            cityRow.setOnClickListener(v -> connectClickListener.onCityConnectClick(country, city));
            CoachMarks.set(btnConnect, R.string.coach_connect_location);
            CoachMarks.set(cityRow, R.string.coach_connect_location);

            llCities.addView(cityRow);
        }
    }

    /**
     * Bundled flag for the country, {@code ic_rounded_<iso>}; the plain placeholder circle when
     * the app ships none for that code. getIdentifier answers 0, not an exception, for a miss.
     */
    @DrawableRes
    public static int flagOf(@NonNull Context context, @NonNull Country country) {
        @SuppressWarnings("DiscouragedApi")
        int resId = context.getResources().getIdentifier(
                "ic_rounded_" + keyOf(country).trim().toLowerCase(Locale.ROOT),
                "drawable", context.getPackageName());
        return resId != 0 ? resId : R.drawable.bg_main_container;
    }

    /**
     * Second line of a card: how many cities the country exposes, falling back to its ISO code
     * when the inventory carries none.
     */
    @NonNull
    public static String secondaryLabel(@NonNull Context context, @NonNull Country country) {
        List<?> cities = country.getCities();
        if (cities != null && !cities.isEmpty()) {
            return context.getResources()
                    .getQuantityString(R.plurals.city_count, cities.size(), cities.size());
        }
        return keyOf(country);
    }

    /**
     * Fills the ping dot and reading. Latency is only known once getOptimizedCountries has
     * reported, and stays 0 when pings are blocked - both views are hidden until then.
     */
    public static void bindPing(@NonNull View dot, @NonNull TextView reading, int latencyMs) {
        if (latencyMs <= 0) {
            dot.setVisibility(View.GONE);
            reading.setVisibility(View.GONE);
            return;
        }

        dot.setVisibility(View.VISIBLE);
        reading.setVisibility(View.VISIBLE);
        dot.setBackgroundResource(pingDrawable(latencyMs));
        reading.setText(reading.getContext().getString(R.string.ping_ms, latencyMs));
    }

    private static int pingDrawable(int latencyMs) {
        if (latencyMs <= PING_GOOD_MS) {
            return R.drawable.bg_ping_high;
        }
        return latencyMs <= PING_FAIR_MS ? R.drawable.bg_ping_mid : R.drawable.bg_ping_low;
    }
}
