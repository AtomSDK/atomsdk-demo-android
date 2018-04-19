/*
 * Copyright (c) 2018 Atom SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import com.atom.sdk.android.VPNCredentials;

import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.activity.ConnectActivity;
import com.atom.vpn.demo.common.Constants;
import com.tooltip.Tooltip;

import static com.atom.vpn.demo.common.Utilities.setViewAndChildrenEnabled;


public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";
    Switch autoGenerateSwitch;
    LinearLayout usernameLay;
    LinearLayout passwordLay;
    LinearLayout uuidLay;
    EditText etUsername;
    EditText etPassword;
    EditText etUUID;
    TextView tvSecretKey;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout secretKeyLay = (LinearLayout) view.findViewById(R.id.secretKeyLay);
        Handler mHandler = new Handler();
        mHandler.postDelayed(() -> secretKeyLay.setAlpha(0.5f), 500);


        tvSecretKey = (TextView) view.findViewById(R.id.tvSecretKey);
        TextView secretKey = (TextView) view.findViewById(R.id.secretKey);
        if (TextUtils.isEmpty(getString(R.string.atom_secret_key))) {
            secretKey.setText(R.string.empty_secret_key);
        } else {
            secretKey.setText(getString(R.string.atom_secret_key));
        }

        usernameLay = (LinearLayout) view.findViewById(R.id.usernameLay);
        passwordLay = (LinearLayout) view.findViewById(R.id.passwordLay);
        uuidLay = (LinearLayout) view.findViewById(R.id.uuidLay);

        setViewAndChildrenEnabled(usernameLay, true);
        setViewAndChildrenEnabled(passwordLay, true);
        setViewAndChildrenEnabled(uuidLay, false);

        ImageView autoGenerateHint = (ImageView) view.findViewById(R.id.autoGenerateHint);
        Tooltip.Builder autoGenerateTipBuilder = new Tooltip.Builder(autoGenerateHint, R.style.TooltipStyle);
        Tooltip autoGenerateTip = autoGenerateTipBuilder.setText(Constants.TooltipAutoGenCred).setDismissOnClick(true).build();

        autoGenerateHint.setOnClickListener(v -> {

            if (!autoGenerateTip.isShowing()) {
                autoGenerateTip.show();
                final Handler handler = new Handler();
                handler.postDelayed(autoGenerateTip::dismiss, 3000);
            } else {
                autoGenerateTip.dismiss();
            }

        });


        autoGenerateSwitch = (Switch) view.findViewById(R.id.autoGenerateSwitch);
        autoGenerateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // do something, the isChecked will be
            // true if the switch is in the On position
            etUsername.setError(null);
            etPassword.setError(null);
            if (isChecked) {
                setViewAndChildrenEnabled(usernameLay, false);
                setViewAndChildrenEnabled(passwordLay, false);
                setViewAndChildrenEnabled(uuidLay, true);

            } else {

                setViewAndChildrenEnabled(usernameLay, true);
                setViewAndChildrenEnabled(passwordLay, true);
                setViewAndChildrenEnabled(uuidLay, false);
            }
        });


        etUsername = (EditText) view.findViewById(R.id.etUsername);
        etPassword = (EditText) view.findViewById(R.id.etPassword);
        etUUID = (EditText) view.findViewById(R.id.etUUID);


        // Connect with Pre-Shared Key
        view.findViewById(R.id.btnConnectPsk).setOnClickListener(v -> {

            if (!autoGenerateSwitch.isChecked()) {
                etUUID.setError(null);
                if (TextUtils.isEmpty(etUsername.getText().toString().trim())) {
                    etUsername.setError(Constants.UsernameRequired);

                }
                else if (TextUtils.isEmpty(etPassword.getText().toString().trim())) {
                    etUsername.setError(null);
                    etPassword.setError(Constants.PasswordRequired);
                } else {

                    etUsername.setError(null);
                    etPassword.setError(null);

                    if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
                        AtomDemoApplicationController.getInstance().getAtomManager().setVPNCredentials(new VPNCredentials(etUsername.getText().toString(), etPassword.getText().toString()));

                        Intent intent = new Intent(getActivity(), ConnectActivity.class);
                        intent.putExtra("connection_type", 1);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), "Atom Manager is not initialized", Toast.LENGTH_LONG).show();
                    }
                }

            } else {
                etUsername.setError(null);
                etPassword.setError(null);

                if (TextUtils.isEmpty(etUUID.getText().toString().trim())) {
                    etUUID.setError(Constants.UUIDRequired);
                } else {
                    etUUID.setError(null);

                    if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
                        AtomDemoApplicationController.getInstance().getAtomManager().setUUID(etUUID.getText().toString().trim());

                        Intent intent = new Intent(getActivity(), ConnectActivity.class);
                        intent.putExtra("connection_type", 1);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), "Atom Manager is not initialized", Toast.LENGTH_LONG).show();
                    }
                }

            }

        });



        // Connect with Params
        view.findViewById(R.id.btnConnectParams).setOnClickListener(v -> {

            if (!autoGenerateSwitch.isChecked()) {
                etUUID.setError(null);
                if (TextUtils.isEmpty(etUsername.getText().toString().trim())) {
                    etUsername.setError(Constants.UsernameRequired);

                } else if (TextUtils.isEmpty(etPassword.getText().toString().trim())) {
                    etUsername.setError(null);
                    etPassword.setError(Constants.PasswordRequired);
                } else {

                    etUsername.setError(null);
                    etPassword.setError(null);

                    if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
                        AtomDemoApplicationController.getInstance().getAtomManager().setVPNCredentials(new VPNCredentials(etUsername.getText().toString(), etPassword.getText().toString()));

                        Intent intent = new Intent(getActivity(), ConnectActivity.class);
                        intent.putExtra("connection_type", 2);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), "Atom Manager is not initialized", Toast.LENGTH_LONG).show();
                    }

                }

            } else {
                etUsername.setError(null);
                etPassword.setError(null);

                if (TextUtils.isEmpty(etUUID.getText().toString().trim())) {
                    etUUID.setError(Constants.UUIDRequired);
                } else {
                    etUUID.setError(null);

                    if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
                        AtomDemoApplicationController.getInstance().getAtomManager().setUUID(etUUID.getText().toString().trim());

                        Intent intent = new Intent(getActivity(), ConnectActivity.class);
                        intent.putExtra("connection_type", 2);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), "Atom Manager is not initialized", Toast.LENGTH_LONG).show();
                    }


                }
            }

        });


        // Connect with Dedicated IP
        view.findViewById(R.id.btnConnectDedicatedIp).setOnClickListener(v -> {

            if (!autoGenerateSwitch.isChecked()) {
                etUUID.setError(null);
                if (TextUtils.isEmpty(etUsername.getText().toString().trim())) {
                    etUsername.setError(Constants.UsernameRequired);

                }
                else if (TextUtils.isEmpty(etPassword.getText().toString().trim())) {
                    etUsername.setError(null);
                    etPassword.setError(Constants.PasswordRequired);
                } else {

                    etUsername.setError(null);
                    etPassword.setError(null);

                    if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
                        AtomDemoApplicationController.getInstance().getAtomManager().setVPNCredentials(new VPNCredentials(etUsername.getText().toString(), etPassword.getText().toString()));

                        Intent intent = new Intent(getActivity(), ConnectActivity.class);
                        intent.putExtra("connection_type", 3);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), "Atom Manager is not initialized", Toast.LENGTH_LONG).show();
                    }
                }

            } else {
                etUsername.setError(null);
                etPassword.setError(null);

                if (TextUtils.isEmpty(etUUID.getText().toString().trim())) {
                    etUUID.setError(Constants.UUIDRequired);
                } else {
                    etUUID.setError(null);

                    if (AtomDemoApplicationController.getInstance().getAtomManager() != null) {
                        AtomDemoApplicationController.getInstance().getAtomManager().setUUID(etUUID.getText().toString().trim());

                        Intent intent = new Intent(getActivity(), ConnectActivity.class);
                        intent.putExtra("connection_type", 3);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), "Atom Manager is not initialized", Toast.LENGTH_LONG).show();
                    }
                }
            }

        });


    }


}
