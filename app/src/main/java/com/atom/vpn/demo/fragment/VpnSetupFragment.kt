/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.atom.sdk.android.AtomManager
import com.atom.vpn.demo.R
import com.atom.vpn.demo.common.UIHelper
import com.atom.vpn.demo.common.logger.Log

class VpnSetupFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup_vpn, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnGivePermission = view.findViewById<Button>(R.id.btnGivePermission)

        btnGivePermission.setOnClickListener {
            val manager = AtomManager.getInstance()
            if (manager != null) {
                // The SDK only accepts an Activity or a Fragment here and reports the outcome
                // through onActivityResult below; there is no ActivityResultLauncher overload.
                manager.getVPNServicePermission(this)
            } else {
                Log.e(TAG, "Atom Manager is null")
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AtomManager.REQUEST_VPN_PERMISSION) {
            if (resultCode == AtomManager.RESULT_VPN_PERMISSION_APPROVED) {
                Log.e(TAG, "VPN API Permission provided")
                openMainFragment()
            } else if (resultCode == AtomManager.RESULT_VPN_PERMISSION_CANCELED) {
                Log.e(TAG, "VPN API Permission cancelled")
                UIHelper.showAlertDialog(
                    requireContext(),
                    "Please provide VPN permission to use this application"
                )
            }
        }
    }

    private fun openMainFragment() {
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.popBackStack()

        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.sample_content_fragment, MainFragment())
        transaction.commit()
    }

    companion object {
        private const val TAG = "VpnSetupFragment"
    }
}
