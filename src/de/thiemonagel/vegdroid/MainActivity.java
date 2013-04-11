package de.thiemonagel.vegdroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;

import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockActivity {
    private static final int    MASK_ALL   = 0x3ff;
    private static final int    MASK_FOOD  = 0x00f;
    private static final int    MASK_SHOP  = 0x050;
    private static final int    MASK_LODGE = 0x100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bt = (Button) findViewById(R.id.btCustom);
        bt.setText( Html.fromHtml( getString(R.string.button_previous) ) );

        // make sure MyData is instantiated
        MyData.initInstance(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // hide "custom" button when category filter has not been customized
        Button bt = (Button) findViewById(R.id.btCustom);
        int filter = Global.getInstance(this).getCatFilterMask() & MASK_ALL;
        Log.d( Global.LOG_TAG, "onResume() filter: " + filter );
        if ( filter == MASK_FOOD || filter == MASK_SHOP || filter == MASK_LODGE || filter == MASK_ALL )
            bt.setVisibility( View.GONE );
        else
            bt.setVisibility( View.VISIBLE );
    }

        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:  // no idea what this is for. This a a link to your icon in the actionbar. Most people set it to the home Screen of the app.
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void StartFood( View view ) {
        Global.getInstance(this).setCatFilterMask(MASK_FOOD);
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    public void StartShop( View view ) {
        Global.getInstance(this).setCatFilterMask(MASK_SHOP);
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    public void StartLodge( View view ) {
        Global.getInstance(this).setCatFilterMask(MASK_LODGE);
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    public void StartAll( View view ) {
        Global.getInstance(this).setCatFilterMask(MASK_ALL);
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    public void StartCustom( View view ) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

}
