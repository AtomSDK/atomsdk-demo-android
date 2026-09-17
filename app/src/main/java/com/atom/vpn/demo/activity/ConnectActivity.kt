package com.atom.vpn.demo.activity

import android.os.Bundle
import android.widget.Toast
import android.widget.ViewAnimator
import androidx.fragment.app.Fragment
import com.atom.sdk.android.AtomManager
import com.atom.vpn.demo.R
import com.atom.vpn.demo.atom
import com.atom.vpn.demo.common.Constants
import com.atom.vpn.demo.common.base.BaseSampleActivity
import com.atom.vpn.demo.common.logger.Log
import com.atom.vpn.demo.common.logger.LogFragment
import com.atom.vpn.demo.common.logger.LogWrapper
import com.atom.vpn.demo.common.logger.MessageOnlyLogFilter
import com.atom.vpn.demo.fragment.ConnectWithChannelFragment
import com.atom.vpn.demo.fragment.ConnectWithDedicatedIPFragment
import com.atom.vpn.demo.fragment.ConnectWithParamsFragment

/**
 * ConnectActivity
 */
class ConnectActivity : BaseSampleActivity() {

    private var logFragment: LogFragment? = null
    private var connectionType = 0

    /**
     * Read directly as a field by the connect fragments, so it is exposed with [JvmField] to
     * keep it a field rather than a getter while those callers are still Java.
     */
    @JvmField
    var logWrapper: LogWrapper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        //initializing logging
        initializeLogging()

        if (savedInstanceState == null) {

            val extras = intent.extras
            if (extras != null && extras.containsKey("connection_type")) {
                connectionType = extras.getInt("connection_type")
            }

            val fragment: Fragment? = when (connectionType) {
                1 -> ConnectWithParamsFragment()
                2 -> ConnectWithDedicatedIPFragment()
                3 -> ConnectWithChannelFragment()
                else -> null
            }

            if (fragment != null) {
                fragment.arguments = extras
                supportFragmentManager.beginTransaction()
                    .replace(R.id.connect_fragment, fragment)
                    .commit()
            }
        }
    }

    override fun setupHomeButton() {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }
    }

    /**
     * Create a chain of targets that will receive log data
     */
    override fun initializeLogging() {
        // Wraps Android's native log framework.
        val wrapper = LogWrapper()
        logWrapper = wrapper
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.logNode = wrapper

        // Filter strips out everything except the message text.
        val msgFilter = MessageOnlyLogFilter()
        wrapper.next = msgFilter

        // On screen logging via a fragment with a TextView.
        val fragment = supportFragmentManager.findFragmentById(R.id.log_fragment) as LogFragment?
        logFragment = fragment
        if (fragment != null) {
            msgFilter.next = fragment.logView
        }

        val output = findViewById<ViewAnimator>(R.id.sample_output)
        output.displayedChild = 1
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        val manager = atom
        if (manager != null) {

            val vpnStatus = manager.getCurrentVpnStatus(this)
            if (!vpnStatus.equals(AtomManager.VPNStatus.DISCONNECTED, ignoreCase = true)) {
                if (!isFinishing) {
                    runOnUiThread {
                        Toast.makeText(this, Constants.DisconnectBeforeExit, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}
