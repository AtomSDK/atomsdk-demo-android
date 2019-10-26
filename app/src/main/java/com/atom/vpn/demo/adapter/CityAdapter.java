package com.atom.vpn.demo.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.atom.core.models.City;


public class CityAdapter extends ArrayAdapter<City> {

    private City[] cities;

    public CityAdapter(Context context, int textViewResourceId,
                          City[] cities) {
        super(context, textViewResourceId, cities);
        this.cities = cities;
    }

    @Override
    public int getCount(){
        if(cities!=null){
            return cities.length;
        }
        return 0;
    }

    @Override
    public City getItem(int position){
        return cities[position];
    }

    @Override
    public long getItemId(int position){
        return position;
    }



    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(cities[position].getName());
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(cities[position].getName());

        return label;
    }
}