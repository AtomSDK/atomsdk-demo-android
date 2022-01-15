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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.atom.core.exceptions.AtomAPIException;
import com.atom.core.exceptions.AtomException;
import com.atom.core.exceptions.AtomValidationException;
import com.atom.core.models.AtomConfiguration;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.ConnectionDetails;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.VPNCredentials;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.logger.Log;
import com.tooltip.Tooltip;


import static com.atom.vpn.demo.common.Utilities.changeButtonState;
import static com.atom.vpn.demo.common.Utilities.changeButtonText;


public class ConnectWithPSKFragment extends Fragment implements VPNStateListener {

    private static final String TAG = "ConnectWithPSKFragment";
    private EditText etPSK;
    private Button btnConnect;
    private String uuid,vpnUsername,vpnPassword;

    public ConnectWithPSKFragment() {
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
        return inflater.inflate(R.layout.fragment_connect_with_psk, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPSK = view.findViewById(R.id.etPSK);

        ImageView pskHint =  view.findViewById(R.id.pskHint);
        Tooltip.Builder pskHintTipBuilder = new Tooltip.Builder(pskHint, R.style.TooltipStyle);
        Tooltip pskHintTip = pskHintTipBuilder.setText(Constants.TooltipPSK).setDismissOnClick(true).build();

        pskHint.setOnClickListener(v -> {

            if (!pskHintTip.isShowing()) {
                pskHintTip.show();
                final Handler handler = new Handler();
                handler.postDelayed(pskHintTip::dismiss, 3000);
            } else {
                pskHintTip.dismiss();
            }

        });

        btnConnect =  view.findViewById(R.id.btnConnect);
        changeButtonText(getActivity(),btnConnect);
        btnConnect.setOnClickListener(v -> {

            if(AtomDemoApplicationController.getInstance().getAtomManager()!=null) {

                    if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTED)) {
                        AtomDemoApplicationController.getInstance().getAtomManager().disconnect(getActivity());

                    } else if (AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(getActivity()).equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING)) {
                        AtomDemoApplicationController.getInstance().getAtomManager().cancel(getActivity());

                    } else {
                        if (!TextUtils.isEmpty(etPSK.getText().toString().trim())) {

                            btnConnect.setText("Cancel");

                            ConnectActivity connectActivity = (ConnectActivity)getActivity();
                            if(connectActivity!=null) {
                                connectActivity.logWrapper.clear();

                                etPSK.setError(null);
                                try {
                                    // Put PSK here
                                    VPNProperties.Builder vpnProperties = new VPNProperties.Builder(etPSK.getText().toString());

                                    if (!TextUtils.isEmpty(vpnUsername) && !TextUtils.isEmpty(vpnPassword)) {
                                        AtomDemoApplicationController.getInstance().getAtomManager().setVPNCredentials(new VPNCredentials(vpnUsername, vpnPassword));
                                    } else if (!TextUtils.isEmpty(uuid)) {
                                        AtomDemoApplicationController.getInstance().getAtomManager().setUUID(uuid);
                                    }

                                    VPNProperties vpnProperties1 = vpnProperties.build();

                                    AtomDemoApplicationController.getInstance().getAtomManager().connect(getActivity(), vpnProperties1);

                                } catch (AtomValidationException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            etPSK.setError(Constants.PSKRequired);
                        }
                }
            }else{
                Toast.makeText(getActivity(),"Atom Manager is not initialized",Toast.LENGTH_LONG).show();

            }

        });

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
        //Log.d(TAG, atomException.getMessage());
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
        Log.d(TAG,  state);

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
        Log.d(TAG, atomException.getMessage() + "");

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