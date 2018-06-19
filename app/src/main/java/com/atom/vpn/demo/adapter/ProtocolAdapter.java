/*
 * Copyright (c) 2018 Atom SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.atom.sdk.android.data.model.protocol.Protocol;




public class ProtocolAdapter extends ArrayAdapter<Protocol> {

    private Protocol[] protocols;

    public ProtocolAdapter(Context context, int textViewResourceId,
                           Protocol[] protocols) {
        super(context, textViewResourceId, protocols);
        this.protocols = protocols;
    }

    @Override
    public int getCount(){
        return protocols.length;
    }

    @Override
    public Protocol getItem(int position){
        return protocols[position];
    }

    @Override
    public long getItemId(int position){
        return position;
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(protocols[position].getName());
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(protocols[position].getName());

        return label;
    }
}