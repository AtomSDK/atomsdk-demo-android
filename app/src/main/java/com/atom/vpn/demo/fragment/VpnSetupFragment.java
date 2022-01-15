/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.atom.sdk.android.AtomManager;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.UIHelper;
import com.atom.vpn.demo.common.logger.Log;


public class VpnSetupFragment extends Fragment {

    private static final String TAG = "VpnSetupFragment";
    private Button btnGivePermission;

    public VpnSetupFragment() {
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
        return inflater.inflate(R.layout.fragment_setup_vpn, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnGivePermission = view.findViewById(R.id.btnGivePermission);

        btnGivePermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AtomManager.getInstance() != null) {
                    AtomManager.getInstance().getVPNServicePermission(VpnSetupFragment.this);
                } else {
                    Log.e(TAG, "Atom Manager is null");
                }
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AtomManager.REQUEST_VPN_PERMISSION) {
            if (resultCode == AtomManager.RESULT_VPN_PERMISSION_APPROVED) {
                Log.e(TAG, "VPN API Permission provided");
                openMainFragment();
            } else if (resultCode == AtomManager.RESULT_VPN_PERMISSION_CANCELED) {
                Log.e(TAG, "VPN API Permission cancelled");
                UIHelper.showAlertDialog(getContext(), "Please provide VPN permission to use this application");
            }
        }
    }


    private void openMainFragment() {
        FragmentManager supportFragmentManager = getActivity().getSupportFragmentManager();
        supportFragmentManager.popBackStack();

        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.replace(R.id.sample_content_fragment, new MainFragment());
        transaction.commit();
    }
}
