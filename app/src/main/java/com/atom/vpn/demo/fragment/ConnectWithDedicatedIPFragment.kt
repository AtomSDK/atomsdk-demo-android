/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.atom.core.exceptions.AtomAPIException
import com.atom.core.exceptions.AtomException
import com.atom.core.exceptions.AtomValidationException
import com.atom.core.models.AtomConfiguration
import com.atom.core.models.Protocol
import com.atom.sdk.android.AtomManager
import com.atom.sdk.android.ConnectionDetails
import com.atom.sdk.android.Errors
import com.atom.sdk.android.ProtocolName
import com.atom.sdk.android.ProtocolType
import com.atom.sdk.android.VPNCredentials
import com.atom.sdk.android.VPNProperties
import com.atom.sdk.android.VPNStateListener
import com.atom.vpn.demo.R
import com.atom.vpn.demo.activity.ConnectActivity
import com.atom.vpn.demo.atom
import com.atom.vpn.demo.common.Constants
import com.atom.vpn.demo.common.Utilities.changeButtonState
import com.atom.vpn.demo.common.Utilities.changeButtonText
import com.atom.vpn.demo.common.logger.Log

class ConnectWithDedicatedIPFragment : Fragment(), VPNStateListener {

    private lateinit var etDedicatedIP: EditText

    //IKEV is only supported protocol for dedicated ip
    private var supportedProtocol: Protocol? = null
    private var btnConnect: Button? = null

    private var uuid: String? = null
    private var vpnUsername: String? = null
    private var vpnPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = arguments
        if (extras != null) {
            if (extras.containsKey("uuid")) {
                uuid = extras.getString("uuid")
            }
            if (extras.containsKey("vpnUsername")) {
                vpnUsername = extras.getString("vpnUsername")
            }
            if (extras.containsKey("vpnPassword")) {
                vpnPassword = extras.getString("vpnPassword")
            }
        }

        AtomManager.addVPNStateListener(this)

        val act = activity
        if (act != null) {
            Handler(Looper.getMainLooper()).postDelayed(
                { atom?.bindIKEVStateService(act) },
                500
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connect_with_dedicatedip, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etDedicatedIP = view.findViewById(R.id.etDedicatedIP)

        val etProtocol = view.findViewById<EditText>(R.id.etProtocol)
        etProtocol.isClickable = false

        btnConnect = view.findViewById(R.id.btnConnect)
        changeButtonText(requireContext(), btnConnect)
        btnConnect?.setOnClickListener {

            val manager = atom
            if (manager != null) {
                val ctx = requireContext()
                val status = manager.getCurrentVpnStatus(ctx)

                if (status.equals(AtomManager.VPNStatus.CONNECTED, ignoreCase = true)) {
                    manager.disconnect(ctx)
                } else if (status.equals(AtomManager.VPNStatus.CONNECTING, ignoreCase = true)) {
                    manager.cancel(ctx)
                } else {
                    if (etDedicatedIP.text.toString().trim().isNotEmpty()) {

                        (activity as? ConnectActivity)?.logWrapper?.clear()

                        etDedicatedIP.error = null

                        val protocol = supportedProtocol
                        if (protocol != null) {
                            try {
                                // Dedicated Host here
                                val vpnPropertiesBuilder = VPNProperties.Builder(
                                    etDedicatedIP.text.toString(), protocol
                                )

                                val user = vpnUsername
                                val pass = vpnPassword
                                val id = uuid
                                if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                                    manager.setVPNCredentials(VPNCredentials(user, pass))
                                } else if (!id.isNullOrEmpty()) {
                                    @Suppress("DEPRECATION")
                                    manager.setUUID(id)
                                }

                                btnConnect?.text = "Cancel"

                                manager.connect(ctx, vpnPropertiesBuilder.build())

                            } catch (e: AtomValidationException) {
                                e.printStackTrace()
                            }
                        }

                    } else {
                        etDedicatedIP.error = Constants.HostRequired
                    }
                }
            }
        }

        // Only Ikev2 Protocol is supported from ATOM SDK
        val protocol = Protocol()
        protocol.name = ProtocolName.IKEV
        protocol.protocol = ProtocolType.IKEV
        supportedProtocol = protocol

        etProtocol.setText(protocol.name)

        val manager = atom
        if (manager != null &&
            manager.getCurrentVpnStatus(requireContext())
                .equals(AtomManager.VPNStatus.CONNECTED, ignoreCase = true)
        ) {
            changeButtonState(btnConnect, "Disconnect")
        }
    }

    // Legacy overload, deprecated by the SDK in favour of onConnected(ConnectionDetails).
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onConnected() {
    }

    override fun onConnected(connectionDetails: ConnectionDetails) {
        Log.d("connected", "Connected")
        changeButtonState(btnConnect, "Disconnect")
    }

    // Legacy overload, deprecated by the SDK in favour of onConnecting(VPNProperties, ...).
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onConnecting() {
    }

    override fun onConnecting(
        vpnProperties: VPNProperties,
        atomConfiguration: AtomConfiguration
    ) {
    }

    override fun onRedialing(
        atomException: AtomException,
        connectionDetails: ConnectionDetails
    ) {
        Log.d(TAG, atomException.message)
    }

    override fun onDialError(
        atomException: AtomException,
        connectionDetails: ConnectionDetails
    ) {

        val manager = atom
        val ctx = context
        if (manager != null && ctx != null &&
            !manager.getCurrentVpnStatus(ctx)
                .equals(AtomManager.VPNStatus.CONNECTED, ignoreCase = true)
        ) {
            Log.d("onDialError", atomException.message)
            if (atomException.code != Errors._5039) {
                changeButtonState(btnConnect, "Connect")
            }
        }

        Log.d(TAG, atomException.code.toString())
        Log.d(TAG, atomException.message)

        val inner = atomException.exception
        if (inner is AtomAPIException) {
            Log.d(TAG, "${inner.errorMessage} - ${inner.code}")
        }
    }

    override fun onStateChange(state: String) {

        Log.d(TAG, state)

        if (state.equals(AtomManager.VPNStatus.CONNECTING, ignoreCase = true) ||
            state.equals(VPNStateListener.VPNState.RECONNECTING, ignoreCase = true)
        ) {
            changeButtonState(btnConnect, "Cancel")
        }
    }

    override fun onPaused(e: AtomException, connectionDetails: ConnectionDetails) {
    }

    override fun onDisconnecting(connectionDetails: ConnectionDetails) {
    }

    // Legacy overload, deprecated by the SDK in favour of onDisconnected(ConnectionDetails).
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onDisconnected(isCancelled: Boolean) {
    }

    override fun onDisconnected(connectionDetails: ConnectionDetails) {
        if (connectionDetails.isCancelled) {
            Log.d(TAG, "Cancelled")
        } else {
            Log.d(TAG, "Disconnected")
        }

        changeButtonState(btnConnect, "Connect")
    }

    override fun onUnableToAccessInternet(
        atomException: AtomException,
        connectionDetails: ConnectionDetails
    ) {
    }

    override fun onPacketsTransmitted(s: String, s1: String, s2: String, s3: String) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AtomManager.removeVPNStateListener(this)
        val act = activity
        if (act != null) {
            atom?.unBindIKEVStateService(act)
        }
    }

    companion object {
        private const val TAG = "ConnectWithDedicatedIPFragment"
    }
}
