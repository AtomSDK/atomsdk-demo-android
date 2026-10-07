/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.common.base;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.common.ThemeToggle;


/**
 * Base launcher activity, to handle most of the common plumbing for samples.
 */
public class BaseSampleActivity extends AppCompatActivity {

    public static final String TAG = "SampleActivityBase";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    setupHomeButton();

        // Every screen's top bar carries the theme toggle, so it is bound here once for all of them.
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm,
                                                      @NonNull Fragment f, @NonNull View v,
                                                      @Nullable Bundle savedInstanceState) {
                        ThemeToggle.bind(v);
                    }
                }, true);
    }

    protected void setupHomeButton() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onMenuHomePressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    protected void onMenuHomePressed() {
        getOnBackPressedDispatcher().onBackPressed();
    }


    @Override
    protected  void onStart() {
        super.onStart();
    }

    @Nullable
    private CoachMarks.Detector coachMarkDetector;

    /** Every touch passes the coach mark detector first; see {@link CoachMarks}. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (coachMarkDetector == null) {
            coachMarkDetector = new CoachMarks.Detector(getWindow().getDecorView(),
                    super::dispatchTouchEvent);
        }
        return coachMarkDetector.onTouch(event) || super.dispatchTouchEvent(event);
    }

    @Override
    protected void onPause() {
        if (coachMarkDetector != null) {
            coachMarkDetector.release();
        }
        CoachMarks.dismiss();
        super.onPause();
    }

    /** Set up targets to receive log data */
    public void initializeLogging() {
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        // Wraps Android's native log framework
//        LogWrapper logWrapper = new LogWrapper();
//        Log.setLogNode(logWrapper);

    }
}
