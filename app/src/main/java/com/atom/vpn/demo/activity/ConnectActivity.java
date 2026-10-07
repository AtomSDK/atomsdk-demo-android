package com.atom.vpn.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentTransaction;

import com.atom.sdk.android.AtomManager;
import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.base.BaseSampleActivity;
import com.atom.vpn.demo.common.logger.Log;
import com.atom.vpn.demo.common.logger.LogWrapper;
import com.atom.vpn.demo.common.logger.MessageOnlyLogFilter;
import com.atom.vpn.demo.fragment.ConnectWithChannelFragment;
import com.atom.vpn.demo.fragment.ConnectWithDedicatedIPFragment;
import com.atom.vpn.demo.fragment.ConnectWithMultipleDedicatedIPFragment;
import com.atom.vpn.demo.fragment.ConnectWithParamsFragment;

/**
 * ConnectActivity
 */

public class ConnectActivity extends BaseSampleActivity {

    /** Bottom nav items, in display order. Kept in step with {@link com.atom.vpn.demo.fragment.HomeFragment}. */
    private static final int[] NAV_ITEM_IDS = {
            R.id.navDashboard, R.id.navConnection, R.id.navLocations, R.id.navMore
    };

    int connection_type;
    public LogWrapper logWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        //initializing logging
        initializeLogging();

        setUpBottomNav();

        Bundle extras  = getIntent().getExtras();
        if(extras!=null) {
            if (extras.containsKey("connection_type")) {
                connection_type =  extras.getInt("connection_type");
            }
        }

        if (savedInstanceState == null) {

            if(connection_type == 1) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                ConnectWithParamsFragment fragment = new ConnectWithParamsFragment();
                fragment.setArguments(extras);
                transaction.replace(R.id.connect_fragment, fragment);
                transaction.commit();
            }else if(connection_type == 2) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                ConnectWithDedicatedIPFragment fragment = new ConnectWithDedicatedIPFragment();
                fragment.setArguments(extras);
                transaction.replace(R.id.connect_fragment, fragment);
                transaction.commit();
            }else if(connection_type == 3) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                ConnectWithChannelFragment fragment = new ConnectWithChannelFragment();
                fragment.setArguments(extras);
                transaction.replace(R.id.connect_fragment, fragment);
                transaction.commit();
            }else if(connection_type == 4) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                ConnectWithMultipleDedicatedIPFragment fragment = new ConnectWithMultipleDedicatedIPFragment();
                fragment.setArguments(extras);
                transaction.replace(R.id.connect_fragment, fragment);
                transaction.commit();
            }
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                AtomManager atomManager = AtomDemoApplicationController.getInstance().getAtomManager();
                if (atomManager != null && !AtomManager.VPNStatus.DISCONNECTED
                        .equalsIgnoreCase(atomManager.getCurrentVpnStatus(ConnectActivity.this))) {
                    Toast.makeText(ConnectActivity.this, Constants.DisconnectBeforeExit, Toast.LENGTH_LONG).show();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /**
     * The tab screens live in SampleMainActivity, so every item but Connection (this screen) hands
     * over to it. Unlike Back this does not wait for a disconnect: the tunnel keeps running, and
     * the connect screens re-read its state whenever they are opened again.
     */
    private void setUpBottomNav() {
        for (int id : NAV_ITEM_IDS) {
            View item = findViewById(id);
            item.setSelected(id == R.id.navConnection);
            item.setOnClickListener(v -> {
                if (v.getId() == R.id.navDashboard) {
                    openMainScreen(SampleMainActivity.SCREEN_DASHBOARD);
                } else if (v.getId() == R.id.navLocations) {
                    openMainScreen(SampleMainActivity.SCREEN_LOCATIONS);
                } else if (v.getId() == R.id.navMore) {
                    openMainScreen(SampleMainActivity.SCREEN_MORE);
                }
            });
        }
        CoachMarks.setBottomNav(findViewById(R.id.bottomNav));
    }

    /**
     * Finishes this screen and shows {@code screen} in SampleMainActivity, which is always the
     * one underneath: CLEAR_TOP drops this activity and SINGLE_TOP hands the request to the
     * existing instance rather than starting it over.
     */
    public void openMainScreen(@NonNull String screen) {
        Intent intent = new Intent(this, SampleMainActivity.class)
                .putExtra(SampleMainActivity.EXTRA_OPEN_SCREEN, screen)
                .putExtra(SampleMainActivity.EXTRA_VPN_USERNAME,
                        getIntent().getStringExtra(SampleMainActivity.EXTRA_VPN_USERNAME))
                .putExtra(SampleMainActivity.EXTRA_VPN_PASSWORD,
                        getIntent().getStringExtra(SampleMainActivity.EXTRA_VPN_PASSWORD))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    protected void setupHomeButton() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    /**
     * Keeps a node behind the demo's Log facade so Log calls from the connect screens stay valid.
     * Nothing is shown here any more: the screens report through LogStore, which the SDK Logs
     * screen displays.
     */
    @Override
    public void initializeLogging() {
        logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);
        logWrapper.setNext(new MessageOnlyLogFilter());
    }

}
