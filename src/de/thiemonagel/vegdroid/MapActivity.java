package de.thiemonagel.vegdroid;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends android.support.v4.app.FragmentActivity {
    private static final String                      LOG_TAG       = "VegDroid";

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

            Date gstart = new Date();
            Geocoder gc = new Geocoder(this);
            int N = 100;
            for ( int i=1; i<=N; i++ )
                try {
                    String loc = "Leopoldstr. " + i + ", München";

                    Date start = new Date();
                    List<Address> la = gc.getFromLocationName(loc, 1);
                    Date end = new Date();
                    float ms = (end.getTime()-start.getTime());
                    Log.d( LOG_TAG, "geocode " + i + ": " + ms + " ms" );

                    if ( la.size() > 0 ) {
                        Address a = la.get(0);
                        fMap.addMarker( new MarkerOptions()
                                .position( new LatLng(a.getLatitude(), a.getLongitude()) )
                                .title("Title")
                                .snippet(loc)
                        );
                    }
                } catch (IOException e) {};

                Date end = new Date();
                float ms = (end.getTime()-gstart.getTime());
                Log.d( LOG_TAG, "avg time per geocode: " + ms/N + " ms" );



            /*
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
                fMap.addMarker( new MarkerOptions()
                        .position( new LatLng(lat.get(i), lon.get(i)) )
                        .title("Title "+i)
                        .snippet("Text "+i)
                        );
            }
            */

        }
    }
}
