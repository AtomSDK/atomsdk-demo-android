/*
 * Copyright (c) 2018 Atom SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.ProtocolType;
import com.atom.sdk.android.ServerType;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.sdk.android.data.callbacks.CollectionCallback;
import com.atom.sdk.android.data.model.lastConnectionDetail.ConnectionDetails;
import com.atom.sdk.android.data.model.protocol.Protocol;

import com.atom.sdk.android.exceptions.AtomException;
import com.atom.sdk.android.exceptions.AtomValidationException;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.logger.Log;
import com.tooltip.Tooltip;

import java.util.List;

import static br.com.zbra.androidlinq.Linq.stream;
import static com.atom.vpn.demo.common.Utilities.changeButtonState;
import static com.atom.vpn.demo.common.Utilities.changeButtonText;


public class ConnectWithDedicatedIPFragment extends Fragment implements VPNStateListener {

    private static final String TAG = "ConnectWithDedicatedIPFragment";
    EditText etDedicatedIP;
    EditText etProtocol;
    List<Protocol> protocolList;
    //IKEV is only supported protocol for dedicated ip
    Protocol supportedProtocol;
    Switch switchSkipUserVerification;
    Button btnConnect;

    public ConnectWithDedicatedIPFragment() {
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
        return inflater.inflate(R.layout.fragment_connect_with_dedicatedip, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchSkipUserVerification = (Switch) view.findViewById(R.id.switchSkipUserVerification);

        ImageView skipUserVerificationHint = (ImageView) view.findViewById(R.id.skipUserVerificationHint);
        Tooltip.Builder skipUserVerificationHintTipBuilder = new Tooltip.Builder(skipUserVerificationHint, R.style.TooltipStyle);
        Tooltip skipUserVerificationHintTip = skipUserVerificationHintTipBuilder.setText(Constants.TooltipSkipVerify).setDismissOnClick(true).build();

        skipUserVerificationHint.setOnClickListener(v -> {

            if (!skipUserVerificationHintTip.isShowing()) {
                skipUserVerificationHintTip.show();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        skipUserVerificationHintTip.dismiss();
                    }
                }, 3000);
            } else {
                skipUserVerificationHintTip.dismiss();
            }

        });


        etDedicatedIP = (EditText) view.findViewById(R.id.etDedicatedIP);

        etProtocol = (EditText) view.findViewById(R.id.etProtocol);
        etProtocol.setClickable(false);

        btnConnect = (Button) view.findViewById(R.id.btnConnect);
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
                        connectActivity.logWrapper.clear();

                        etDedicatedIP.setError(null);

                        if (supportedProtocol != null) {

                            btnConnect.setText("Cancel");

                            try {
                                // Dedicated Host here
                                VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(
                                        etDedicatedIP.getText().toString(), supportedProtocol, ServerType.LINUX);

                                if (switchSkipUserVerification.isChecked()) {
                                    vpnPropertiesBuilder.withSkipUserVerification();
                                }

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

        // get Protocols from Atom SDK
        if(AtomDemoApplicationController.getInstance().getAtomManager()!=null) {
            AtomDemoApplicationController.getInstance().getAtomManager().getProtocols(new CollectionCallback<Protocol>() {
                @Override
                public void onError(AtomException atomException) {
                    Log.e("error", atomException.getMessage() + " : " + atomException.getCode());

                }

                @Override
                public void onNetworkError(AtomException atomException) {
                    Log.e("error", atomException.getMessage() + " : " + atomException.getCode());

                }

                @Override
                public void onSuccess(List<Protocol> list) {
                    protocolList = list;

                    // Dedicated IP Supported Protocol
                    if (protocolList != null) {
                        supportedProtocol = stream(list)
                                .where(c -> c.getNumber() == ProtocolType.IKEV).first();

                        etProtocol.setText(supportedProtocol.getName());
                    }
                }
            });
        }

    }

    @Override
    public void onConnected() {
        Log.e("connected",   "Connected");
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
        if(atomException.getCode()!= Errors._5039)
            changeButtonState(btnConnect, "Connect");
    }

    @Override
    public void onStateChange(String state) {
        Log.d("state", state);

        if (state.equalsIgnoreCase(AtomManager.VPNStatus.CONNECTING)) {

            changeButtonState(btnConnect, "Cancel");
        }
    }

    @Override
    public void onDisconnected(boolean isCancelled) {
        if (isCancelled) {
            Log.d("onCancelled", "Cancelled");
        } else {
            Log.d("onDisconnected", "Disconnected");
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