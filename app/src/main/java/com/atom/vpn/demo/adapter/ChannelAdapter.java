package com.atom.vpn.demo.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.atom.core.models.Channel;

public class ChannelAdapter extends ArrayAdapter<Channel> {


    private Channel[] channels;

    public ChannelAdapter(Context context, int textViewResourceId,
                          Channel[] channels) {
        super(context, textViewResourceId, channels);
        this.channels = channels;
    }

    @Override
    public int getCount(){
        if(channels!=null){
            return channels.length;
        }
        return 0;
    }

    @Override
    public Channel getItem(int position){
        return channels[position];
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(channels[position].getName());
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(channels[position].getName());

        return label;
    }
}