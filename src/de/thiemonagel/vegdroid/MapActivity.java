package de.thiemonagel.vegdroid;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.Menu;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends android.support.v4.app.FragmentActivity {
    private GoogleMap fMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        setupMap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_map, menu);
        return true;
    }

    private void setupMap() {
        if ( fMap == null ) {
            fMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            fMap.setMyLocationEnabled(true);
            UiSettings ui = fMap.getUiSettings();
            ui.setCompassEnabled(true);
            ui.setMyLocationButtonEnabled(true);
            ui.setRotateGesturesEnabled(true);
            ui.setScrollGesturesEnabled(true);
            ui.setTiltGesturesEnabled(true);
            ui.setZoomControlsEnabled(true);
            ui.setZoomGesturesEnabled(true);

            ArrayList<Float> lat = new ArrayList<Float>();
            ArrayList<Float> lon = new ArrayList<Float>();
            int N=1000;
            for ( int i=0; i<N; i++ ) {
                float la = 48.139126f;
                float lo = 11.580186f;
                for ( int j=0; j<20; j++ ) {
                    la += (Math.random()-0.5)/30;
                    lo += (Math.random()-0.5)/30;
                }
                lat.add(la);
                lon.add(lo);
            }

            for ( int i=0; i<N; i++ ) {
                fMap.addMarker(new MarkerOptions()
                        .position( new LatLng(lat.get(i), lon.get(i)) )
                        .title("Title "+i)
                        .snippet("Text "+i)
                        );
            }

        }
    }
}
