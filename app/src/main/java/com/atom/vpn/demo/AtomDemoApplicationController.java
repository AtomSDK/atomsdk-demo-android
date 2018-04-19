/*
 * Copyright (c) 2018 Atom SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo;

import android.app.Application;
import android.text.TextUtils;
import android.widget.Toast;

import com.atom.sdk.android.AtomConfiguration;
import com.atom.sdk.android.AtomManager;
import com.atom.sdk.android.exceptions.AtomValidationException;
import com.atom.vpn.demo.common.Constants;


/**
 * AtomDemoApplicationController
 */

public class AtomDemoApplicationController extends Application {

    private static AtomDemoApplicationController mInstance;
    private AtomManager atomManager;

    public static synchronized AtomDemoApplicationController getInstance() {
        return mInstance;
    }

    public AtomManager getAtomManager() {
        return atomManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        //put Atom Application Secret here
        String ATOM_SECRET_KEY = getString(R.string.atom_secret_key);

        if (!TextUtils.isEmpty(ATOM_SECRET_KEY)) {

            // configure the Atom SDK
            AtomConfiguration.Builder atomConfigurationBuilder = new AtomConfiguration.Builder(ATOM_SECRET_KEY);
            AtomConfiguration atomConfiguration = atomConfigurationBuilder.build();

            try {
                AtomManager.initialize(this, atomConfiguration, mAtomManager -> atomManager = mAtomManager);
            } catch (AtomValidationException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, Constants.SecretKeyRequired, Toast.LENGTH_SHORT).show();
        }

    }

}
