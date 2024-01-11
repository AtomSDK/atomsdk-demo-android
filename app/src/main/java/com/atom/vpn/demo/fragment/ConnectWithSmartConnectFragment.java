/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.Fragment;

import com.atom.core.exceptions.AtomAPIException;
import com.atom.core.exceptions.AtomException;
import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.AtomConfiguration;
import com.atom.core.models.Protocol;
import com.atom.core.models.SmartConnectTag;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.ConnectionDetails;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.VPNCredentials;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.sdk.android.common.Common;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.adapter.ProtocolAdapter;
import com.atom.vpn.demo.common.logger.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.atom.vpn.demo.common.Utilities.changeButtonState;
import static com.atom.vpn.demo.common.Utilities.changeButtonText;


public class ConnectWithSmartConnectFragment extends Fragment implements VPNStateListener {

    private static final String TAG = "ConnectWithSmartConnectFragment";
    private AppCompatSpinner primaryProtocolSpinner;
    private ListView smartConnectTagsListView;
    private ArrayAdapter<String> smartConnectAdapter;

    private List<Protocol> protocolList;

    private Button btnConnect;

    private String uuid, vpnUsername, vpnPassword;

    private Protocol primaryProtocol = null;

    public ConnectWithSmartConnectFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = this.getArguments();
        if (extras != null) {

            if (extras.containsKey("uuid")) {
                uuid = extras.getString("uuid");
            }

            if (extras.containsKey("vpnUsername")) {
                vpnUsername = extras.getString("vpnUsername");
            }

            if (extras.containsKey("vpnPassword")) {
                vpnPassword = extras.getString("vpnPassword");
            }

        }

        AtomManager.addVPNStateListener(this);

        if (getActivity() != null)
            new Handler().postDelayed(() -> AtomDemoApplicationController.getInstance().getAtomManager().bindIKEVStateService(getActivity()), 500);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connect_with_smartconnect, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        smartConnectTagsListView = view.findViewById(R.id.smartConnectTagsList);

        btnConnect = view.findViewById(R.id.btnConnect);
        changeButtonText(getActivity(), btnConnect);
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


                    if (primaryProtocol != null) {

                        btnConnect.setText("Cancel");

                        ConnectActivity connectActivity = (ConnectActivity)getActivity();
                        if(connectActivity!=null)
                            connectActivity.logWrapper.clear();

                        // Dedicated Host here
                        VPNProperties.Builder vpnPropertiesBuilder;

                        List<SmartConnectTag> smartConnectTagList = Arrays.asList(SmartConnectTag.class.getEnumConstants());
                        List<SmartConnectTag> selectedSmartConnectTag = new ArrayList<>();

                            SparseBooleanArray checked = smartConnectTagsListView.getCheckedItemPositions();
                            for (int i = 0; i < checked.size(); i++) {
                                // Item position in adapter
                                int position = checked.keyAt(i);
                                if (checked.valueAt(i))
                                    selectedSmartConnectTag.add(smartConnectTagList.get(position));
                            }

                        try {
                            vpnPropertiesBuilder = new VPNProperties.Builder(primaryProtocol, selectedSmartConnectTag);
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

        if(getActivity()!=null) {
            String[] smartConnectTags = getResources().getStringArray(R.array.smartconnect_tags);
            smartConnectAdapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_list_item_multiple_choice, smartConnectTags);
            smartConnectTagsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            smartConnectTagsListView.setAdapter(smartConnectAdapter);
        }

        primaryProtocolSpinner = view.findViewById(R.id.primaryProtocolSpinner);
        primaryProtocolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                // Here you get the current item (a User object) that is selected by its position
                primaryProtocol = (Protocol) primaryProtocolSpinner.getAdapter().getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {

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


        if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
            if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
                changeButtonState(btnConnect, "Disconnect");
            }
        }

    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onConnected(ConnectionDetails connectionDetails) {
        Log.d(TAG, "Connected");
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

        if (!AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
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

        Log.d(TAG, state);

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
            Log.d(TAG, "Cancelled");
        } else {
            Log.d(TAG, "Disconnected");
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