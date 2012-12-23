package de.thiemonagel.myapp;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class EntryActivity extends Activity {
	private static final String LOG_TAG = "VegDroid";
	
	String                  furi;
	HashMap<String, String> fData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        
        Intent i = getIntent();
        furi = i.getStringExtra( "uri" );
    	fData = MyData.getInstance().getMap().get(furi);

    	{ 
    		TextView tv = (TextView) findViewById(R.id.name);
    		tv.setText( fData.get("name") );
    	}{ 
    		TextView tv = (TextView) findViewById(R.id.veg_level_description);
    		tv.setText( fData.get("veg_level_description") );
    	}{ 
    		Button but = (Button) findViewById(R.id.phone);
    		if ( fData.get("phone").equals("") )
    			but.setVisibility( View.GONE );
    		else
    			but.setText( "Dial " + fData.get("phone") );
    	}{ 
    		Button but = (Button) findViewById(R.id.website);
    		if ( fData.get("website").equals("") )
    			but.setVisibility( View.GONE );
    		else
    			but.setText( "Visit " + fData.get("website") );
    	}{ 
    		TextView tv = (TextView) findViewById(R.id.address);
    		String n  = fData.get("neighborhood");
    		String a1 = fData.get("address1");
    		String a2 = fData.get("address2");
    		String c  = fData.get("city");
    		String pc = fData.get("postal_code");
    		String a  = ( n.equals("")  ? "" : "<em>" + n + "</em>" );
    		a        += ( a1.equals("") ? "" : (a.equals("")?"":"<br />") + a1 );
    		a        += ( a2.equals("") ? "" : (a.equals("")?"":"<br />") + a2 );
    		a        += ( c.equals("")  ? "" : (a.equals("")?"":"<br />") + c  );
    		a        += ( pc.equals("") ? "" : (a.equals("")?"":"<br />") + pc );
    		if ( a.equals("") )
    			tv.setVisibility( View.GONE );
    		else
    			tv.setText( Html.fromHtml(a) );
    	}{ 
    		TextView tv = (TextView) findViewById(R.id.long_description);
    		tv.setMovementMethod( LinkMovementMethod.getInstance() );
    		if ( fData.get("long_description").equals("") )
    			tv.setVisibility( View.GONE );
    		else
    			tv.setText( Html.fromHtml( fData.get("long_description") ) );
    		Log.v( LOG_TAG, "long desc: " + fData.get("long_description") );
    	} 

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_entry, menu);
        return true;
    }
    
    public void clickPhone( View view ) {
    	String uri = "tel:" + fData.get("phone");
    	Intent intent = new Intent( Intent.ACTION_DIAL );
    	intent.setData( Uri.parse(uri) );
    	startActivity(intent);
    }

    public void clickWebsite( View view ) {
    	Intent intent = new Intent( Intent.ACTION_VIEW );
    	intent.setData( Uri.parse(fData.get("website")) );
    	startActivity(intent);
    }
}
