package de.thiemonagel.myapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    */
    
    public void Start( View view ) {
        Log.i( "MyApp", "MainActivity.Start()" );
    	Intent intent = new Intent(this, DisplayListActivity.class);
    	startActivity(intent);
    }
}
