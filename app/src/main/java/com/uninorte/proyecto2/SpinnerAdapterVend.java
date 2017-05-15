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

public class SpinnerAdapterVend extends ArrayAdapter<Vendedor> {

    private Context context;

    private List<Vendedor> vendedorLists;

    public SpinnerAdapterVend(Context context, int textViewResourceId, List<Vendedor> vendedorLists) {
        super(context, textViewResourceId, vendedorLists);
        this.context = context;
        this.vendedorLists = vendedorLists;
    }

    public int getCount(){
        return vendedorLists.size();
    }

    public Vendedor getItem(int position){
        return vendedorLists.get(position);
    }

    public long getItemId(int position){
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) View.inflate(context, android.R.layout.simple_spinner_item, null);
        textView.setText(vendedorLists.get(position).getKey());
        return textView;


    }


    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        ((TextView) convertView).setText(vendedorLists.get(position).getKey());
        return convertView;

    }
}
