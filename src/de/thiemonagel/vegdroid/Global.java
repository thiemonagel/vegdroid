package de.thiemonagel.vegdroid;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.stream.JsonReader;

public class Global {

    // public
    public static final String          LOG_TAG = "VegDroid";

    public volatile MapActivity         mapActivity;
    public volatile DisplayListActivity listActivity;
    public volatile Map<Integer,Venue>  venues = Collections.synchronizedMap( new TreeMap<Integer,Venue>() );

    // private
    private static final String         PREFS_CATMASK = "CategoryMask";
    private static final String         PREFS_FILE    = "config";

    private volatile static Global      sInstance;
    private SharedPreferences           mSettings;
    private int                         mCatFilterMask;
    private int                         mCatFilterMaskApplied;  // last mask committed to storage

    private Global( Context c ) {

        // load from SharedPreferences
        mSettings             = c.getSharedPreferences( PREFS_FILE, Context.MODE_PRIVATE );
        mCatFilterMask        = mSettings.getInt( PREFS_CATMASK, -1 );
        mCatFilterMaskApplied = mCatFilterMask;
        Log.d( Global.LOG_TAG, "Read CatFilterMask: " + mCatFilterMask );
    }

    // Obtain instance, constructing it if necessary.
    // Double-checked locking is ok with Java 5 or later because writing to volatile fInstance is atomic,
    // cf. http://www.javamex.com/tutorials/double_checked_locking_fixing.shtml
    public static Global getInstance( Context c ) {
        if ( sInstance == null ) {
            synchronized (Global.class) {
                if ( sInstance == null ) {
                    sInstance = new Global( c );
                }
            }
        }
        return sInstance;
    }
}


class LoadGeoCode extends AsyncTask<String, Void, MarkerOptions> {
    private volatile static int sGeoCount = 0;

    private          Context  mContext;
    private          Geocoder mGC;
    private volatile int      mVenueId;

    public LoadGeoCode( Context context ) {
        mContext = context.getApplicationContext();
        mGC      = new Geocoder( context );
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
                count = sGeoCount;
                sGeoCount++;
            }
            Log.d( Global.LOG_TAG, "geocode " + count + ": " + ms + " ms" );

            if ( la.size() > 0 ) {
                Address a = la.get(0);
                return new MarkerOptions()
                        .position( new LatLng(a.getLatitude(), a.getLongitude()) )
                        .title(name)
                        .snippet(desc);
            }
        } catch (IOException e) {
            Log.e( Global.LOG_TAG, "geocode #" + count + " io exception" );
        }

        return null;
    }

    @Override
    protected void onPostExecute( MarkerOptions mo ) {
        if ( mo == null ) return;  // catch network errors, etc.

        MapActivity ma = Global.getInstance(mContext).mapActivity;
        Map<String,Integer> markerMap = null;
        GoogleMap           googleMap = null;
        if ( ma != null ) {
            markerMap = ma.markers;
            googleMap = ma.map;
        }
        Marker m = null;
        if ( googleMap != null ) {
            m = googleMap.addMarker( mo );
        }
        if ( markerMap != null && m != null ) {
            markerMap.put( m.getId(), mVenueId );
        }
    }
}  // class LoadGeoCode


class LoadStream extends AsyncTask<LatLng, Void, Void> {
    private Context mContext;

    public LoadStream( Context context ) {
        mContext = context.getApplicationContext();
    }

    @Override
    protected Void doInBackground(LatLng... llarray) {
        assert( llarray.length == 1 );

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
            Log.d( Global.LOG_TAG, "request: " + urlstring );
            URL url = new URL( urlstring );
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty( "User-Agent", R.string.app_name + " " + R.string.version_string );
            Log.d( Global.LOG_TAG, "content length: " + conn.getContentLength() );
            if ( conn.getContentLength() < 500 ) {
                BufferedReader r = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
                String line;
                while ( (line = r.readLine()) != null ) {
                    Log.d( Global.LOG_TAG, "-- " + line );
                }
                return null;
            }
            try {
                InputStream bin = new BufferedInputStream( conn.getInputStream() );
                reader = new JsonReader( new InputStreamReader( bin ) );
                reader.beginObject();
                while ( reader.hasNext() ) {
                    String item = reader.nextName();
                    //Log.v( Global.LOG_TAG, "JSON: " + item );
                    if ( !item.equals("entries") ) {
                        reader.skipValue();
                    } else {
                        reader.beginArray();
                        //int ecount = -1;
                        while ( reader.hasNext() ) {
                            // read entry
                            //ecount++;
                            //Log.v( Global.LOG_TAG, "JSON:     #" + ecount );
                            Venue v = new Venue();
                            v.parseJson(reader, mContext);

                            // write to global storage, skip if id doesn't exist
                            // TODO: this may lead to missed updates in case the venue already does exist
                            try {
                                Global.getInstance(mContext).venues.put( v.getId(), v );
                            } catch ( IllegalStateException e ) {
                                Log.e( Global.LOG_TAG, "JSON:     getId() error" );
                                continue;
                            }

                            // skip closed venues
                            if ( v.closed() ) continue;

                            // work around the fact that by default, multiple AsyncTasks are run
                            // *sequentially* on honeycomb or later
                            LoadGeoCode lgc = new LoadGeoCode( mContext );
                            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
                                Log.d( Global.LOG_TAG, "Queue LoadGeoCode in executor." );
                                lgc.executeOnExecutor( exec, v.locString(), v.name, v.shortDescription, String.valueOf( v.getId() ) );
                            } else {
                                Log.d( Global.LOG_TAG, "Queue LoadGeoCode." );
                                lgc.execute( v.locString(), v.name, v.shortDescription, String.valueOf( v.getId() ) );
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
            Log.e( Global.LOG_TAG, "malformed url" );
        } catch (IOException e) {
            Log.e( Global.LOG_TAG, "io exception" );
        } finally {
            if ( conn != null ) conn.disconnect();
            try {
                if ( reader != null ) reader.close();
            } catch (IOException e) {
                Log.e( Global.LOG_TAG, "io exception on reader close" );
            }
            if ( exec != null ) exec.shutdown();
        }
        return null;
    }

}  // class LoadStream


