/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 */

package com.atom.vpn.demo.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.atom.core.models.Country

class CountryAdapter(
    context: Context,
    textViewResourceId: Int,
    private val countries: Array<Country>
) : ArrayAdapter<Country>(context, textViewResourceId, countries) {

    override fun getCount(): Int = countries.size

    override fun getItem(position: Int): Country = countries[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getView(position, convertView, parent) as TextView
        label.setTextColor(Color.BLACK)
        label.text = countries[position].name
        return label
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getDropDownView(position, convertView, parent) as TextView
        label.setTextColor(Color.BLACK)
        label.text = countries[position].name
        return label
    }
}
