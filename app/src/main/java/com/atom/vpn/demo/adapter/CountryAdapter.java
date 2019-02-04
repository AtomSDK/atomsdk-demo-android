/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 */

package com.atom.vpn.demo.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.atom.sdk.android.data.model.countries.Country;


public class CountryAdapter extends ArrayAdapter<Country> {


    private Country[] countries;

    public CountryAdapter(Context context, int textViewResourceId,
                          Country[] countries) {
        super(context, textViewResourceId, countries);
        this.countries = countries;
    }

    @Override
    public int getCount(){
        if(countries!=null){
        return countries.length;
        }
        return 0;
    }

    @Override
    public Country getItem(int position){
        return countries[position];
    }

    @Override
    public long getItemId(int position){
        return position;
    }



    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(countries[position].getName());
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(countries[position].getName());

        return label;
    }
}