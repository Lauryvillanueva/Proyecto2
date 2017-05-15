package com.uninorte.proyecto2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by LauryV on 14/05/2017.
 */

public class SpinnerAdapterReco extends ArrayAdapter<RecowithKey> {

    private Context context;

    private List<RecowithKey> recorridoLists;

    public SpinnerAdapterReco(Context context, int textViewResourceId, List<RecowithKey> recorridoLists) {
        super(context, textViewResourceId, recorridoLists);
        this.context = context;
        this.recorridoLists = recorridoLists;
    }

    public int getCount(){
        return recorridoLists.size();
    }

    public RecowithKey getItem(int position){
        return recorridoLists.get(position);
    }

    public long getItemId(int position){
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) View.inflate(context, android.R.layout.simple_spinner_item, null);
        textView.setText(recorridoLists.get(position).getRecorrido().getTimeI());
        return textView;


    }


    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        ((TextView) convertView).setText(recorridoLists.get(position).getRecorrido().getTimeI());
        return convertView;

    }
}
