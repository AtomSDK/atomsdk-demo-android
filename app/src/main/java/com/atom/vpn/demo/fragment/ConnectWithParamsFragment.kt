/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.Fragment
import com.atom.core.exceptions.AtomAPIException
import com.atom.core.exceptions.AtomException
import com.atom.core.exceptions.AtomValidationException
import com.atom.core.models.AtomConfiguration
import com.atom.core.models.City
import com.atom.core.models.Country
import com.atom.core.models.Protocol
import com.atom.sdk.android.AtomManager
import com.atom.sdk.android.ConnectionDetails
import com.atom.sdk.android.Errors
import com.atom.sdk.android.VPNCredentials
import com.atom.sdk.android.VPNProperties
import com.atom.sdk.android.VPNStateListener
import com.atom.sdk.android.common.Utils
import com.atom.sdk.android.data.callbacks.CollectionCallback
import com.atom.vpn.demo.R
import com.atom.vpn.demo.activity.ConnectActivity
import com.atom.vpn.demo.adapter.CityAdapter
import com.atom.vpn.demo.adapter.CountryAdapter
import com.atom.vpn.demo.adapter.ProtocolAdapter
import com.atom.vpn.demo.atom
import com.atom.vpn.demo.common.Constants
import com.atom.vpn.demo.common.Utilities
import com.atom.vpn.demo.common.Utilities.changeButtonState
import com.atom.vpn.demo.common.Utilities.changeButtonText
import com.atom.vpn.demo.common.logger.Log
import com.tooltip.Tooltip

class ConnectWithParamsFragment : Fragment(), VPNStateListener {

    private lateinit var primaryProtocolSpinner: AppCompatSpinner
    private lateinit var secondaryProtocolSpinner: AppCompatSpinner
    private lateinit var tertiaryProtocolSpinner: AppCompatSpinner
    private lateinit var countrySpinner: AppCompatSpinner
    private lateinit var citySpinner: AppCompatSpinner

    private var protocolList: List<Protocol>? = null
    private var countriesList: List<Country>? = null
    private var citiesList: List<City>? = null

    private var countriesForSmartDialing: List<Country>? = null

    private lateinit var switchOptimizedConnection: Switch
    private lateinit var switchSmartDialing: Switch

    private var btnConnect: Button? = null
    private var btnPause: Button? = null

    private var uuid: String? = null
    private var vpnUsername: String? = null
    private var vpnPassword: String? = null

    private var primaryProtocol: Protocol? = null
    private var secondaryProtocol: Protocol? = null
    private var tertiaryProtocol: Protocol? = null

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
        return inflater.inflate(R.layout.fragment_connect_with_params, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnPause = view.findViewById(R.id.btnPause)
        togglePauseBtn(false)

        val smartDialingHint = view.findViewById<ImageView>(R.id.smartDialingHint)
        val smartCountriesHintTip = Tooltip.Builder(smartDialingHint, R.style.TooltipStyle)
            .setText(Constants.TooltipSmartDialing)
            .setDismissOnClick(true)
            .build()
        smartDialingHint.setOnClickListener {
            if (!smartCountriesHintTip.isShowing) {
                smartCountriesHintTip.show()
                Handler(Looper.getMainLooper()).postDelayed(
                    { smartCountriesHintTip.dismiss() },
                    3000
                )
            } else {
                smartCountriesHintTip.dismiss()
            }
        }

        val optConnectionHint = view.findViewById<ImageView>(R.id.optConnectionHint)
        val optConnectionHintTip = Tooltip.Builder(optConnectionHint, R.style.TooltipStyle)
            .setText(Constants.TooltipOptimization)
            .setDismissOnClick(true)
            .build()

        optConnectionHint.setOnClickListener {
            if (!optConnectionHintTip.isShowing) {
                optConnectionHintTip.show()
                Handler(Looper.getMainLooper()).postDelayed(
                    { optConnectionHintTip.dismiss() },
                    3000
                )
            } else {
                optConnectionHintTip.dismiss()
            }
        }

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

                    val source =
                        if (switchSmartDialing.isChecked) countriesForSmartDialing else countriesList
                    displayCountries(
                        countriesByAllSelectedProtocol(
                            source, primaryProtocol, secondaryProtocol, tertiaryProtocol
                        )
                    )

                    refreshCities(primaryProtocol)
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

                        val source =
                            if (switchSmartDialing.isChecked) countriesForSmartDialing else countriesList
                        displayCountries(
                            countriesByAllSelectedProtocol(
                                source, selectedPrimary, secondaryProtocol, tertiaryProtocol
                            )
                        )
                    } else {
                        secondaryProtocol = null

                        val source =
                            if (switchSmartDialing.isChecked) countriesForSmartDialing else countriesList
                        displayCountries(
                            countriesByAllSelectedProtocol(
                                source, primaryProtocol, secondaryProtocol, tertiaryProtocol
                            )
                        )
                    }

                    refreshCities(primaryProtocol)
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

                        val source =
                            if (switchSmartDialing.isChecked) countriesForSmartDialing else countriesList
                        displayCountries(
                            countriesByAllSelectedProtocol(
                                source, selectedPrimary, secondaryProtocol, tertiaryProtocol
                            )
                        )
                    } else {
                        tertiaryProtocol = null

                        val source =
                            if (switchSmartDialing.isChecked) countriesForSmartDialing else countriesList
                        displayCountries(
                            countriesByAllSelectedProtocol(
                                source, primaryProtocol, secondaryProtocol, tertiaryProtocol
                            )
                        )
                    }

                    refreshCities(primaryProtocol)
                }

                override fun onNothingSelected(adapter: AdapterView<*>?) {
                }
            }

        countrySpinner = view.findViewById(R.id.countrySpinner)

        citySpinner = view.findViewById(R.id.citySpinner)

        switchOptimizedConnection = view.findViewById(R.id.switchOptConnection)
        switchOptimizedConnection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                switchSmartDialing.isChecked = false
                displayCountries(
                    countriesByAllSelectedProtocol(
                        countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol
                    )
                )
            }
        }

        switchSmartDialing = view.findViewById(R.id.switchSmartDialing)
        switchSmartDialing.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                switchOptimizedConnection.isChecked = false
                atom?.getCountriesForSmartDialing(object : CollectionCallback<Country> {
                    override fun onSuccess(list: List<Country>?) {
                        if (list != null) {
                            countriesForSmartDialing = list
                            displayCountries(
                                countriesByAllSelectedProtocol(
                                    list, primaryProtocol, secondaryProtocol, tertiaryProtocol
                                )
                            )
                        }
                    }

                    override fun onError(exception: AtomException) {
                    }

                    override fun onNetworkError(exception: AtomException) {
                    }
                })
            } else {
                displayCountries(
                    countriesByAllSelectedProtocol(
                        countriesList, primaryProtocol, secondaryProtocol, tertiaryProtocol
                    )
                )
            }
        }

        countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                refreshCities(primaryProtocol)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
            }
        }

        btnPause?.setOnClickListener {
            val manager = atom ?: return@setOnClickListener
            val vpnStatus = manager.getCurrentVpnStatus(requireContext())
            if (AtomManager.VPNStatus.PAUSED.equals(vpnStatus, ignoreCase = true)) {
                manager.resume()
            } else {
                Utilities.getPauseTimerList(requireActivity()) { timer ->
                    if (timer != null) {
                        manager.pause(timer)
                    }
                }
            }
        }

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

                    val country = countrySpinner.selectedItem as? Country

                    val city = if (citySpinner.selectedItemPosition > 0) {
                        citySpinner.selectedItem as? City
                    } else {
                        null
                    }

                    if (selectedPrimary != null && (country != null || city != null)) {

                        btnConnect?.text = "Cancel"

                        (activity as? ConnectActivity)?.logWrapper?.clear()

                        try {
                            // Dedicated Host here
                            val vpnPropertiesBuilder = if (city != null) {
                                VPNProperties.Builder(city, selectedPrimary)
                            } else {
                                VPNProperties.Builder(country!!, selectedPrimary)
                            }

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

                            if (switchOptimizedConnection.isChecked) {
                                vpnPropertiesBuilder.withOptimization()
                            }

                            if (switchSmartDialing.isChecked) {
                                vpnPropertiesBuilder.withSmartDialing()
                            }

                            vpnPropertiesBuilder.withAutomaticPort()

                            vpnPropertiesBuilder.withManualPort(5500)

                            val user = vpnUsername
                            val pass = vpnPassword
                            val id = uuid
                            if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                                manager.setVPNCredentials(VPNCredentials(user, pass))
                            } else if (!id.isNullOrEmpty()) {
                                @Suppress("DEPRECATION")
                                manager.setUUID(id)
                            }

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

        // get Countries from ATOM SDK
        atom?.getCountries(object : CollectionCallback<Country> {
            override fun onSuccess(countries: List<Country>?) {
                if (countries != null) {
                    countriesList = countries

                    displayCountries(
                        countriesByAllSelectedProtocol(
                            countries, primaryProtocol, secondaryProtocol, tertiaryProtocol
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

        // get Cities from ATOM SDK
        atom?.getCities(object : CollectionCallback<City> {
            override fun onSuccess(cities: List<City>?) {
                if (cities != null) {
                    citiesList = cities
                }
            }

            override fun onError(atomException: AtomException) {
                Log.d(TAG, "${atomException.message} : ${atomException.code}")
            }

            override fun onNetworkError(atomException: AtomException) {
                Log.d(TAG, "${atomException.message} : ${atomException.code}")
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

    /** Re-filters the city spinner against the currently selected country, if there is one. */
    private fun refreshCities(primary: Protocol?) {
        val country = countrySpinner.selectedItem as? Country ?: return
        displayCities(
            citiesByAllSelectedProtocolByCountry(
                citiesList, primary, secondaryProtocol, tertiaryProtocol, country
            )
        )
    }

    /**
     * Countries supporting every selected protocol.
     *
     * Collapses the Java original's four-branch cascade: `containsAll` of a one-element list is
     * the same as `contains`, so the branches only differed in which protocols were non-null.
     * A null [primary] still yields null (leaving the spinner untouched), as before.
     */
    private fun countriesByAllSelectedProtocol(
        countries: List<Country>?,
        primary: Protocol?,
        secondary: Protocol?,
        tertiary: Protocol?
    ): List<Country>? {
        if (countries == null || primary == null) return null
        val wanted = listOfNotNull(primary, secondary, tertiary)
        return countries.filter { it.protocols?.containsAll(wanted) == true }
    }

    /**
     * Cities in [country] supporting every selected protocol.
     *
     * City.country is nullable while Country.country is not, so a city with no country is
     * treated as a non-match rather than throwing, which is what the Java original did.
     */
    private fun citiesByAllSelectedProtocolByCountry(
        cities: List<City>?,
        primary: Protocol?,
        secondary: Protocol?,
        tertiary: Protocol?,
        country: Country
    ): List<City>? {
        if (cities == null || primary == null) return null
        val wanted = listOfNotNull(primary, secondary, tertiary)
        return cities.filter {
            it.protocols?.containsAll(wanted) == true &&
                it.country?.equals(country.country, ignoreCase = true) == true
        }
    }

    private fun displayCountries(countries: List<Country>?) {
        if (countries == null) return
        val ctx = activity ?: return
        val countryAdapter = CountryAdapter(
            ctx, android.R.layout.simple_spinner_item, countries.toTypedArray()
        )
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countrySpinner.adapter = countryAdapter
        countryAdapter.notifyDataSetChanged()
    }

    private fun displayCities(cities: List<City>?) {
        if (cities == null) return
        val ctx = activity ?: return

        val dummyCity = City().apply {
            name = "Select City"
            id = 0
        }
        // The Java original mutated the incoming list with add(0, ...); prepending to a new
        // list keeps the same ordering without touching the caller's collection.
        val cityArray = (listOf(dummyCity) + cities).toTypedArray()

        val cityAdapter = CityAdapter(ctx, android.R.layout.simple_spinner_item, cityArray)
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = cityAdapter
        cityAdapter.notifyDataSetChanged()
    }

    private fun togglePauseBtn(enable: Boolean) {
        requireActivity().runOnUiThread { btnPause?.isEnabled = enable }
    }

    // Legacy overload, deprecated by the SDK in favour of onConnected(ConnectionDetails).
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onConnected() {
    }

    override fun onConnected(connectionDetails: ConnectionDetails) {
        Log.d(TAG, "Connected")
        changeButtonState(btnConnect, "Disconnect")

        Utils.objectToString(connectionDetails) { result ->
            Log.e("connection", result)
        }
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

        if (AtomManager.VPNStatus.CONNECTED.equals(state, ignoreCase = true)) {
            changeButtonState(btnPause, "Pause")
            togglePauseBtn(true)
        } else if (AtomManager.VPNStatus.PAUSED.equals(state, ignoreCase = true)) {
            changeButtonState(btnPause, "Resume")
            togglePauseBtn(true)
        } else {
            changeButtonState(btnPause, "Pause")
            togglePauseBtn(false)
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
        private const val TAG = "ConnectWithParamsFragment"
    }
}
