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
import java.util.Map;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.stream.JsonReader;

public class Global {

    // public
    public static final String          LOG_TAG = "VegDroid";

    public volatile MapActivity         mapActivity;  // provide access to activity for LoadGeoCode
    public volatile EntryListActivity   listActivity; // provide access to activity
    public volatile Map<Integer,Venue>  venues = Collections.synchronizedMap( new TreeMap<Integer,Venue>() );  // data store

    public volatile CachingGeoCoder     CGC = new CachingGeoCoder();

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


    public synchronized int getCatFilterMask() {
        return mCatFilterMask;
    }

    public synchronized boolean[] getCatFilterBool(Context context) {
        String[] list = context.getResources().getStringArray(R.array.categories);
        int len = list.length;
        boolean[] ret = new boolean[len];
        for ( int mask = mCatFilterMask, i = 0; mask != 0 && i < len; mask >>>= 1, i++ )
            ret[i] = (mask&1)==1 ? true : false;
        return ret;
    }

    // set whole mask at once
    public synchronized void setCatFilterMask( int mask ) {
        mCatFilterMask = mask;
    }

    // set category index to value val
    public synchronized void setCatFilter( int index, boolean val ) {
        if ( val )
            mCatFilterMask |= (1<<index);
        else
            mCatFilterMask &= ~(1<<index);
    }

    // commit to SharedPreferences
    public synchronized void commitCatFilter( Context context ) {
        if ( mCatFilterMask == mCatFilterMaskApplied )
            return;

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt( PREFS_CATMASK, mCatFilterMask );
        editor.commit();   // TODO: use apply() instead, requires API 9

        if ( mapActivity != null ) mapActivity.updateFilter( context );

        mCatFilterMaskApplied = mCatFilterMask;
    }

}  // class Global


class FilterDialog {

    //adapted from https://developer.android.com/guide/topics/ui/dialogs.html
    public static Dialog CreateDialog(Context c) {
        final Context context = c;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle( R.string.popup_title_filter_cat )
        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
            .setMultiChoiceItems(R.array.categories, Global.getInstance(context).getCatFilterBool(context),
                         new DialogInterface.OnMultiChoiceClickListener() {
                 //@Override
                 public void onClick(DialogInterface dialog, int which,
                         boolean isChecked) {
                     Global.getInstance(context).setCatFilter(which, isChecked);
                 }
             })
         // Set the action buttons
             .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                 //@Override
                 public void onClick(DialogInterface dialog, int id) {
                     Global.getInstance(context).commitCatFilter(context);
                 }
             });

         return builder.create();
    }

}  // class FilterDialog


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

        try {
            LatLng ll = llarray[0];
            String urlstring = "https://www.vegguide.org/search/by-lat-long/";
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

                            Global.getInstance(mContext).CGC.Resolve(mContext, v);
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
        }
        return null;
    }

}  // class LoadStream


