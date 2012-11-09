package de.thiemonagel.myapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.TelephonyManager;
import android.util.FloatMath;
import android.util.Log;

public class MyData {
	private static MyData instance = null;

	Context                                  fContext;
	ArrayList<HashMap<String, String>>       fDataList;  // currently to be displayed
	HashMap<String, HashMap<String, String>> fDataMap;   // cache of full information
	boolean                                  fLoaded;
	boolean                                  fkm;        // whether distances are to be displayed in km
	
	private MyData( Context c ) {
		fContext  = c;
		fDataList = new ArrayList<HashMap<String, String>>();
        fDataMap  = new HashMap<String,HashMap<String, String>>();
        fLoaded   = false;

    	// derive preferred units from SIM card country
    	TelephonyManager tm = (TelephonyManager)fContext.getSystemService(Context.TELEPHONY_SERVICE);
    	String ISO = tm.getSimCountryIso().toLowerCase();
	    Log.i( "MyApp", "SIM country ISO: " + ISO );
	    
	    // it seems that only USA and GB still use miles:
	    // https://en.wikipedia.org/wiki/Imperial_units#Current_use_of_imperial_units
    	if (    ISO.equals("gb")
    		 || ISO.equals("io")   // British Indian Ocean Territory
    		 || ISO.equals("uk")   // wrong, but safe to put here "just in case"
    		 || ISO.equals("um")   // U.S. Minor Outlying Islands
    		 || ISO.equals("us") 
    		 || ISO.equals("vg")   // British Virgin Islands
    		 || ISO.equals("vi") ) // U.S. Virgin Islands
    		fkm = false;
    	else
    		fkm = true;
}
	
	// provide application context!
	public static void initInstance( Context c ) {
		if ( instance == null )
			instance = new MyData( c );
	}

	public static MyData getInstance() {
		assert( instance != null );
		return instance;
	}
	
    protected void Load() {
    	if ( fLoaded )
    		return;
    	
		// find location
		LocationManager lMan = (LocationManager) fContext.getSystemService(Context.LOCATION_SERVICE);
		List<String> lproviders = lMan.getProviders( false );  // true = enabled only
		Location best = null;
	    Log.i( "MyApp", lproviders.size() + " location providers found." );
	    for ( String prov : lproviders ) {
	    	Location l = lMan.getLastKnownLocation(prov);
	
	    	String logstr = prov + ": ";
	    	if ( l != null ) {
	    		logstr += l.getLatitude();
	    		logstr += ", " + l.getLongitude();
	    		logstr += ", time: " + l.getTime();
	    		if ( l.hasAccuracy() ) {
	    			logstr += ", error: " + l.getAccuracy() + " m";
	    		}
	    	} else {
	    		logstr += "[empty]";
	    	}
	    	Log.i( "MyApp", logstr );
	    	if ( l == null ) {
	    		continue;
	    	}
	    	
	    	if ( best == null ) {
	    		best = l;
	    		continue;
	    	}
	    	
	    	// if one reading doesn't have accuracy, the latest is preferred
	    	if ( !best.hasAccuracy() || !l.hasAccuracy() ) {
	    		if ( l.getTime() > best.getTime() ) {
	    			best = l;
	    		}
				continue;
	    	}
	    	
	    	long  btime = best.getTime();     // ms
	    	long  ltime = l.getTime();        // ms
	    	float bacc  = best.getAccuracy(); // m
	    	float lacc  = l.getAccuracy();    // m
	    	
	    	// both have accuracy, l is more recent and more accurate
	    	if ( ltime > btime && lacc < bacc ) {
	    		best = l;
	    		continue;
	    	}
	    	
	    	long  tdist = ltime - btime;
	    	float dist  = l.distanceTo( best );
	    	// agreement in sigmas
	    	float agr  = dist / FloatMath.sqrt( bacc*bacc + lacc*lacc );
	    	
	    	// use outdated but more precise measurement only
	    	// when agreement isn't too bad and time difference isn't
	    	// too large
	    	float crit = 1e5f / tdist;
	    	if ( crit < 3f ) { crit = 3f; }
	    	if ( agr < crit ) {
	    		if ( lacc < bacc ) {
	    			best = l;
	    		}
	    	} else {
	    		if ( ltime > btime ) {
	    			best = l;
	    		}
	    	}
	    }
	    
	    String url = "http://www.vegguide.org/search/by-lat-long/";
    	float locationAccuracy;
	    if ( best == null ) {
	        Log.i( "MyApp", "No location found." );
	        url += "0,0";
	        locationAccuracy = .75f;
	    } else {
	    	//url += "44.9617005,-93.2766566";
			url += best.getLatitude() + "," + best.getLongitude();
			locationAccuracy = best.getAccuracy() / ( fkm ? 1000f : 1609.344f ); 
	    }
	    
	    int roundMultiplier;  // for km/miles
	    int roundDigits;
	    if ( locationAccuracy < .015f ) {
	    	roundMultiplier = 1000;
	    	roundDigits     = 3;
	    } else if ( locationAccuracy < .15f ) {
	    	roundMultiplier = 100;
	    	roundDigits     = 2;
	    } else if ( locationAccuracy < 1.5f ) {
	    	roundMultiplier = 10;
	    	roundDigits     = 1;
	    } else {
	    	roundMultiplier = 1;
	    	roundDigits     = 0;
	    }
	    Log.i( "MyApp", "roundMultiplier: " + roundMultiplier );
	    
	    url += "?unit=km&distance=100&limit=50";
	    
	    Log.i( "MyApp", "Getting: " +url );
	    HttpClient client = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet( url );
	    StringBuilder builder = new StringBuilder();
	    try {
	      HttpResponse response = client.execute(httpGet);
	      StatusLine statusLine = response.getStatusLine();
	      int statusCode = statusLine.getStatusCode();
	      if (statusCode == 200) {
	        HttpEntity entity = response.getEntity();
	        InputStream content = entity.getContent();
	        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
	        String line;
	        while ((line = reader.readLine()) != null) {
	          builder.append(line);
	        }
	      } else {
	    	  // bad
	      }
	    } catch (ClientProtocolException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    
	    Log.i( "MyApp", builder.toString() );
	    
	    try {
	    	JSONObject json = new JSONObject(builder.toString());
	        JSONArray entries = json.getJSONArray("entries");
	        for ( int i = 0; i < entries.length(); i++ ) {
	        	JSONObject entry = entries.getJSONObject(i);
	//        	Log.i("MyApp", entry.getString("name") );
	
	        	HashMap<String, String> map = new HashMap<String, String>();
	        	String keylist[] = {
	        			"address1", "address2", "city", "distance", 
	        			"name", "neighborhood", "phone", "postal_code",
	        			"price_range", "short_description", "uri",
	        			"veg_level", "website", "weighted_rating" }; 

	        	// missing keys are set to empty strings
	        	for ( String key : keylist ) {
		        	String s = "";
		        	try {
		            	s = entry.getString(key);
		            } catch (JSONException e) {};
	            	map.put( key, s );
	        	}

	            // long description (use short description if long one is missing)
	            String ldes;
	            try {
	            	ldes = entry.getJSONObject("long_description").getString("text/html");
	            } catch (JSONException e) { ldes = map.get("short_description"); }
	            map.put( "long_description", ldes );
	            
	            String cats;
	            try {
		            JSONArray acats = entry.getJSONArray( "categories" );
		            cats = "(";
		            for ( int j = 0; j < acats.length(); j++ ) {
		            	if ( cats != "(" ) {
		            		cats += ", ";
		            	}
		            	cats += acats.getString(j);
		            }
		            cats += ")";
	            } catch (JSONException e) { cats = ""; }
	            map.put( "categories", cats );
	
	            // store results
	            try {
	            	String uri = entry.getString("uri");
		            // add to cache
		            fDataMap.put( uri, map );

		            String sd = map.get("distance");
		            float  fd = 1e10f;
	            	try {
	            		fd = Float.parseFloat(sd);
	            	} catch (Throwable e) {};
	            	if ( !fkm ) fd /= 1.609344;  // international yard and pound treaty (1959)
	            	fd = Math.round(fd*roundMultiplier) / (float) roundMultiplier;
	            	if ( fd < 1f )
	            		if ( fkm )
	            			sd = String.format( "%.0f m", fd*1000 );
	            		else
	            			sd = String.format( "%.0f yds", fd*1760 );
	            	else
	            		sd = String.format( "%."+roundDigits+"f %s", fd, ( fkm ? " km" : " miles" ) );
		            map.put("distance", sd);
		            
		            // add to list for current display
		            fDataList.add( map );
	            } catch (JSONException e) { Log.e("MyApp", "uri missing!"); }
	        }
	    } catch (JSONException e) {
	        e.printStackTrace();
	    }
	    
	    fLoaded = true;
    }
}
