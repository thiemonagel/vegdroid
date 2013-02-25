package de.thiemonagel.vegdroid;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.Marker;

/**
 * Activity to display venues as markers on a zoom- and moveable map.
 *
 * To receive tap events a map of (Marker.getId() --> venueId) is required.
 *
 */
public class MapActivity extends android.support.v4.app.FragmentActivity {
    public volatile GoogleMap           map;
    public volatile Map<String,Integer> markers = Collections.synchronizedMap( new TreeMap<String,Integer>() );  // (Marker.getId() --> venueId)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Work around pre-Froyo bugs in HTTP connection reuse.
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO ) {
            System.setProperty("http.keepAlive", "false");
        }

        Global.getInstance(this).mapActivity = this;
        MyData.initInstance(this);
        setupMap();

        new LoadStream(this)
                .execute( MyData.getInstance().getLocation() );
    }

    @Override
    protected void onDestroy() {
        Global.getInstance(this).mapActivity = null;
        map = null;  // maybe this helps against leaking bandwidth?
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:  // no idea what this is for
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_filter_cat:
                FilterDialog.CreateDialog(this).show();
                return true;
            case R.id.menu_list:
                intent = new Intent(this, DisplayListActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupMap() {
        if ( map != null )
            return;

        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
        UiSettings ui = map.getUiSettings();
        ui.setCompassEnabled(true);
        ui.setMyLocationButtonEnabled(true);
        ui.setRotateGesturesEnabled(true);
        ui.setScrollGesturesEnabled(true);
        ui.setTiltGesturesEnabled(true);
        ui.setZoomControlsEnabled(true);
        ui.setZoomGesturesEnabled(true);

        map.setOnInfoWindowClickListener( new OnInfoWindowClickListener() {
            public void onInfoWindowClick( Marker m ) {
                int VenueId = markers.get( m.getId() );
                Intent in = new Intent( getApplicationContext(), EntryActivity.class );
                in.putExtra( "VenueId", VenueId );
                startActivity(in);
            }
        });

        if ( MyData.getInstance().UpdateLocation() )
            map.moveCamera( CameraUpdateFactory.newLatLngZoom( MyData.getInstance().getLocation(), 12.f) );

        /*
        Date gstart = new Date();
        Geocoder gc = new Geocoder(this);
        int N = 1;
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
        */



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
