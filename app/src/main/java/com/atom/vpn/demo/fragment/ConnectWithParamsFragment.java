/*
 * Copyright (c) 2018 Atom SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.sdk.android.data.model.countries.Country;
import com.atom.sdk.android.data.model.lastConnectionDetail.ConnectionDetails;
import com.atom.sdk.android.data.model.protocol.Protocol;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.atom.vpn.demo.common.Utilities.changeButtonState;
import static com.atom.vpn.demo.common.Utilities.changeButtonText;


public class ConnectWithParamsFragment extends Fragment implements VPNStateListener {

    private static final String TAG = "ConnectWithParamsFragment";
    AppCompatSpinner primaryProtocolSpinner, secondaryProtocolSpinner, tertiaryProtocolSpinner, countrySpinner;
    List<Protocol> protocolList;
    List<Country> countriesList;
    Switch switchOptimizedConnection;
    Button btnConnect;
    LinearLayout laySecondaryProtocol, layTertiaryProtocol;

    public ConnectWithParamsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AtomManager.addVPNStateListener(this);

        new Handler().postDelayed(() -> AtomDemoApplicationController.getInstance().getAtomManager().bindIKEVStateService(getActivity()),500);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connect_with_params, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        laySecondaryProtocol = (LinearLayout) view.findViewById(R.id.laySecondaryProtocol);
        layTertiaryProtocol = (LinearLayout) view.findViewById(R.id.layTertiaryProtocol);

        ImageView optConnectionHint = (ImageView) view.findViewById(R.id.optConnectionHint);
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

        primaryProtocolSpinner = (AppCompatSpinner) view.findViewById(R.id.primaryProtocolSpinner);
        primaryProtocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                // Here you get the current item (a User object) that is selected by its position
                Protocol primaryProtocol = (Protocol) primaryProtocolSpinner.getAdapter().getItem(position);

                Protocol secondaryProtocol = null;
                Protocol tertiaryProtocol = null;

                if (secondaryProtocolSpinner.getSelectedItemPosition() > 0) {
                    secondaryProtocol = (Protocol) secondaryProtocolSpinner.getAdapter().getItem(position);
                }

                if (tertiaryProtocolSpinner.getSelectedItemPosition() > 0) {
                    tertiaryProtocol = (Protocol) tertiaryProtocolSpinner.getAdapter().getItem(position);
                }

                List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                displayCountries(filteredCountry);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
            }
        });

        secondaryProtocolSpinner = (AppCompatSpinner) view.findViewById(R.id.secondaryProtocolSpinner);
        secondaryProtocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                // Here you get the current item (a Protocol object) that is selected by its position
                if (position > 0) {
                    Protocol primaryProtocol = (Protocol) primaryProtocolSpinner.getSelectedItem();

                    Protocol secondaryProtocol = null;
                    Protocol tertiaryProtocol = null;
                    if (secondaryProtocolSpinner.getSelectedItemPosition() > 0) {
                        secondaryProtocol = (Protocol) secondaryProtocolSpinner.getSelectedItem();
                    }

                    if (tertiaryProtocolSpinner.getSelectedItemPosition() > 0) {
                        tertiaryProtocol = (Protocol) tertiaryProtocolSpinner.getSelectedItem();
                    }

                    List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                    displayCountries(filteredCountry);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {

            }
        });

        tertiaryProtocolSpinner = (AppCompatSpinner) view.findViewById(R.id.tertiaryProtocolSpinner);
        tertiaryProtocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                if (position > 0) {

                    Protocol primaryProtocol = (Protocol) primaryProtocolSpinner.getSelectedItem();

                    Protocol secondaryProtocol = null;
                    Protocol tertiaryProtocol = null;

                    if (secondaryProtocolSpinner.getSelectedItemPosition() > 0) {
                        secondaryProtocol = (Protocol) secondaryProtocolSpinner.getSelectedItem();
                    }

                    if (tertiaryProtocolSpinner.getSelectedItemPosition() > 0) {
                        tertiaryProtocol = (Protocol) tertiaryProtocolSpinner.getSelectedItem();
                    }

                    List<Country> filteredCountry = getCountriesByAllSelectedProtocol(countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                    displayCountries(filteredCountry);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
            }
        });

        countrySpinner = (AppCompatSpinner) view.findViewById(R.id.countrySpinner);

        switchOptimizedConnection = (Switch) view.findViewById(R.id.switchOptConnection);

        btnConnect = (Button) view.findViewById(R.id.btnConnect);
        changeButtonText(getActivity(), btnConnect);
        btnConnect.setOnClickListener(v -> {

            if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {

                if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
                    AtomDemoApplicationController.getInstance().getAtomManager().disconnect(getActivity());

                } else if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING)) {
                    AtomDemoApplicationController.getInstance().getAtomManager().cancel(getActivity());
                } else {


                    // Put username and password here
                    Protocol primaryProtocol = (Protocol) primaryProtocolSpinner.getSelectedItem();

                    Country country = (Country) countrySpinner.getSelectedItem();

                    if (primaryProtocol != null) {

                        btnConnect.setText("Cancel");

                        ConnectActivity connectActivity = (ConnectActivity)getActivity();
                        connectActivity.logWrapper.clear();

                        // Dedicated Host here
                        try {
                            VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(country, primaryProtocol);

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

                            AtomDemoApplicationController.getInstance().getAtomManager().connect(getActivity(), vpnPropertiesBuilder.build());

                        } catch (AtomValidationException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        });

        // get Protocols from Atom SDK
        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            AtomDemoApplicationController.getInstance().getAtomManager().getProtocols(new CollectionCallback<Protocol>() {

                @Override
                public void onSuccess(List<Protocol> protocols) {
                    protocolList = protocols;

                    if (protocolList != null) {

                        Protocol[] protocolArray = protocolList.toArray(new Protocol[protocolList.size()]);
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


        // get Countries from Atom SDK
        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            AtomDemoApplicationController.getInstance().getAtomManager().getCountries(new CollectionCallback<Country>() {
                @Override
                public void onSuccess(List<Country> countries) {
                    countriesList = countries;

                    if (countriesList != null) {
                        displayCountries(countriesList);
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
        List<Country> filteredCountries = new LinkedList<>();

        if (countries != null) {
            for (Country country : countries) {

                if (primaryProtocol != null && secondaryProtocol != null && tertiaryProtocol != null) {

                    if (country.getProtocols().contains(primaryProtocol) && country.getProtocols().contains(secondaryProtocol) && country.getProtocols().contains(tertiaryProtocol)) {
                        filteredCountries.add(country);
                    }

                } else if (primaryProtocol != null && secondaryProtocol != null) {

                    if (country.getProtocols().contains(primaryProtocol) && country.getProtocols().contains(secondaryProtocol)) {
                        filteredCountries.add(country);
                    }

                } else if (primaryProtocol != null) {

                    if (country.getProtocols().contains(primaryProtocol)) {
                        filteredCountries.add(country);
                    }

                } else {
                    return countries;
                }
            }
        }
        return filteredCountries;
    }


    private void displayCountries(List<Country> countries) {
        if (countries != null) {
            Country[] countryArray = countries.toArray(new Country[countries.size()]);
            CountryAdapter countryAdapter = new CountryAdapter(getActivity(), android.R.layout.simple_spinner_item,
                    countryArray);
            countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            countrySpinner.setAdapter(countryAdapter);
            countryAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConnected() {
        Log.d("connected",   "Connected");
        changeButtonState(btnConnect, "Disconnect");

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
        Log.d("onDialError", atomException.getMessage());
        if (atomException.getCode() != Errors._5039)
            changeButtonState(btnConnect, "Connect");
    }


    @Override
    public void onStateChange(String state) {
        Log.d("state",  state);

        if (state.equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING)) {
            changeButtonState(btnConnect, "Cancel");
        }
    }


    @Override
    public void onDisconnected(boolean isCancelled) {
        if (isCancelled) {
            Log.d("onCancelled",   "Cancelled");
        } else {
            Log.d("onDisconnected",   "Disconnected");
        }

        changeButtonState(btnConnect, "Connect");

    }

    @Override
    public void onPacketsTransmitted(String in, String out) {
        //Log.d("onPacketsTransmitted", in + " : " + out);
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
