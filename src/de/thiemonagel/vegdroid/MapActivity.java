package de.thiemonagel.vegdroid;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;


import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Activity to display venues as markers on a zoom- and moveable map.
 *
 * To receive tap events a map of (Marker.getId() --> venueId) is required.
 * To filter venues, a mapping of Marker <--> venueId is required.
 *
 */
public class MapActivity extends SherlockFragmentActivity {
    public volatile GoogleMap           map;
    private SupportMapFragment          fMap;

    private Map<Marker,Integer> markers = new HashMap<Marker,Integer>();  // (Marker --> venueId)
    private Map<Integer,Marker> venues  = new HashMap<Integer,Marker>();  // (venueId --> Marker)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));

        if (savedInstanceState == null ) {
            //first time fragment is set
            fMap.setRetainInstance(true);
        } else {
            //previous map keeps from reinitializing every time.
            map = fMap.getMap();
        }

        // Work around pre-Froyo bugs in HTTP connection reuse.
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO ) {
            System.setProperty("http.keepAlive", "false");
        }

        Global.getInstance(this).mapActivity = this;
        MyData.initInstance(this);
        setupMapIfNeeded();

        new LoadStream(this)
                .execute( MyData.getInstance().getLocation() );
    }

    @Override
    protected void onDestroy() {
        Global.getInstance(this).mapActivity = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.activity_map, menu);
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
                intent = new Intent(this, EntryListActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupMapIfNeeded() {
        if ( map == null ) {
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        }
        if ( map != null) {
            setupMap();
        }
    }

        private void setupMap() {
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
                int VenueId = markers.get( m );
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

    public synchronized void addMarker( Context context, int vId, LatLng ll ) {
        if ( map == null ) return;

        Venue v = Global.getInstance(context).venues.get(vId);
        if ( v == null || v.filtered(context) ) return;

        String name = Global.getInstance(context).venues.get(vId).name;
        String desc = Global.getInstance(context).venues.get(vId).shortDescription;
        MarkerOptions mo = new MarkerOptions()
                .position(ll)
                .title(name)
                .snippet(desc);
        Marker m = map.addMarker( mo );
        if ( m == null ) return;

        markers.put( m, vId );
        venues .put( vId, m );
    }

    public synchronized void updateFilter( Context context ) {
        for ( Iterator<Map.Entry<Marker, Integer>> it = markers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Marker, Integer> entry = it.next();
            Marker  m   = entry.getKey();
            Integer vId = entry.getValue();
            Venue   v   = Global.getInstance(context).venues.get(vId);
            if ( v == null || v.filtered(context) ) {
                venues.remove(vId);  // remove from MapActivity.venues map
                m.remove();          // remove from map
                it.remove();         // remove from MapActivity.markes map
            }
        }
        for ( Map.Entry<Integer,Venue> entry : Global.getInstance(context).venues.entrySet() ) {
            if ( venues.containsKey(entry.getKey()) ) continue;
            int vId = entry.getKey();
            Venue v = Global.getInstance(context).venues.get(vId);
            if ( v == null || v.filtered(context) ) continue;
            Global.getInstance(context).CGC.Resolve(context, v);
        }
    }
}
