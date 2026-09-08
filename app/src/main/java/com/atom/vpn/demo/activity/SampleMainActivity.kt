/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.atom.sdk.android.AtomManager
import com.atom.vpn.demo.R
import com.atom.vpn.demo.atom
import com.atom.vpn.demo.common.base.BaseSampleActivity
import com.atom.vpn.demo.fragment.MainFragment
import com.atom.vpn.demo.fragment.VpnSetupFragment

class SampleMainActivity : BaseSampleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_main)

        if (savedInstanceState == null) {
            val transaction = supportFragmentManager.beginTransaction()
            val atomManager = AtomManager.getInstance()
            val fragment: Fragment =
                if (atomManager != null && !atomManager.isVPNServicePrepared(this)) {
                    VpnSetupFragment()
                } else {
                    MainFragment()
                }

            transaction.replace(R.id.sample_content_fragment, fragment)
            transaction.commit()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {

        val manager = atom
        if (manager != null) {
            val status = manager.getCurrentVpnStatus(this)
            if (status.equals(AtomManager.VPNStatus.CONNECTED, ignoreCase = true)) {
                manager.disconnect(this)
            } else if (status.equals(AtomManager.VPNStatus.CONNECTING, ignoreCase = true)) {
                manager.cancel(this)
            }
        }

        super.onBackPressed()
    }
}
