/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.atom.vpn.demo.R
import com.atom.vpn.demo.activity.ConnectActivity
import com.atom.vpn.demo.atom
import com.atom.vpn.demo.common.Constants
import com.atom.vpn.demo.common.Utilities.setViewAndChildrenEnabled
import de.blinkt.openvpn.BuildConfig

class MainFragment : Fragment() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val secretKeyLay = view.findViewById<LinearLayout>(R.id.secretKeyLay)
        Handler(Looper.getMainLooper()).postDelayed({ secretKeyLay.alpha = 0.5f }, 500)

        val secretKey = view.findViewById<TextView>(R.id.secretKey)
        secretKey.text = getString(R.string.atom_secret_key).ifEmpty {
            getString(R.string.empty_secret_key)
        }

        view.findViewById<TextView>(R.id.sdkVersion).text = "Version: ${BuildConfig.SDKVersion}"

        val usernameLay = view.findViewById<LinearLayout>(R.id.usernameLay)
        val passwordLay = view.findViewById<LinearLayout>(R.id.passwordLay)

        setViewAndChildrenEnabled(usernameLay, true)
        setViewAndChildrenEnabled(passwordLay, true)

        etUsername = view.findViewById(R.id.etUsername)
        etPassword = view.findViewById(R.id.etPassword)

        // Connect with Params
        view.findViewById<View>(R.id.btnConnectParams).setOnClickListener {
            launchActivityForConnectionMethod(1)
        }

        // Connect with Dedicated IP
        view.findViewById<View>(R.id.btnConnectDedicatedIp).setOnClickListener {
            launchActivityForConnectionMethod(2)
        }

        // Connect with Channel
        view.findViewById<View>(R.id.btnConnectChannel).setOnClickListener {
            launchActivityForConnectionMethod(3)
        }
    }

    private fun launchActivityForConnectionMethod(connectionMethodType: Int) {
        if (etUsername.text.toString().trim().isEmpty()) {
            etUsername.error = Constants.UsernameRequired
        } else if (etPassword.text.toString().trim().isEmpty()) {
            etUsername.error = null
            etPassword.error = Constants.PasswordRequired
        } else {

            etUsername.error = null
            etPassword.error = null

            if (atom != null) {

                val intent = Intent(requireContext(), ConnectActivity::class.java)
                intent.putExtra("connection_type", connectionMethodType)
                intent.putExtra("vpnUsername", etUsername.text.toString())
                intent.putExtra("vpnPassword", etPassword.text.toString())
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Atom Manager is not initialized",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
