/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atom.vpn.demo.common.logger

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.Fragment

/**
 * Simple fragment which contains a LogView and uses is to output log data it receives
 * through the LogNode interface.
 *
 * This class is referenced by name from the layout XML, so its package and name must not change.
 */
class LogFragment : Fragment() {

    var logView: LogView? = null
        private set

    private var scrollView: ScrollView? = null

    fun inflateViews(): View {
        val scroll = ScrollView(requireActivity())
        val scrollParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        scroll.layoutParams = scrollParams

        val log = LogView(requireActivity())
        val logParams = ViewGroup.LayoutParams(scrollParams)
        logParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        log.layoutParams = logParams
        log.isClickable = true
        log.isFocusable = true
        log.typeface = Typeface.MONOSPACE
        log.setPadding(16, 16, 16, 16)

        // Want to set padding as 16 dips, setPadding takes pixels.  Hooray math!
        val paddingDips = 16
        val scale = resources.displayMetrics.density.toDouble()
        val paddingPixels = ((paddingDips * scale) + .5).toInt()
        log.setPadding(paddingPixels, paddingPixels, paddingPixels, paddingPixels)
        log.compoundDrawablePadding = paddingPixels

        log.gravity = Gravity.BOTTOM
        @Suppress("DEPRECATION")
        log.setTextAppearance(requireActivity(), android.R.style.TextAppearance_Holo_Medium)

        scroll.addView(log)

        scrollView = scroll
        logView = log
        return scroll
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val result = inflateViews()

        logView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                scrollView?.fullScroll(ScrollView.FOCUS_DOWN)
            }
        })
        return result
    }
}
