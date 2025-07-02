/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.common.Constants;
import com.tooltip.Tooltip;

import static com.atom.vpn.demo.common.Utilities.setViewAndChildrenEnabled;

import de.blinkt.openvpn.BuildConfig;


public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";
    private LinearLayout usernameLay;
    private LinearLayout passwordLay;
    private EditText etUsername;
    private EditText etPassword;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout secretKeyLay = view.findViewById(R.id.secretKeyLay);
        Handler mHandler = new Handler();
        mHandler.postDelayed(() -> secretKeyLay.setAlpha(0.5f), 500);


        TextView secretKey =  view.findViewById(R.id.secretKey);
        if (TextUtils.isEmpty(getString(R.string.atom_secret_key))) {
            secretKey.setText(getString(R.string.empty_secret_key));
        } else {
            secretKey.setText(getString(R.string.atom_secret_key));
        }

        ((TextView) view.findViewById(R.id.sdkVersion)).setText("Version: "+ BuildConfig.SDKVersion);

        usernameLay = view.findViewById(R.id.usernameLay);
        passwordLay =  view.findViewById(R.id.passwordLay);

        setViewAndChildrenEnabled(usernameLay, true);
        setViewAndChildrenEnabled(passwordLay, true);

        etUsername =  view.findViewById(R.id.etUsername);
        etPassword =  view.findViewById(R.id.etPassword);

        // Connect with Params
        view.findViewById(R.id.btnConnectParams).setOnClickListener(v -> {
            launchActivityForConnectionMethod(1);
        });


        // Connect with Dedicated IP
        view.findViewById(R.id.btnConnectDedicatedIp).setOnClickListener(v -> {
            launchActivityForConnectionMethod(2);
        });


        // Connect with Channel
        view.findViewById(R.id.btnConnectChannel).setOnClickListener(v -> {
            launchActivityForConnectionMethod(3);
        });
    }

    private void launchActivityForConnectionMethod(int connectionMethodType){
        if (TextUtils.isEmpty(etUsername.getText().toString().trim())) {
            etUsername.setError(Constants.UsernameRequired);
        } else if (TextUtils.isEmpty(etPassword.getText().toString().trim())) {
            etUsername.setError(null);
            etPassword.setError(Constants.PasswordRequired);
        } else {

            etUsername.setError(null);
            etPassword.setError(null);

            if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {

                Intent intent = new Intent(getActivity(), ConnectActivity.class);
                intent.putExtra("connection_type", connectionMethodType);
                intent.putExtra("vpnUsername", etUsername.getText().toString());
                intent.putExtra("vpnPassword", etPassword.getText().toString());
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else {
                Toast.makeText(getActivity(), "Atom Manager is not initialized", Toast.LENGTH_LONG).show();
            }
        }
    }

}
