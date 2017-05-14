package com.uninorte.proyecto2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback {


    protected static final String TAG = MapsActivity.class.getSimpleName();

    protected Boolean mRequestingLocationUpdates;

    private String vendedorID;
    private String recorridoID;

    private LatLng mCurrentLocation;

    private LatLng mPreviousLocation;


    private ArrayList<LatLng> mLocationsList;

    private FirebaseAuth auth;
    private DatabaseReference mDatabaseRef;


    private Activity mAct;
    private GoogleMap mMap;


    private void updateMap() {
        if (mPreviousLocation != null) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(mCurrentLocation)
                    .add(mPreviousLocation)
                    .color(Color.RED)
                    .width(5);
            mMap.addPolyline(polylineOptions);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, 21f));

    }

    private void updateMap(ArrayList<LatLng> loc) {

        LatLng current;
        LatLng previous;
        for (int i = 0; i < loc.size() - 1; i++) {
            current = loc.get(i);
            previous = loc.get(i + 1);

            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(current)
                    .add(previous)
                    .color(Color.RED)
                    .width(5);

            mMap.addPolyline(polylineOptions);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 21f));
        }


    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*vendedorID=getIntent().getStringExtra("VendedorID");
        recorridoID=getIntent().getStringExtra("RecorridoID");*/

        auth = FirebaseAuth.getInstance();

        vendedorID=auth.getCurrentUser().getUid();
        recorridoID="-Kk6wM33bZTJVfcGjSk_";
        mDatabaseRef=FirebaseDatabase.getInstance().getReference("tracks").child(recorridoID);
        mDatabaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Track track= dataSnapshot.getValue(Track.class);
                LatLng latLng=new LatLng(Double.valueOf(track.getLat()),Double.valueOf(track.getLon()));
                Log.d(TAG, "onChildAdded: "+latLng.toString());
                //mLocationsList.add(latLng);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mAct = this;
        mRequestingLocationUpdates = true;


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setZoomControlsEnabled(true);

        if (mLocationsList != null) {
            updateMap(mLocationsList);
        }
    }


}