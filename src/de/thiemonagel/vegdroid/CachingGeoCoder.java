package de.thiemonagel.vegdroid;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 *
 * TODO: Implement caching.
 *
 * TODO: At some point this could be further abstracted, avoiding any reference to Venue,
 * making it re-usable in other apps.
 *
 */
public class CachingGeoCoder {

    private ThreadPoolExecutor mExec = null;

    public CachingGeoCoder() {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            mExec = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        }
    }

    void Resolve( Context context, Venue v ) {
        // work around the fact that by default, multiple AsyncTasks are run
        // *sequentially* on honeycomb or later
        LoadGeoCode lgc = new LoadGeoCode( context );
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            Log.d( Global.LOG_TAG, "Queue LoadGeoCode in executor." );
            lgc.executeOnExecutor( mExec, v.locString(), String.valueOf( v.getId() ) );
        } else {
            Log.d( Global.LOG_TAG, "Queue LoadGeoCode." );
            lgc.execute( v.locString(), String.valueOf( v.getId() ) );
        }
    }

}  // class CachingGeoCoder


class LoadGeoCode extends AsyncTask<String, Void, LatLng> {
    private volatile static int sGeoCount = 0;

    private          Context  mContext;
    private          Geocoder mGC;
    private volatile int      mVenueId;

    public LoadGeoCode( Context context ) {
        mContext = context.getApplicationContext();
        mGC      = new Geocoder( context );
    }

    @Override
    protected LatLng doInBackground(String... strings) {
        assert( strings.length == 2 );

        String loc  = strings[0];
        String vid  = strings[1];
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
                return new LatLng(a.getLatitude(), a.getLongitude());
            }
        } catch (IOException e) {
            Log.e( Global.LOG_TAG, "geocode #" + count + " io exception" );
        }

        return null;
    }

    @Override
    protected void onPostExecute( LatLng ll ) {
        if ( ll == null ) return;  // catch network errors, etc.

        MapActivity ma = Global.getInstance(mContext).mapActivity;
        if ( ma == null ) return;

        ma.addMarker( mContext, mVenueId, ll );
    }
}  // class LoadGeoCode
