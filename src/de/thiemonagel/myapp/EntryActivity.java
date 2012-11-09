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
import android.widget.TextView;

public class EntryActivity extends Activity {
	
	String                  furi;
	HashMap<String, String> fData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i( "MyApp", "EntryActivity.onCreate()" );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        
        Intent i = getIntent();
        furi = i.getStringExtra( "uri" );
    	Log.i( "MyApp", "EntryActivity.onCreate() uri: " + furi );
    	fData = MyData.getInstance().fDataMap.get(furi);

    	{ 
    		TextView tv = (TextView) findViewById(R.id.name);
    		tv.setText( fData.get("name") );
    	}{ 
    		TextView tv = (TextView) findViewById(R.id.phone);
    		tv.setText( Html.fromHtml( "<a href='tel:" + fData.get("phone") + "'>" + fData.get("phone") + "</a>" ) );
    		tv.setMovementMethod( LinkMovementMethod.getInstance() );
    	}{ 
    		TextView tv = (TextView) findViewById(R.id.website);
    		tv.setText( Html.fromHtml( "<a href='" + fData.get("website") + "'>" + fData.get("website") + "</a>" ) );
    		tv.setMovementMethod( LinkMovementMethod.getInstance() );
    	}{ 
    		TextView tv = (TextView) findViewById(R.id.address);
    		String n  = fData.get("neighborhood");
    		String a1 = fData.get("address1");
    		String a2 = fData.get("address2");
    		String c  = fData.get("city");
    		String pc = fData.get("postal_code");
    		String a  = ( n.equals("") ? "" : "<em>" + n + "</em><br />" );
    		a        +=  a1 + "<br />" + a2 + ( a2.equals("") ? "" : "<br />" ) + c + " " + pc; 
    		tv.setText( Html.fromHtml(a) );
    	}{ 
    		TextView tv = (TextView) findViewById(R.id.long_description);
    		tv.setText( Html.fromHtml( fData.get("long_description") ) );
    		tv.setMovementMethod( LinkMovementMethod.getInstance() );
    	} 

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_entry, menu);
        return true;
    }
    
    public void actionWebsite( View view ) {
        Log.i( "MyApp", "actionWebsite()" );
        Uri uri = Uri.parse( fData.get("website") );
    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    	startActivity(intent);
    }
}
