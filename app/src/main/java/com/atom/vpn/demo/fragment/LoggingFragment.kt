/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.atom.vpn.demo.R

/**
 * NOTE: this fragment is currently unreferenced - nothing instantiates it and no layout names
 * it (the layouts embed [com.atom.vpn.demo.common.logger.LogFragment] instead).
 */
class LoggingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_logging, container, false)
    }
}
