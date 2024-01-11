/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.Fragment;

import com.atom.core.exceptions.AtomAPIException;
import com.atom.core.exceptions.AtomException;
import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.AtomConfiguration;
import com.atom.core.models.Channel;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.ConnectionDetails;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.VPNCredentials;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.adapter.ChannelAdapter;
import com.atom.vpn.demo.adapter.ProtocolAdapter;
import com.atom.vpn.demo.common.logger.Log;
import java.util.ArrayList;
import java.util.List;


import static br.com.zbra.androidlinq.Linq.stream;
import static com.atom.vpn.demo.common.Utilities.changeButtonState;
import static com.atom.vpn.demo.common.Utilities.changeButtonText;


public class ConnectWithChannelFragment extends Fragment implements VPNStateListener {

    private static final String TAG = "ConnectWithChannelFragment";
    private AppCompatSpinner primaryProtocolSpinner, secondaryProtocolSpinner, tertiaryProtocolSpinner, channelSpinner;
    private List<Protocol> protocolList;
    private List<Channel> channelsList;

    private Button btnConnect;

    private String uuid,vpnUsername,vpnPassword;

    private Protocol primaryProtocol = null;
    private Protocol secondaryProtocol = null;
    private Protocol tertiaryProtocol = null;

    VPNProperties vpnProperties = null;

    public ConnectWithChannelFragment() {
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
        return inflater.inflate(R.layout.fragment_connect_with_channels, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

                List<Channel> filteredChannels = getChannelsByAllSelectedProtocol(channelsList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                displayChannels(filteredChannels);

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

                    List<Channel> filteredChannels = getChannelsByAllSelectedProtocol(channelsList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                    displayChannels(filteredChannels);

                }else{
                    secondaryProtocol = null;
                }

                List<Channel> filteredChannels = getChannelsByAllSelectedProtocol(channelsList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                displayChannels(filteredChannels);
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

                    List<Channel> filteredChannels = getChannelsByAllSelectedProtocol(channelsList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                    displayChannels(filteredChannels);
                }else{
                    tertiaryProtocol = null;
                    List<Channel> filteredChannels = getChannelsByAllSelectedProtocol(channelsList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                    displayChannels(filteredChannels);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {

            }
        });

        channelSpinner =  view.findViewById(R.id.channelsSpinner);

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

                    Channel channel = (Channel) channelSpinner.getSelectedItem();

                    if (primaryProtocol != null && channel!=null) {

                        btnConnect.setText("Cancel");

                        ConnectActivity connectActivity = (ConnectActivity)getActivity();
                        if(connectActivity!=null)
                            connectActivity.logWrapper.clear();

                        // Dedicated Host here
                        VPNProperties.Builder vpnPropertiesBuilder;
                        try {

                            vpnPropertiesBuilder = new VPNProperties.Builder(channel, primaryProtocol);

                            if (secondaryProtocolSpinner.getSelectedItemPosition() >= 1) {
                                Protocol secondaryProtocol = (Protocol) secondaryProtocolSpinner.getSelectedItem();
                                vpnPropertiesBuilder.withSecondaryProtocol(secondaryProtocol);
                            }

                            if (tertiaryProtocolSpinner.getSelectedItemPosition() >= 1) {
                                Protocol tertiaryProtocol = (Protocol) tertiaryProtocolSpinner.getSelectedItem();
                                vpnPropertiesBuilder.withTertiaryProtocol(tertiaryProtocol);
                            }

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
                    Log.e(TAG, atomException.getMessage() + " : " + atomException.getCode());

                }

                @Override
                public void onNetworkError(AtomException atomException) {
                    Log.e(TAG, atomException.getMessage() + " : " + atomException.getCode());

                }
            });
        }

        // get Channels from ATOM SDK
        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            AtomDemoApplicationController.getInstance().getAtomManager().getChannels(new CollectionCallback<Channel>() {
                @Override
                public void onSuccess(List<Channel> channels) {

                    if(channels!=null) {
                        channelsList = channels;

                        List<Channel> countryList = getChannelsByAllSelectedProtocol(channelsList, primaryProtocol, secondaryProtocol, tertiaryProtocol);
                        displayChannels(countryList);
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


        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
                changeButtonState(btnConnect, "Disconnect");
            }
        }

    }

    private List<Channel> getChannelsByAllSelectedProtocol(List<Channel> channels, Protocol primaryProtocol, Protocol secondaryProtocol, Protocol tertiaryProtocol) {

        if (channels != null) {

            if (primaryProtocol != null && secondaryProtocol != null && tertiaryProtocol != null) {

                List<Protocol> protocolList = new ArrayList<>();
                protocolList.add(primaryProtocol);
                protocolList.add(secondaryProtocol);
                protocolList.add(tertiaryProtocol);
                return stream(channels)
                        .where(c ->c.getProtocols()!=null && c.getProtocols().containsAll(protocolList)).toList();

            } else if (primaryProtocol != null && secondaryProtocol != null) {


                List<Protocol> protocolList = new ArrayList<>();
                protocolList.add(primaryProtocol);
                protocolList.add(secondaryProtocol);
                return stream(channels)
                        .where(c -> c.getProtocols()!=null && c.getProtocols().containsAll(protocolList)).toList();

            }else if (primaryProtocol != null && tertiaryProtocol != null) {


                List<Protocol> protocolList = new ArrayList<>();
                protocolList.add(primaryProtocol);
                protocolList.add(tertiaryProtocol);
                return stream(channels)
                        .where(c -> c.getProtocols()!=null && c.getProtocols().containsAll(protocolList)).toList();

            } else if (primaryProtocol != null) {

                return stream(channels)
                        .where(c ->c.getProtocols()!=null && c.getProtocols().contains(primaryProtocol)).toList();

            }
        }

        return null;
    }


    private void displayChannels(List<Channel> channels) {
        if (channels != null) {
            Channel[] channelArray = channels.toArray(new Channel[0]);
            if(getActivity()!=null) {
                ChannelAdapter channelAdapter = new ChannelAdapter(getActivity(), android.R.layout.simple_spinner_item,
                        channelArray);
                channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                channelSpinner.setAdapter(channelAdapter);
                channelAdapter.notifyDataSetChanged();
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
        Log.d(TAG, atomException.getMessage());

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
