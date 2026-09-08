/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.atom.core.models.Protocol

class ProtocolAdapter(
    context: Context,
    textViewResourceId: Int,
    private val protocols: Array<Protocol>
) : ArrayAdapter<Protocol>(context, textViewResourceId, protocols) {

    override fun getCount(): Int = protocols.size

    override fun getItem(position: Int): Protocol = protocols[position]

    override fun getItemId(position: Int): Long = position.toLong()

    /** The "passive" state of the spinner. */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // A dynamic TextView is used here, but a custom layout could be referenced instead.
        val label = super.getView(position, convertView, parent) as TextView
        label.setTextColor(Color.BLACK)
        label.text = protocols[position].name
        return label
    }

    /** Shown when the "chooser" is popped up. Normally the same view, but customisable. */
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getDropDownView(position, convertView, parent) as TextView
        label.setTextColor(Color.BLACK)
        label.text = protocols[position].name
        return label
    }
}
