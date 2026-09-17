/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.Fragment
import com.atom.core.exceptions.AtomAPIException
import com.atom.core.exceptions.AtomException
import com.atom.core.exceptions.AtomValidationException
import com.atom.core.models.AtomConfiguration
import com.atom.core.models.Channel
import com.atom.core.models.Protocol
import com.atom.sdk.android.AtomManager
import com.atom.sdk.android.ConnectionDetails
import com.atom.sdk.android.Errors
import com.atom.sdk.android.VPNCredentials
import com.atom.sdk.android.VPNProperties
import com.atom.sdk.android.VPNStateListener
import com.atom.sdk.android.data.callbacks.CollectionCallback
import com.atom.vpn.demo.R
import com.atom.vpn.demo.activity.ConnectActivity
import com.atom.vpn.demo.adapter.ChannelAdapter
import com.atom.vpn.demo.adapter.ProtocolAdapter
import com.atom.vpn.demo.atom
import com.atom.vpn.demo.common.Utilities.changeButtonState
import com.atom.vpn.demo.common.Utilities.changeButtonText
import com.atom.vpn.demo.common.logger.Log

class ConnectWithChannelFragment : Fragment(), VPNStateListener {

    private lateinit var primaryProtocolSpinner: AppCompatSpinner
    private lateinit var secondaryProtocolSpinner: AppCompatSpinner
    private lateinit var tertiaryProtocolSpinner: AppCompatSpinner
    private lateinit var channelSpinner: AppCompatSpinner

    private var protocolList: List<Protocol>? = null
    private var channelsList: List<Channel>? = null

    private var btnConnect: Button? = null

    private var uuid: String? = null
    private var vpnUsername: String? = null
    private var vpnPassword: String? = null

    private var primaryProtocol: Protocol? = null
    private var secondaryProtocol: Protocol? = null
    private var tertiaryProtocol: Protocol? = null

    /**
     * NOTE: assigned but never read, carried over from the Java original along with the second
     * `build()` call below. Safe to delete along with that call.
     */
    internal var vpnProperties: VPNProperties? = null

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
        return inflater.inflate(R.layout.fragment_connect_with_channels, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        primaryProtocolSpinner = view.findViewById(R.id.primaryProtocolSpinner)
        primaryProtocolSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Here you get the current item (a User object) that is selected by its position
                    primaryProtocol = primaryProtocolSpinner.adapter.getItem(position) as? Protocol

                    secondaryProtocol = if (secondaryProtocolSpinner.selectedItemPosition > 0) {
                        secondaryProtocolSpinner.adapter.getItem(position) as? Protocol
                    } else {
                        null
                    }

                    tertiaryProtocol = if (tertiaryProtocolSpinner.selectedItemPosition > 0) {
                        tertiaryProtocolSpinner.adapter.getItem(position) as? Protocol
                    } else {
                        null
                    }

                    displayChannels(
                        channelsByAllSelectedProtocol(
                            channelsList, primaryProtocol, secondaryProtocol, tertiaryProtocol
                        )
                    )
                }

                override fun onNothingSelected(adapter: AdapterView<*>?) {
                }
            }

        secondaryProtocolSpinner = view.findViewById(R.id.secondaryProtocolSpinner)
        secondaryProtocolSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Here you get the current item (a Protocol object) that is selected by its position
                    if (position > 0) {
                        // NOTE: the original shadows the field with the spinner's current
                        // selection inside this branch only; preserved here.
                        val selectedPrimary = primaryProtocolSpinner.selectedItem as? Protocol

                        secondaryProtocol = if (secondaryProtocolSpinner.selectedItemPosition > 0) {
                            secondaryProtocolSpinner.adapter.getItem(position) as? Protocol
                        } else {
                            null
                        }

                        tertiaryProtocol = if (tertiaryProtocolSpinner.selectedItemPosition > 0) {
                            tertiaryProtocolSpinner.adapter.getItem(position) as? Protocol
                        } else {
                            null
                        }

                        displayChannels(
                            channelsByAllSelectedProtocol(
                                channelsList, selectedPrimary, secondaryProtocol, tertiaryProtocol
                            )
                        )
                    } else {
                        secondaryProtocol = null
                    }

                    displayChannels(
                        channelsByAllSelectedProtocol(
                            channelsList, primaryProtocol, secondaryProtocol, tertiaryProtocol
                        )
                    )
                }

                override fun onNothingSelected(adapter: AdapterView<*>?) {
                }
            }

        tertiaryProtocolSpinner = view.findViewById(R.id.tertiaryProtocolSpinner)
        tertiaryProtocolSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position > 0) {
                        val selectedPrimary = primaryProtocolSpinner.selectedItem as? Protocol

                        secondaryProtocol = if (secondaryProtocolSpinner.selectedItemPosition > 0) {
                            secondaryProtocolSpinner.adapter.getItem(position) as? Protocol
                        } else {
                            null
                        }

                        tertiaryProtocol = if (tertiaryProtocolSpinner.selectedItemPosition > 0) {
                            tertiaryProtocolSpinner.adapter.getItem(position) as? Protocol
                        } else {
                            null
                        }

                        displayChannels(
                            channelsByAllSelectedProtocol(
                                channelsList, selectedPrimary, secondaryProtocol, tertiaryProtocol
                            )
                        )
                    } else {
                        tertiaryProtocol = null
                        displayChannels(
                            channelsByAllSelectedProtocol(
                                channelsList, primaryProtocol, secondaryProtocol, tertiaryProtocol
                            )
                        )
                    }
                }

                override fun onNothingSelected(adapter: AdapterView<*>?) {
                }
            }

        channelSpinner = view.findViewById(R.id.channelsSpinner)

        btnConnect = view.findViewById(R.id.btnConnect)
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

                    changeButtonText(ctx, btnConnect)

                    // Put username and password here
                    val selectedPrimary = primaryProtocolSpinner.selectedItem as? Protocol
                    val channel = channelSpinner.selectedItem as? Channel

                    if (selectedPrimary != null && channel != null) {

                        btnConnect?.text = "Cancel"

                        (activity as? ConnectActivity)?.logWrapper?.clear()

                        try {
                            // Dedicated Host here
                            val vpnPropertiesBuilder =
                                VPNProperties.Builder(channel, selectedPrimary)

                            if (secondaryProtocolSpinner.selectedItemPosition >= 1) {
                                vpnPropertiesBuilder.withSecondaryProtocol(
                                    secondaryProtocolSpinner.selectedItem as? Protocol
                                )
                            }

                            if (tertiaryProtocolSpinner.selectedItemPosition >= 1) {
                                vpnPropertiesBuilder.withTertiaryProtocol(
                                    tertiaryProtocolSpinner.selectedItem as? Protocol
                                )
                            }

                            val user = vpnUsername
                            val pass = vpnPassword
                            val id = uuid
                            if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                                manager.setVPNCredentials(VPNCredentials(user, pass))
                            } else if (!id.isNullOrEmpty()) {
                                @Suppress("DEPRECATION")
                                manager.setUUID(id)
                            }

                            vpnProperties = vpnPropertiesBuilder.build()
                            manager.connect(ctx, vpnPropertiesBuilder.build())

                        } catch (e: AtomValidationException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // get Protocols from ATOM SDK
        atom?.getProtocols(object : CollectionCallback<Protocol> {

            override fun onSuccess(protocols: List<Protocol>?) {
                protocolList = protocols
                val list = protocols ?: return
                val ctx = activity ?: return

                primaryProtocolSpinner.adapter = protocolAdapterFor(ctx, list, null)
                secondaryProtocolSpinner.adapter =
                    protocolAdapterFor(ctx, list, "Secondary Protocol")
                tertiaryProtocolSpinner.adapter =
                    protocolAdapterFor(ctx, list, "Tertiary Protocol")
            }

            override fun onError(atomException: AtomException) {
                Log.e(TAG, "${atomException.message} : ${atomException.code}")
            }

            override fun onNetworkError(atomException: AtomException) {
                Log.e(TAG, "${atomException.message} : ${atomException.code}")
            }
        })

        // get Channels from ATOM SDK
        atom?.getChannels(object : CollectionCallback<Channel> {
            override fun onSuccess(channels: List<Channel>?) {
                if (channels != null) {
                    channelsList = channels

                    displayChannels(
                        channelsByAllSelectedProtocol(
                            channels, primaryProtocol, secondaryProtocol, tertiaryProtocol
                        )
                    )
                }
            }

            override fun onError(atomException: AtomException) {
                Log.e(TAG, "${atomException.message} : ${atomException.code}")
            }

            override fun onNetworkError(atomException: AtomException) {
                Log.e(TAG, "${atomException.message} : ${atomException.code}")
            }
        })

        val manager = atom
        if (manager != null &&
            manager.getCurrentVpnStatus(requireContext())
                .equals(AtomManager.VPNStatus.CONNECTED, ignoreCase = true)
        ) {
            changeButtonState(btnConnect, "Disconnect")
        }
    }

    /**
     * Builds a spinner adapter over [protocols], optionally prefixed with a placeholder entry
     * named [dummyName]. Replaces three near-identical blocks in the Java original.
     */
    private fun protocolAdapterFor(
        context: Context,
        protocols: List<Protocol>,
        dummyName: String?
    ): ProtocolAdapter {
        val items = if (dummyName == null) {
            protocols
        } else {
            listOf(Protocol().apply { name = dummyName }) + protocols
        }
        val adapter = ProtocolAdapter(
            context, android.R.layout.simple_spinner_item, items.toTypedArray()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    /**
     * Channels supporting every selected protocol.
     *
     * Collapses the Java original's four-branch cascade: `containsAll` of a one-element list is
     * the same as `contains`, so the branches only differed in which protocols were non-null.
     * A null [primary] still yields null (leaving the spinner untouched), as before.
     */
    private fun channelsByAllSelectedProtocol(
        channels: List<Channel>?,
        primary: Protocol?,
        secondary: Protocol?,
        tertiary: Protocol?
    ): List<Channel>? {
        if (channels == null || primary == null) return null
        val wanted = listOfNotNull(primary, secondary, tertiary)
        return channels.filter { it.protocols?.containsAll(wanted) == true }
    }

    private fun displayChannels(channels: List<Channel>?) {
        if (channels == null) return
        val ctx = activity ?: return
        val channelAdapter = ChannelAdapter(
            ctx, android.R.layout.simple_spinner_item, channels.toTypedArray()
        )
        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        channelSpinner.adapter = channelAdapter
        channelAdapter.notifyDataSetChanged()
    }

    // Legacy overload, deprecated by the SDK in favour of onConnected(ConnectionDetails).
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onConnected() {
    }

    override fun onConnected(connectionDetails: ConnectionDetails) {
        Log.d(TAG, "Connected")
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
            Log.d(TAG, atomException.message)
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
        Log.e(TAG, state)
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
        private const val TAG = "ConnectWithChannelFragment"
    }
}
