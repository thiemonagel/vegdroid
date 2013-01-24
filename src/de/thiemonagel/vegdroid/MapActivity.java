package de.thiemonagel.vegdroid;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.stream.JsonReader;

public class MapActivity extends android.support.v4.app.FragmentActivity {
    private static final String                      LOG_TAG       = "VegDroid";

    private static int fGeoCount = 0;

    private GoogleMap fMap;
    private Geocoder  fGC;

    private Map<String,Integer> mMarkers = new TreeMap<String,Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Work around pre-Froyo bugs in HTTP connection reuse.
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO ) {
            System.setProperty("http.keepAlive", "false");
        }

        MyData.initInstance(this);
        setupMap();

        new LoadStream(this)
                .execute( MyData.getInstance().getLocation() );
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
            case R.id.menu_list:
                intent = new Intent(this, DisplayListActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class LoadGeoCode extends AsyncTask<String, Void, MarkerOptions> {
        private          Geocoder mGC;
        private volatile int      mVenueId;

        public LoadGeoCode( Context context ) {
            mGC = new Geocoder( context );
        }

        @Override
        protected MarkerOptions doInBackground(String... strings) {
            assert( strings.length == 4 );

            String loc  = strings[0];
            String name = strings[1];
            String desc = strings[2];
            String vid  = strings[3];
            mVenueId = Integer.parseInt(vid);
            int count = -1;
            try {
                Date start = new Date();
                List<Address> la = mGC.getFromLocationName(loc, 1);
                Date end = new Date();
                float ms = (end.getTime()-start.getTime());
                synchronized (LoadGeoCode.class) {
                    count = fGeoCount;
                    fGeoCount++;
                }
                Log.d( LOG_TAG, "geocode " + count + ": " + ms + " ms" );

                if ( la.size() > 0 ) {
                    Address a = la.get(0);
                    return new MarkerOptions()
                            .position( new LatLng(a.getLatitude(), a.getLongitude()) )
                            .title(name)
                            .snippet(desc);
                }
            } catch (IOException e) {
                Log.e( LOG_TAG, "geocode #" + count + " io exception" );
            }

            return null;
        }

        @Override
        protected void onPostExecute( MarkerOptions mo ) {
            if ( mo != null ) {
                Marker m = fMap.addMarker( mo );
                mMarkers.put( m.getId(), mVenueId );
            }
        }
    }  // class LoadGeoCode


    private class LoadStream extends AsyncTask<LatLng, Void, Void> {
        Context fContext;

        public LoadStream( Context context ) {
            fContext = context.getApplicationContext();
        }

        @Override
        protected Void doInBackground(LatLng... llarray) {
            assert( llarray.length == 1 );

            Date now = new Date();

            HttpURLConnection  conn   = null;
            JsonReader         reader = null;
            ThreadPoolExecutor exec   = null;
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
                exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            }
            try {
                LatLng ll = llarray[0];
                String urlstring = "http://www.vegguide.org/search/by-lat-long/";
                urlstring += ll.latitude + "," + ll.longitude;
                urlstring += "?unit=km&distance=200&limit=100";
                Log.d( LOG_TAG, "request: " + urlstring );
                URL url = new URL( urlstring );
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty( "User-Agent", R.string.app_name + " " + R.string.version_string );
                Log.d( LOG_TAG, "content length: " + conn.getContentLength() );
                if ( conn.getContentLength() < 500 ) {
                    BufferedReader r = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
                    String line;
                    while ( (line = r.readLine()) != null ) {
                        Log.d( LOG_TAG, "-- " + line );
                    }
                    return null;
                }
                try {
                    InputStream bin = new BufferedInputStream( conn.getInputStream() );
                    reader = new JsonReader( new InputStreamReader( bin ) );
                    reader.beginObject();
                    while ( reader.hasNext() ) {
                        if ( !reader.nextName().equals("entries") ) {
                            reader.skipValue();
                        } else {
                            reader.beginArray();
                            while ( reader.hasNext() ) {
                                // read entry
                                reader.beginObject();
                                Venue v = new Venue();
                                while ( reader.hasNext() ) {
                                    String item = reader.nextName();
                                    //Log.d( LOG_TAG, "-- " + item );
                                    if ( item.equals("name") ) {
                                        v.name = reader.nextString();
                                    } else if ( item.equals("address1") ) {
                                        v.address1 = reader.nextString();
                                    } else if ( item.equals("address2") ) {
                                        v.address2 = reader.nextString();
                                    } else if ( item.equals("city") ) {
                                        v.city = reader.nextString();
                                    } else if ( item.equals("postal_code") ) {
                                        v.postCode = reader.nextString();
                                    } else if ( item.equals("short_description") ) {
                                        v.shortDescription = reader.nextString();
                                    } else if ( item.equals("close_date") ) {
                                        // TODO: catch IndexOutOfBoundsException???
                                        try {
                                            v.closeDate = Rfc3339.parseDate( reader.nextString() );
                                        } catch ( java.text.ParseException e ) {
                                            v.closeDate = new Date(0);
                                        }
                                    } else if ( item.equals("uri") ) {
                                        v.setId( reader.nextString() );
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();

                                // write to global storage, skip if id doesn't exist
                                // TODO: this may lead to missed updates in case the venue already does exist
                                try {
                                    Global.getInstance().venues.put( v.getId(), v);
                                } catch ( IllegalStateException e ) {
                                    continue;
                                }

                                // work around the fact that by default, multiple AsyncTasks are run
                                // *sequentially* on honeycomb or later
                                LoadGeoCode lgc = new LoadGeoCode( fContext );
                                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
                                    lgc.executeOnExecutor( exec, v.locString(), v.name, v.shortDescription );
                                } else {
                                    lgc.execute( v.locString(), v.name, v.shortDescription );
                                }
                            }
                            reader.endArray();
                        }
                    }
                    reader.endObject();
                } finally {
                    reader.close();
                }
            } catch (MalformedURLException e) {
                Log.e( LOG_TAG, "malformed url" );
            } catch (IOException e) {
                Log.e( LOG_TAG, "io exception" );
            } finally {
                if ( conn != null ) conn.disconnect();
                try {
                    if ( reader != null ) reader.close();
                } catch (IOException e) {
                    Log.e( LOG_TAG, "io exception on reader close" );
                }
                if ( exec != null ) exec.shutdown();
            }
            return null;
        }

    }  // class LoadStream


    private void setupMap() {
        if ( fMap != null )
            return;

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

        fMap.setOnInfoWindowClickListener( new OnInfoWindowClickListener() {
            public void onInfoWindowClick( Marker m ) {
                int VenueId = mMarkers.get( m.getId() );
                Intent in = new Intent( getApplicationContext(), EntryActivity.class );
                in.putExtra( "VenueId", VenueId );
                startActivity(in);
            }
        });

        if ( MyData.getInstance().UpdateLocation() )
            fMap.moveCamera( CameraUpdateFactory.newLatLngZoom( MyData.getInstance().getLocation(), 12.f) );

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
