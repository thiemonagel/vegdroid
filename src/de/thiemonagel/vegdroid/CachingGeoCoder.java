package de.thiemonagel.vegdroid;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 *
 * TODO: Make caching persistent across app restarts.
 *
 * TODO: At some point this could be further abstracted, avoiding any reference to Venue,
 * making it re-usable in other apps.
 *
 */
public class CachingGeoCoder {
    private static final long EXPIRY = 1000l * 3600 * 24 * 29;  // expire after 29 days for compliance with Google Maps API ToS 10.1.3 (b)

    private ThreadPoolExecutor          mExec  = null;
    private Map<String,LatLngTimestamp> mCache = new TreeMap<String,LatLngTimestamp>();  // TreeMap uses less memory, I hope

    public CachingGeoCoder() {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            mExec = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        }
    }

    public synchronized void Resolve( final Context context, final Venue v ) {
        final LatLngTimestamp llt = mCache.get(v.locString());
        if ( llt != null ) {
            if ( llt.expired() ) {
                mCache.remove(v.locString());
            } else {
                Handler guiThread = new Handler( context.getMainLooper() );
                guiThread.post( new Runnable() {
                    public void run() {
                        // warning: duplicated code!
                        MapActivity ma = Global.getInstance(context).mapActivity;
                        if ( ma == null ) return;
                        ma.addMarker( context, v.getId(), llt.ll );
                    }
                });
            }
        }

        // work around the fact that by default, multiple AsyncTasks are run
        // *sequentially* on honeycomb or later
        LoadGeoCode lgc = new LoadGeoCode( context, this );
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            Log.d( Global.LOG_TAG, "Queue LoadGeoCode in executor." );
            lgc.executeOnExecutor( mExec, v.locString(), String.valueOf( v.getId() ) );
        } else {
            Log.d( Global.LOG_TAG, "Queue LoadGeoCode." );
            lgc.execute( v.locString(), String.valueOf( v.getId() ) );
        }
    }

    public synchronized void AddEntry( String s, LatLng ll ) {
        LatLngTimestamp llt = new LatLngTimestamp( ll );
        mCache.put( s, llt );
    }

    private class LatLngTimestamp {
        public LatLng ll;
        public Date   expiry;  // UTC
        public LatLngTimestamp ( LatLng LL ) {
            ll     = LL;
            expiry = new Date();
            expiry.setTime( expiry.getTime() + EXPIRY );
        }
        public boolean expired() {
            Date now = new Date();  // UTC
            return expiry.before(now);
        }
    }

}  // class CachingGeoCoder


class LoadGeoCode extends AsyncTask<String, Void, LatLng> {
    private volatile static int sGeoCount = 0;

    private          Context         mContext;
    private          Geocoder        mGC;
    private          CachingGeoCoder mCGC;
    private volatile int             mVenueId;

    public LoadGeoCode( Context context, CachingGeoCoder CGC ) {
        mContext = context.getApplicationContext();
        mGC      = new Geocoder( context );
        mCGC     = CGC;
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
                LatLng ll = new LatLng(a.getLatitude(), a.getLongitude());
                mCGC.AddEntry( loc, ll );
                return ll;
            }
        } catch (IOException e) {
            Log.e( Global.LOG_TAG, "geocode #" + count + " io exception" );
        }

        return null;
    }

    @Override
    protected void onPostExecute( LatLng ll ) {
        if ( ll == null ) return;  // catch network errors, etc.

        // warning: duplicated code!
        MapActivity ma = Global.getInstance(mContext).mapActivity;
        if ( ma == null ) return;
        ma.addMarker( mContext, mVenueId, ll );
    }
}  // class LoadGeoCode
