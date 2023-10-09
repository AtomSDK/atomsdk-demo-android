/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.Fragment;

import com.atom.core.exceptions.AtomAPIException;
import com.atom.core.exceptions.AtomException;
import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.AtomConfiguration;
import com.atom.core.models.City;
import com.atom.core.models.Country;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.ConnectionDetails;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.VPNCredentials;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.sdk.android.common.Utils;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.adapter.CityAdapter;
import com.atom.vpn.demo.adapter.CountryAdapter;
import com.atom.vpn.demo.adapter.ProtocolAdapter;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.logger.Log;
import com.tooltip.Tooltip;


import java.util.ArrayList;
import java.util.List;

import static br.com.zbra.androidlinq.Linq.stream;
import static com.atom.vpn.demo.common.Utilities.changeButtonState;
import static com.atom.vpn.demo.common.Utilities.changeButtonText;


public class ConnectWithParamsFragment extends Fragment implements VPNStateListener {

    private static final String TAG = "ConnectWithParamsFragment";
    private AppCompatSpinner primaryProtocolSpinner, secondaryProtocolSpinner, tertiaryProtocolSpinner, countrySpinner,citySpinner;
    private List<Protocol> protocolList;
    private List<Country> countriesList;
    private List<City> citiesList;

    private List<Country> countriesForSmartDialing;

    private Switch switchOptimizedConnection,switchSmartDialing;
    private Button btnConnect;

    private String uuid,vpnUsername,vpnPassword;

    private Protocol primaryProtocol = null;
    private Protocol secondaryProtocol = null;
    private Protocol tertiaryProtocol = null;

    public ConnectWithParamsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras  = this.getArguments();
        if(extras!=null) {

            if (extras.containsKey("uuid")) {
                uuid =  extras.getString("uuid");
            }

            if (extras.containsKey("vpnUsername")) {
                vpnUsername =  extras.getString("vpnUsername");
            }

            if (extras.containsKey("vpnPassword")) {
                vpnPassword =  extras.getString("vpnPassword");
            }
        }

        AtomManager.addVPNStateListener(this);

        if(getActivity()!=null)
            new Handler().postDelayed(() -> AtomDemoApplicationController.getInstance().getAtomManager().bindIKEVStateService(getActivity()),500);


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connect_with_params, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView smartDialingHint =  view.findViewById(R.id.smartDialingHint);
        Tooltip.Builder smartDialingHintTipBuilder = new Tooltip.Builder(smartDialingHint, R.style.TooltipStyle);
        Tooltip smartCountriesHintTip = smartDialingHintTipBuilder.setText(Constants.TooltipSmartDialing).setDismissOnClick(true).build();
        smartDialingHint.setOnClickListener(v -> {

            if (!smartCountriesHintTip.isShowing()) {
                smartCountriesHintTip.show();
                final Handler handler = new Handler();
                handler.postDelayed(smartCountriesHintTip::dismiss, 3000);
            } else {
                smartCountriesHintTip.dismiss();
            }

        });


        ImageView optConnectionHint =  view.findViewById(R.id.optConnectionHint);
        Tooltip.Builder optConnectionHintTipBuilder = new Tooltip.Builder(optConnectionHint, R.style.TooltipStyle);
        Tooltip optConnectionHintTip = optConnectionHintTipBuilder.setText(Constants.TooltipOptimization).setDismissOnClick(true).build();

        optConnectionHint.setOnClickListener(v -> {

            if (!optConnectionHintTip.isShowing()) {
                optConnectionHintTip.show();
                final Handler handler = new Handler();
                handler.postDelayed(optConnectionHintTip::dismiss, 3000);
            } else {
                optConnectionHintTip.dismiss();
            }

        });

        primaryProtocolSpinner = view.findViewById(R.id.primaryProtocolSpinner);
        primaryProtocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                // Here you get the current item (a User object) that is selected by its position
                primaryProtocol = (Protocol) primaryProtocolSpinner.getAdapter().getItem(position);

                if (secondaryProtocolSpinner.getSelectedItemPosition() > 0) {
                    secondaryProtocol = (Protocol) secondaryProtocolSpinner.getAdapter().getItem(position);
                }else{
                    secondaryProtocol = null;
                }

                if (tertiaryProtocolSpinner.getSelectedItemPosition() > 0) {
                    tertiaryProtocol = (Protocol) tertiaryProtocolSpinner.getAdapter().getItem(position);
                }else{
                    tertiaryProtocol = null;
                }

                if(switchSmartDialing.isChecked()){
                    List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesForSmartDialing, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                    displayCountries(filteredCountry);
                }else {
                    List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                    displayCountries(filteredCountry);
                }


                if(countrySpinner.getSelectedItem()!=null){
                    Country country  = (Country)countrySpinner.getSelectedItem();
                    if(country!=null){
                        List<City> filteredCities = getCitiesByAllSelectedProtocolByCountry(citiesList, primaryProtocol, secondaryProtocol, tertiaryProtocol,country);
                        displayCities(filteredCities);

                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {

            }
        });

        secondaryProtocolSpinner =  view.findViewById(R.id.secondaryProtocolSpinner);
        secondaryProtocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                // Here you get the current item (a Protocol object) that is selected by its position
                if (position > 0) {
                    Protocol primaryProtocol = (Protocol) primaryProtocolSpinner.getSelectedItem();

                    if (secondaryProtocolSpinner.getSelectedItemPosition() > 0) {
                        secondaryProtocol = (Protocol) secondaryProtocolSpinner.getAdapter().getItem(position);
                    }else{
                        secondaryProtocol = null;
                    }

                    if (tertiaryProtocolSpinner.getSelectedItemPosition() > 0) {
                        tertiaryProtocol = (Protocol) tertiaryProtocolSpinner.getAdapter().getItem(position);
                    }else{
                        tertiaryProtocol = null;
                    }

                    if(switchSmartDialing.isChecked()){
                        List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesForSmartDialing, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayCountries(filteredCountry);
                    }else {
                        List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayCountries(filteredCountry);
                    }

                }else{
                    secondaryProtocol = null;

                    if(switchSmartDialing.isChecked()){
                        List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesForSmartDialing, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayCountries(filteredCountry);
                    }else {
                        List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayCountries(filteredCountry);
                    }
                }

                if(countrySpinner.getSelectedItem()!=null){
                    Country country  = (Country)countrySpinner.getSelectedItem();
                    if(country!=null){
                        List<City> filteredCities = getCitiesByAllSelectedProtocolByCountry(citiesList, primaryProtocol, secondaryProtocol, tertiaryProtocol,country);
                        displayCities(filteredCities);

                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {

            }
        });

        tertiaryProtocolSpinner = view.findViewById(R.id.tertiaryProtocolSpinner);
        tertiaryProtocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                if (position > 0) {

                    Protocol primaryProtocol = (Protocol) primaryProtocolSpinner.getSelectedItem();

                    if (secondaryProtocolSpinner.getSelectedItemPosition() > 0) {
                        secondaryProtocol = (Protocol) secondaryProtocolSpinner.getAdapter().getItem(position);
                    }else{
                        secondaryProtocol = null;
                    }

                    if (tertiaryProtocolSpinner.getSelectedItemPosition() > 0) {
                        tertiaryProtocol = (Protocol) tertiaryProtocolSpinner.getAdapter().getItem(position);
                    }else{
                        tertiaryProtocol = null;
                    }

                    if(switchSmartDialing.isChecked()){
                        List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesForSmartDialing, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayCountries(filteredCountry);
                    }else {
                        List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayCountries(filteredCountry);
                    }
                }else{
                    tertiaryProtocol = null;
                    if(switchSmartDialing.isChecked()){
                        List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesForSmartDialing, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayCountries(filteredCountry);
                    }else {
                        List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayCountries(filteredCountry);
                    }
                }

                if(countrySpinner.getSelectedItem()!=null){
                    Country country  = (Country)countrySpinner.getSelectedItem();
                    if(country!=null){
                        List<City> filteredCities = getCitiesByAllSelectedProtocolByCountry(citiesList, primaryProtocol, secondaryProtocol, tertiaryProtocol,country);
                        displayCities(filteredCities);

                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {

            }
        });

        countrySpinner =  view.findViewById(R.id.countrySpinner);

        citySpinner =  view.findViewById(R.id.citySpinner);

        switchOptimizedConnection =  view.findViewById(R.id.switchOptConnection);
        switchOptimizedConnection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                switchSmartDialing.setChecked(false);
                List<Country> countryList = getCountriesByAllSelectedProtocol(countriesList,primaryProtocol,secondaryProtocol,tertiaryProtocol);
                displayCountries(countryList);
            }

        });

        switchSmartDialing =  view.findViewById(R.id.switchSmartDialing);
        switchSmartDialing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                switchOptimizedConnection.setChecked(false);
                AtomDemoApplicationController.getInstance().getAtomManager().getCountriesForSmartDialing(new CollectionCallback<Country>() {
                    @Override
                    public void onSuccess(List<Country> list) {
                        if (list != null) {
                            countriesForSmartDialing = list;
                            List<Country> countryList = getCountriesByAllSelectedProtocol(countriesForSmartDialing,primaryProtocol,secondaryProtocol,tertiaryProtocol);
                            displayCountries(countryList);
                        }
                    }

                    @Override
                    public void onError(AtomException exception) {

                    }

                    @Override
                    public void onNetworkError(AtomException exception) {

                    }
                });

            }else{
                List<Country> countryList = getCountriesByAllSelectedProtocol(countriesList,primaryProtocol,secondaryProtocol,tertiaryProtocol);
                displayCountries(countryList);
            }

        });


        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(countrySpinner.getSelectedItem()!=null){
                    Country country  = (Country)countrySpinner.getSelectedItem();
                    if(country!=null){
                        List<City> filteredCities = getCitiesByAllSelectedProtocolByCountry(citiesList, primaryProtocol, secondaryProtocol, tertiaryProtocol,country);
                        displayCities(filteredCities);

                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btnConnect = view.findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(v -> {

            if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {

                if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
                    AtomDemoApplicationController.getInstance().getAtomManager().disconnect(getActivity());

                } else if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING)) {
                    AtomDemoApplicationController.getInstance().getAtomManager().cancel(getActivity());
                } else {

                    changeButtonText(getActivity(), btnConnect);

                    // Put username and password here
                    Protocol primaryProtocol = (Protocol) primaryProtocolSpinner.getSelectedItem();

                    Country country = (Country) countrySpinner.getSelectedItem();

                    City city = null;

                    if(citySpinner.getSelectedItemPosition()>0) {
                        city = (City) citySpinner.getSelectedItem();
                    }

                    if (primaryProtocol != null && (country!=null || city!=null)) {

                        btnConnect.setText("Cancel");

                        ConnectActivity connectActivity = (ConnectActivity)getActivity();
                        if(connectActivity!=null)
                        connectActivity.logWrapper.clear();

                        // Dedicated Host here
                        VPNProperties.Builder vpnPropertiesBuilder=null;
                        try {

                            if(city!=null){
                                vpnPropertiesBuilder = new VPNProperties.Builder(city, primaryProtocol);
                            }else if(country!=null) {
                                vpnPropertiesBuilder = new VPNProperties.Builder(country, primaryProtocol);
                            }

                            if (secondaryProtocolSpinner.getSelectedItemPosition() >= 1) {
                                Protocol secondaryProtocol = (Protocol) secondaryProtocolSpinner.getSelectedItem();
                                vpnPropertiesBuilder.withSecondaryProtocol(secondaryProtocol);
                            }

                            if (tertiaryProtocolSpinner.getSelectedItemPosition() >= 1) {
                                Protocol tertiaryProtocol = (Protocol) tertiaryProtocolSpinner.getSelectedItem();
                                vpnPropertiesBuilder.withTertiaryProtocol(tertiaryProtocol);
                            }


                            if (switchOptimizedConnection.isChecked()) {
                                vpnPropertiesBuilder.withOptimization();
                            }

                            if (switchSmartDialing.isChecked()) {
                                vpnPropertiesBuilder.withSmartDialing();
                            }

                            if(!TextUtils.isEmpty(vpnUsername) && !TextUtils.isEmpty(vpnPassword)) {
                                AtomDemoApplicationController.getInstance().getAtomManager().setVPNCredentials(new VPNCredentials(vpnUsername,vpnPassword));
                            }else if(!TextUtils.isEmpty(uuid)){
                                AtomDemoApplicationController.getInstance().getAtomManager().setUUID(uuid);
                            }

                            AtomDemoApplicationController.getInstance().getAtomManager().connect(getActivity(), vpnPropertiesBuilder.build());

                        } catch (AtomValidationException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        });

        // get Protocols from ATOM SDK
        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            AtomDemoApplicationController.getInstance().getAtomManager().getProtocols(new CollectionCallback<Protocol>() {

                @Override
                public void onSuccess(List<Protocol> protocols) {
                    protocolList = protocols;

                    if (protocolList != null) {

                        Protocol[] protocolArray = protocolList.toArray(new Protocol[0]);

                        if(getActivity()!=null) {
                            ProtocolAdapter protocolAdapter = new ProtocolAdapter(getActivity(), android.R.layout.simple_spinner_item,
                                    protocolArray);
                            protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            primaryProtocolSpinner.setAdapter(protocolAdapter);


                            // Preparing Secondary Protocol
                            ArrayList<Protocol> secondaryProtocol = new ArrayList<>();
                            Protocol dummySecondaryProtocol = new Protocol();
                            dummySecondaryProtocol.setName("Secondary Protocol");
                            secondaryProtocol.add(dummySecondaryProtocol);
                            secondaryProtocol.addAll(protocolList);

                            Protocol[] secondaryProtocolArray = secondaryProtocol.toArray(new Protocol[secondaryProtocol.size()]);
                            ProtocolAdapter secondaryProtocolAdapter = new ProtocolAdapter(getActivity(), android.R.layout.simple_spinner_item,
                                    secondaryProtocolArray);
                            secondaryProtocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            secondaryProtocolSpinner.setAdapter(secondaryProtocolAdapter);


                            // Preparing Secondary Protocol
                            ArrayList<Protocol> tertiaryProtocol = new ArrayList<>();
                            Protocol dummyTertiaryProtocol = new Protocol();
                            dummyTertiaryProtocol.setName("Tertiary Protocol");
                            tertiaryProtocol.add(dummyTertiaryProtocol);
                            tertiaryProtocol.addAll(protocolList);

                            Protocol[] tertiaryProtocolArray = tertiaryProtocol.toArray(new Protocol[tertiaryProtocol.size()]);
                            ProtocolAdapter tertiaryProtocolAdapter = new ProtocolAdapter(getActivity(), android.R.layout.simple_spinner_item,
                                    tertiaryProtocolArray);
                            tertiaryProtocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            tertiaryProtocolSpinner.setAdapter(tertiaryProtocolAdapter);
                        }

                    }
                }


                @Override
                public void onError(AtomException atomException) {
                    Log.e(TAG, atomException.getMessage() + " : " + atomException.getCode());



                }

                @Override
                public void onNetworkError(AtomException atomException) {
                    Log.e(TAG, atomException.getMessage() + " : " + atomException.getCode());

                }
            });
        }

        // get Countries from ATOM SDK
        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            AtomDemoApplicationController.getInstance().getAtomManager().getCountries(new CollectionCallback<Country>() {
                @Override
                public void onSuccess(List<Country> countries) {

                    if(countries!=null) {
                        countriesList = countries;

                        List<Country> countryList = getCountriesByAllSelectedProtocol(countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayCountries(countryList);

                    }
                }

                @Override
                public void onError(AtomException atomException) {
                    Log.e(TAG, atomException.getMessage() + " : " + atomException.getCode());

                }

                @Override
                public void onNetworkError(AtomException atomException) {
                    Log.e(TAG, atomException.getMessage() + " : " + atomException.getCode());

                }

            });

        }

        // get Cities from ATOM SDK
        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            AtomDemoApplicationController.getInstance().getAtomManager().getCities(new CollectionCallback<City>() {
                @Override
                public void onSuccess(List<City> cities) {

                    if(cities!=null) {
                        citiesList = cities;
                    }
                }

                @Override
                public void onError(AtomException atomException) {
                    Log.d(TAG, atomException.getMessage() + " : " + atomException.getCode());

                }

                @Override
                public void onNetworkError(AtomException atomException) {
                    Log.d(TAG, atomException.getMessage() + " : " + atomException.getCode());

                }

            });

        }


        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
                changeButtonState(btnConnect, "Disconnect");
            }
        }

    }

    private List<Country> getCountriesByAllSelectedProtocol(List<Country> countries, Protocol primaryProtocol, Protocol secondaryProtocol, Protocol tertiaryProtocol) {

        if (countries != null) {

                if (primaryProtocol != null && secondaryProtocol != null && tertiaryProtocol != null) {

                    List<Protocol> protocolList = new ArrayList<>();
                    protocolList.add(primaryProtocol);
                    protocolList.add(secondaryProtocol);
                    protocolList.add(tertiaryProtocol);
                    return stream(countries)
                                    .where(c ->c.getProtocols()!=null && c.getProtocols().containsAll(protocolList)).toList();

                } else if (primaryProtocol != null && secondaryProtocol != null) {


                    List<Protocol> protocolList = new ArrayList<>();
                    protocolList.add(primaryProtocol);
                    protocolList.add(secondaryProtocol);
                    return stream(countries)
                            .where(c -> c.getProtocols()!=null && c.getProtocols().containsAll(protocolList)).toList();

                }else if (primaryProtocol != null && tertiaryProtocol != null) {


                    List<Protocol> protocolList = new ArrayList<>();
                    protocolList.add(primaryProtocol);
                    protocolList.add(tertiaryProtocol);
                    return stream(countries)
                            .where(c -> c.getProtocols()!=null && c.getProtocols().containsAll(protocolList)).toList();

                } else if (primaryProtocol != null) {

                    return stream(countries)
                            .where(c ->c.getProtocols()!=null && c.getProtocols().contains(primaryProtocol)).toList();

                }
        }

        return null;
    }


    private List<City> getCitiesByAllSelectedProtocolByCountry(List<City> cities, Protocol primaryProtocol, Protocol secondaryProtocol, Protocol tertiaryProtocol,Country country) {

        if (cities != null) {

            if (primaryProtocol != null && secondaryProtocol != null && tertiaryProtocol != null) {

                List<Protocol> protocolList = new ArrayList<>();
                protocolList.add(primaryProtocol);
                protocolList.add(secondaryProtocol);
                protocolList.add(tertiaryProtocol);
                return stream(cities)
                        .where(c -> c.getProtocols() != null && c.getProtocols().containsAll(protocolList) && c.getCountry().equalsIgnoreCase(country.getCountry())).toList();

            } else if (primaryProtocol != null && secondaryProtocol != null) {


                List<Protocol> protocolList = new ArrayList<>();
                protocolList.add(primaryProtocol);
                protocolList.add(secondaryProtocol);
                return stream(cities)
                        .where(c -> c.getProtocols() != null && c.getProtocols().containsAll(protocolList) && c.getCountry().equalsIgnoreCase(country.getCountry())).toList();

            } else if (primaryProtocol != null && tertiaryProtocol != null) {


                List<Protocol> protocolList = new ArrayList<>();
                protocolList.add(primaryProtocol);
                protocolList.add(tertiaryProtocol);
                return stream(cities)
                        .where(c -> c.getProtocols() != null && c.getProtocols().containsAll(protocolList)  && c.getCountry().equalsIgnoreCase(country.getCountry())).toList();

            } else if (primaryProtocol != null) {

                return stream(cities)
                        .where(c -> c.getProtocols() != null && c.getProtocols().contains(primaryProtocol)  && c.getCountry().equalsIgnoreCase(country.getCountry())).toList();

            }
        }

        return null;
    }


    private void displayCountries(List<Country> countries) {
        if (countries != null) {
            Country[] countryArray = countries.toArray(new Country[0]);
            if(getActivity()!=null) {
                CountryAdapter countryAdapter = new CountryAdapter(getActivity(), android.R.layout.simple_spinner_item,
                        countryArray);
                countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                countrySpinner.setAdapter(countryAdapter);
                countryAdapter.notifyDataSetChanged();
            }
        }
    }


    private void displayCities(List<City> cities) {
        if (cities != null) {
            City dummyCity = new City();
            dummyCity.setName("Select City");
            dummyCity.setId(0);
            cities.add(0,dummyCity);
            City[] cityArray = cities.toArray(new City[0]);
            if (getActivity() != null) {
                CityAdapter cityAdapter = new CityAdapter(getActivity(), android.R.layout.simple_spinner_item,
                        cityArray);
                cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                citySpinner.setAdapter(cityAdapter);
                cityAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onConnected(ConnectionDetails connectionDetails) {
        Log.d(TAG,   "Connected");
        changeButtonState(btnConnect, "Disconnect");


        Utils.INSTANCE.objectToString(connectionDetails, result -> {
            Log.e("connection", result);
           return null;
        });
    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onConnecting(VPNProperties vpnProperties, AtomConfiguration atomConfiguration) {

    }


    @Override
    public void onRedialing(AtomException atomException, ConnectionDetails connectionDetails) {
        Log.d(TAG, atomException.getMessage());
    }

    @Override
    public void onDialError(AtomException atomException, ConnectionDetails connectionDetails) {

        if(!AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
            Log.d(TAG, atomException.getMessage());
            if (atomException.getCode() != Errors._5039)
                changeButtonState(btnConnect, "Connect");
        }


        Log.d(TAG, atomException.getCode() + "");
        Log.d(TAG, atomException.getMessage() + "");

        if (atomException.getException() instanceof AtomAPIException) {
            AtomAPIException atomAPIException = (AtomAPIException) atomException.getException();
            Log.d(TAG, atomAPIException.getErrorMessage()  + " - " +atomAPIException.getCode());
        }
    }


    @Override
    public void onStateChange(String state) {

        Log.e(TAG,  state );
        if (state.equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING) || state.equalsIgnoreCase(VPNState.RECONNECTING)) {
            changeButtonState(btnConnect, "Cancel");
        }
    }

    @Override
    public void onDisconnecting(ConnectionDetails connectionDetails) {

    }


    @Override
    public void onDisconnected(boolean isCancelled) {

    }

    @Override
    public void onDisconnected(ConnectionDetails connectionDetails) {
        if (connectionDetails.isCancelled()) {
            Log.d(TAG,   "Cancelled");
        } else {
            Log.d(TAG,   "Disconnected");
        }

        changeButtonState(btnConnect, "Connect");
    }

    @Override
    public void onUnableToAccessInternet(AtomException atomException, ConnectionDetails connectionDetails) {

    }

    @Override
    public void onPacketsTransmitted(String s, String s1, String s2, String s3) {

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        AtomManager.removeVPNStateListener(this);
        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {

            AtomDemoApplicationController.getInstance().getAtomManager().unBindIKEVStateService(getActivity());
        }
    }

}
