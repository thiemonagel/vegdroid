package de.thiemonagel.vegdroid;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class AboutActivity extends SherlockActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // display Google Maps API licensing info, as required in
        // https://developers.google.com/maps/documentation/android/intro
        {
            TextView tv = (TextView) findViewById(R.id.aboutText);
            tv.setText( Html.fromHtml(getString(R.string.text_about)) );
            tv.setMovementMethod( LinkMovementMethod.getInstance() );
        }{
            TextView tv = (TextView) findViewById(R.id.GMapLegalese);
            tv.setText( GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo( this ) );
        }
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_about, menu);
        return true;
    }
    */

    public void OK( View view ) {
        finish();
    }

}
