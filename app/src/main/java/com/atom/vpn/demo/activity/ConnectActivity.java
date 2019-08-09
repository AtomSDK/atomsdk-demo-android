package com.atom.vpn.demo.activity;

import android.os.Bundle;
import android.widget.Toast;
import android.widget.ViewAnimator;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentTransaction;

import com.atom.sdk.android.AtomManager;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.base.BaseSampleActivity;
import com.atom.vpn.demo.common.logger.Log;
import com.atom.vpn.demo.common.logger.LogFragment;
import com.atom.vpn.demo.common.logger.LogWrapper;
import com.atom.vpn.demo.common.logger.MessageOnlyLogFilter;
import com.atom.vpn.demo.fragment.ConnectWithDedicatedIPFragment;
import com.atom.vpn.demo.fragment.ConnectWithPSKFragment;
import com.atom.vpn.demo.fragment.ConnectWithParamsFragment;

/**
 * ConnectActivity
 */

public class ConnectActivity extends BaseSampleActivity {

    LogFragment logFragment;
    int connection_type;
    public LogWrapper logWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        //initializing logging
        initializeLogging();

        if (savedInstanceState == null) {

            Bundle extras  = getIntent().getExtras();
            if(extras!=null) {
                if (extras.containsKey("connection_type")) {
                    connection_type =  extras.getInt("connection_type");
                }
            }

            if(connection_type == 1) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                ConnectWithPSKFragment fragment = new ConnectWithPSKFragment();
                fragment.setArguments(extras);
                transaction.replace(R.id.connect_fragment, fragment);
                transaction.commit();
            }else if(connection_type == 2) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                ConnectWithParamsFragment fragment = new ConnectWithParamsFragment();
                fragment.setArguments(extras);
                transaction.replace(R.id.connect_fragment, fragment);
                transaction.commit();
            }else{
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                ConnectWithDedicatedIPFragment fragment = new ConnectWithDedicatedIPFragment();
                fragment.setArguments(extras);
                transaction.replace(R.id.connect_fragment, fragment);
                transaction.commit();
            }
        }


    }

    protected void setupHomeButton() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    /**
     * Create a chain of targets that will receive log data
     */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        logWrapper= new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);


        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.

        logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        if(logFragment!=null)
        msgFilter.setNext(logFragment.getLogView());

        ViewAnimator output =  findViewById(R.id.sample_output);
        output.setDisplayedChild(1);

    }

    @Override
    public void onBackPressed() {
        if(AtomDemoApplicationController.getInstance().getAtomManager()!=null) {

            String vpnStatus = AtomDemoApplicationController.getInstance().getAtomManager().getCurrentVpnStatus(this);
            if (!vpnStatus.equalsIgnoreCase(AtomManager.VPNStatus.DISCONNECTED)) {
                if(!isFinishing()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(ConnectActivity.this, Constants.DisconnectBeforeExit, Toast.LENGTH_LONG).show();
                        }
                    });
                }            } else {
                super.onBackPressed();
            }
        }else{
            super.onBackPressed();
        }
    }

}
