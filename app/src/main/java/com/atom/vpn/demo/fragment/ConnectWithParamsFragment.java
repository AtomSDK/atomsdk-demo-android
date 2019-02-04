/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.ConnectionDetails;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.VPNCredentials;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.sdk.android.common.Common;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.sdk.android.data.model.countries.Country;
import com.atom.sdk.android.data.model.protocol.Protocol;
import com.atom.sdk.android.exceptions.AtomAPIException;
import com.atom.sdk.android.exceptions.AtomException;
import com.atom.sdk.android.exceptions.AtomValidationException;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.adapter.CountryAdapter;
import com.atom.vpn.demo.adapter.ProtocolAdapter;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.logger.Log;
import com.tooltip.Tooltip;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;



import static br.com.zbra.androidlinq.Linq.stream;
import static com.atom.vpn.demo.common.Utilities.changeButtonState;
import static com.atom.vpn.demo.common.Utilities.changeButtonText;


public class ConnectWithParamsFragment extends Fragment implements VPNStateListener {

    private static final String TAG = "ConnectWithParamsFragment";
    private AppCompatSpinner primaryProtocolSpinner, secondaryProtocolSpinner, tertiaryProtocolSpinner, countrySpinner;
    private List<Protocol> protocolList;
    private List<Country> countriesList;
    private List<Country> countriesForSmartDialing;
    private List<Country> countriesOptimizedList;

    private Switch switchOptimizedConnection,switchSmartDialing;
    private Button btnConnect;

    private String uuid,vpnUsername,vpnPassword;

    private Protocol primaryProtocol = null;
    private Protocol secondaryProtocol = null;
    private Protocol tertiaryProtocol = null;

    VPNProperties vpnProperties = null;
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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AtomDemoApplicationController.getInstance().getAtomManager().bindIKEVStateService(getActivity());
            }
        },500);

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
        smartDialingHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!smartCountriesHintTip.isShowing()) {
                    smartCountriesHintTip.show();
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            smartCountriesHintTip.dismiss();
                        }
                    }, 3000);
                } else {
                    smartCountriesHintTip.dismiss();
                }

            }
        });


        ImageView optConnectionHint =  view.findViewById(R.id.optConnectionHint);
        Tooltip.Builder optConnectionHintTipBuilder = new Tooltip.Builder(optConnectionHint, R.style.TooltipStyle);
        Tooltip optConnectionHintTip = optConnectionHintTipBuilder.setText(Constants.TooltipOptimization).setDismissOnClick(true).build();

        optConnectionHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!optConnectionHintTip.isShowing()) {
                    optConnectionHintTip.show();
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            optConnectionHintTip.dismiss();
                        }
                    }, 3000);
                } else {
                    optConnectionHintTip.dismiss();
                }

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

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {

            }
        });

        countrySpinner =  view.findViewById(R.id.countrySpinner);

        switchOptimizedConnection =  view.findViewById(R.id.switchOptConnection);
        switchOptimizedConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    switchSmartDialing.setChecked(false);
                    List<Country> countryList = getCountriesByAllSelectedProtocol(countriesList,primaryProtocol,secondaryProtocol,tertiaryProtocol);
                    displayCountries(countryList);
                }

            }
        });

        switchSmartDialing =  view.findViewById(R.id.switchSmartDialing);
        switchSmartDialing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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

                    if (primaryProtocol != null && country!=null) {

                        btnConnect.setText("Cancel");

                        ConnectActivity connectActivity = (ConnectActivity)getActivity();
                        if(connectActivity!=null)
                        connectActivity.logWrapper.clear();

                        // Dedicated Host here
                        VPNProperties.Builder vpnPropertiesBuilder;
                        try {

                            vpnPropertiesBuilder = new VPNProperties.Builder(country, primaryProtocol);

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
                                //vpnPropertiesBuilder.withAutomaticPort();
                            }

                            if (switchSmartDialing.isChecked()) {
                                //vpnPropertiesBuilder.withManualPort(5600);
                                vpnPropertiesBuilder.withSmartDialing();
                            }

//                            vpnPropertiesBuilder.withManualPort(80);
//                            String[] applications = {"com.android.chrome"};
//                            vpnPropertiesBuilder.withSplitTunneling(applications);

                            if(!TextUtils.isEmpty(vpnUsername) && !TextUtils.isEmpty(vpnPassword)) {
                                AtomDemoApplicationController.getInstance().getAtomManager().setVPNCredentials(new VPNCredentials(vpnUsername,vpnPassword));
                            }else if(!TextUtils.isEmpty(uuid)){
                                AtomDemoApplicationController.getInstance().getAtomManager().setUUID(uuid);
                            }

                            vpnProperties = vpnPropertiesBuilder.build();
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
                    Log.e("error", atomException.getMessage() + " : " + atomException.getCode());

                }

                @Override
                public void onNetworkError(AtomException atomException) {
                    Log.e("error", atomException.getMessage() + " : " + atomException.getCode());

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
                    Log.e("error", atomException.getMessage() + " : " + atomException.getCode());

                }

                @Override
                public void onNetworkError(AtomException atomException) {
                    Log.e("error", atomException.getMessage() + " : " + atomException.getCode());

                }

            });
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
    //         AtomDemoApplicationController.getInstance().getAtomManager().getOptimizedCountries(new CollectionCallback<Country>() {
//             @Override
//             public void onSuccess(List<Country> list) {
//                    Log.e("list",list.size()+"");
//
//             }
//
//             @Override
//             public void onError(AtomException exception) {
//
//             }
//
//             @Override
//             public void onNetworkError(AtomException exception) {
//
//             }
//         });


    @Override
    public void onConnected() {

    }

    @Override
    public void onConnected(ConnectionDetails connectionDetails) {
        Log.d("connected",   "Connected");
        changeButtonState(btnConnect, "Disconnect");
        Log.d("ip",AtomDemoApplicationController.getInstance().getAtomManager().getConnectedIp(getActivity()));
        Date date = AtomDemoApplicationController.getInstance().getAtomManager().getConnectedTime(getActivity());

        if(date!=null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Log.d("date", sdf.format(date));
        }

    }

    @Override
    public void onConnecting() {

    }


    @Override
    public void onRedialing(AtomException atomException, ConnectionDetails connectionDetails) {
        Log.d("onRedialing", atomException.getMessage());
    }

    @Override
    public void onDialError(AtomException atomException, ConnectionDetails connectionDetails) {

        if(!AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
            Log.d("onDialError", atomException.getMessage());
            if (atomException.getCode() != Errors._5039)
                changeButtonState(btnConnect, "Connect");
        }

        if(atomException!=null) {
            Log.d("Code", atomException.getCode() + "");
            if (atomException.getMessage() != null)
                Log.d("Message", atomException.getMessage());
            if (atomException.getCause() != null)
                Log.d("Cause", atomException.getCause() + "");
        }
    }


    @Override
    public void onStateChange(String state) {

        Log.e("state",  state );
        if (state.equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING) || state.equalsIgnoreCase(VPNState.RECONNECTING)) {
            changeButtonState(btnConnect, "Cancel");
        }
    }


    @Override
    public void onDisconnected(boolean isCancelled) {

    }

    @Override
    public void onDisconnected(ConnectionDetails connectionDetails) {
        if (connectionDetails.isCancelled()) {
            Log.d("onCancelled",   "Cancelled");
        } else {
            Log.d("onDisconnected",   "Disconnected");
        }

        changeButtonState(btnConnect, "Connect");

    }

    @Override
    public void onPacketsTransmitted(String in, String out) {

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
