/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.common.base

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

/**
 * Base launcher activity, to handle most of the common plumbing for samples.
 *
 * [setupHomeButton] and [initializeLogging] are `open` because ConnectActivity overrides them.
 */
open class BaseSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupHomeButton()
    }

    protected open fun setupHomeButton() {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setHomeButtonEnabled(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onMenuHomePressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    protected open fun onMenuHomePressed() {
        @Suppress("DEPRECATION")
        onBackPressed()
    }

    override fun onStart() {
        super.onStart()
    }

    /** Set up targets to receive log data */
    open fun initializeLogging() {
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        // Wraps Android's native log framework
//        LogWrapper logWrapper = new LogWrapper();
//        Log.setLogNode(logWrapper);
    }

    companion object {
        const val TAG = "SampleActivityBase"
    }
}
