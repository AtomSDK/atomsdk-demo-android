package com.atom.vpn.demo.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.atom.core.models.City

class CityAdapter(
    context: Context,
    textViewResourceId: Int,
    private val cities: Array<City>
) : ArrayAdapter<City>(context, textViewResourceId, cities) {

    override fun getCount(): Int = cities.size

    override fun getItem(position: Int): City = cities[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getView(position, convertView, parent) as TextView
        label.setTextColor(Color.BLACK)
        label.text = cities[position].name
        return label
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getDropDownView(position, convertView, parent) as TextView
        label.setTextColor(Color.BLACK)
        label.text = cities[position].name
        return label
    }
}
