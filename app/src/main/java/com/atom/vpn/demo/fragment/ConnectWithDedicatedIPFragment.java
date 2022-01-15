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
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.atom.core.exceptions.AtomAPIException;
import com.atom.core.exceptions.AtomException;
import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.AtomConfiguration;
import com.atom.core.models.Protocol;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.ConnectionDetails;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.ProtocolName;
import com.atom.sdk.android.ProtocolType;
import com.atom.sdk.android.VPNCredentials;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.sdk.android.common.Common;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.logger.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.atom.vpn.demo.common.Utilities.changeButtonState;
import static com.atom.vpn.demo.common.Utilities.changeButtonText;


public class ConnectWithDedicatedIPFragment extends Fragment implements VPNStateListener {

    private static final String TAG = "ConnectWithDedicatedIPFragment";
    private EditText etDedicatedIP;
    //IKEV is only supported protocol for dedicated ip
    private Protocol supportedProtocol;
    private Button btnConnect;

    private String uuid,vpnUsername,vpnPassword;

    public ConnectWithDedicatedIPFragment() {
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
        return inflater.inflate(R.layout.fragment_connect_with_dedicatedip, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        etDedicatedIP =  view.findViewById(R.id.etDedicatedIP);

        EditText etProtocol = view.findViewById(R.id.etProtocol);
        etProtocol.setClickable(false);

        btnConnect = view.findViewById(R.id.btnConnect);
        changeButtonText(getActivity(),btnConnect);
        btnConnect.setOnClickListener(v -> {

            if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {

                if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
                    AtomDemoApplicationController.getInstance().getAtomManager().disconnect(getActivity());

                } else if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING)) {
                    AtomDemoApplicationController.getInstance().getAtomManager().cancel(getActivity());
                } else {
                    if (!TextUtils.isEmpty(etDedicatedIP.getText().toString().trim())) {

                        ConnectActivity connectActivity = (ConnectActivity)getActivity();
                        if(connectActivity!=null)
                        connectActivity.logWrapper.clear();

                        etDedicatedIP.setError(null);

                        if (supportedProtocol != null) {
                            try {
                                VPNProperties.Builder vpnPropertiesBuilder;
                                // Dedicated Host here

                                    vpnPropertiesBuilder = new VPNProperties.Builder(
                                            etDedicatedIP.getText().toString(), supportedProtocol);


                                        if (!TextUtils.isEmpty(vpnUsername) && !TextUtils.isEmpty(vpnPassword)) {
                                            AtomDemoApplicationController.getInstance().getAtomManager().setVPNCredentials(new VPNCredentials(vpnUsername, vpnPassword));
                                        } else if (!TextUtils.isEmpty(uuid)) {
                                            AtomDemoApplicationController.getInstance().getAtomManager().setUUID(uuid);
                                        }

                                        btnConnect.setText("Cancel");

                                        AtomDemoApplicationController.getInstance().getAtomManager().connect(getActivity(), vpnPropertiesBuilder.build());



                            } catch (AtomValidationException e) {
                                e.printStackTrace();
                            }
                        }


                    } else {
                        etDedicatedIP.setError(Constants.HostRequired);
                    }
                }
            }

        });

        // Only Ikev2 Protocol is supported from ATOM SDK
        supportedProtocol = new Protocol();
        supportedProtocol.setName(ProtocolName.IKEV);
        supportedProtocol.setProtocol(ProtocolType.IKEV);

        if(supportedProtocol!=null)
        etProtocol.setText(supportedProtocol.getName());


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
        Log.d("connected",   "Connected");
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
            Log.d("onDialError", atomException.getMessage());
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

        Log.d(TAG,  state );

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