/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo

import android.graphics.Color
import android.text.TextUtils
import android.widget.Toast
import androidx.multidex.MultiDexApplication
import com.atom.core.exceptions.AtomValidationException
import com.atom.core.models.AtomConfiguration
import com.atom.core.models.AtomNotification
import com.atom.sdk.android.AtomManager
import com.atom.vpn.demo.common.Constants

/**
 * Shorthand for the SDK manager held by the application instance.
 *
 * Still nullable: the manager only exists once the SDK's asynchronous initialize callback has
 * fired. Prefer `atom?.foo()` over the long `AtomDemoApplicationController.instance.atomManager`
 * chain that the Java sources use.
 */
internal val atom: AtomManager?
    get() = AtomDemoApplicationController.instance.atomManager

/**
 * AtomDemoApplicationController
 */
class AtomDemoApplicationController : MultiDexApplication() {

    /**
     * Assigned when the SDK's asynchronous initialize callback fires, so this is genuinely
     * nullable for a window after start-up. Callers must handle the not-yet-initialised case.
     */
    var atomManager: AtomManager? = null

    private val notificationId = com.atom.sdk.android.common.Constants.Notification.DEFAULT_ID

    override fun onCreate() {
        super.onCreate()
        instance = this

        //put ATOM Application Secret here
        val atomSecretKey = getString(R.string.atom_secret_key)

        if (!TextUtils.isEmpty(atomSecretKey)) {

            // configure the ATOM SDK
            val atomConfigurationBuilder = AtomConfiguration.Builder(atomSecretKey)
            atomConfigurationBuilder.setVpnInterfaceName("Atom SDK Demo")
            val atomNotificationBuilder = AtomNotification.Builder(
                notificationId,
                "Atom SDK Demo",
                "You are now secured with Atom",
                R.drawable.ic_stat_icn_connected,
                Color.BLUE
            )
            atomConfigurationBuilder.setNotification(atomNotificationBuilder.build())
            atomConfigurationBuilder.enableVPNPause()
            val atomConfiguration = atomConfigurationBuilder.build()
            try {
                AtomManager.initialize(this, atomConfiguration) { mAtomManager ->
                    atomManager = mAtomManager
                }
            } catch (e: AtomValidationException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, Constants.SecretKeyRequired, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        /**
         * Assigned in [onCreate], which the framework runs before any other component of the
         * app, so this is non-null for as long as the process lives.
         */
        @JvmStatic
        lateinit var instance: AtomDemoApplicationController
            private set
    }
}
