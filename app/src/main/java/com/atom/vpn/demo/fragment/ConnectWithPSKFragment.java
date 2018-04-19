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
import android.widget.Toast;

import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.Errors;
import com.atom.sdk.android.VPNProperties;
import com.atom.sdk.android.VPNStateListener;
import com.atom.sdk.android.data.model.lastConnectionDetail.ConnectionDetails;
import com.atom.sdk.android.exceptions.AtomException;
import com.atom.sdk.android.exceptions.AtomValidationException;
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
    EditText etPSK;
    Button btnConnect;

    public ConnectWithPSKFragment() {
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
        return inflater.inflate(R.layout.fragment_connect_with_psk, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPSK = (EditText) view.findViewById(R.id.etPSK);

        ImageView pskHint = (ImageView) view.findViewById(R.id.pskHint);
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

        btnConnect = (Button) view.findViewById(R.id.btnConnect);
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
                            connectActivity.logWrapper.clear();

                            etPSK.setError(null);
                            try {
                                //  Put PSK here
                                VPNProperties vpnProperties  = new VPNProperties.Builder(etPSK.getText().toString())
                                        .build();

                                AtomDemoApplicationController.getInstance().getAtomManager().connect(getActivity(), vpnProperties);

                            } catch (AtomValidationException e) {
                                e.printStackTrace();
                            }
                        } else {
                            etPSK.setError(Constants.PSKRequired);
                        }
                }
            }else{
                Toast.makeText(getActivity(),"Atom Manager is not initialized",Toast.LENGTH_LONG).show();

            }

        });


    }

    @Override
    public void onConnected() {
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