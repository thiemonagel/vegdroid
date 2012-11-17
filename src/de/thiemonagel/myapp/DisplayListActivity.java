package de.thiemonagel.myapp;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class DisplayListActivity extends ListActivity {
	
    // Progress Dialog
    private ProgressDialog pDialog;
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_list);
        //getActionBar().setDisplayHomeAsUpEnabled(true);

        // loading in background thread
        MyData.initInstance( getApplicationContext() );
        new Load().execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    /**
     * Background Async Task to Load all product by making HTTP Request
     * */
    class Load extends AsyncTask<String, String, String> {
 
        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(DisplayListActivity.this);
            pDialog.setMessage("Loading. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }
 
        /**
         * getting All products from url
         * */
        protected String doInBackground(String... args) {
        	
        	MyData.getInstance().Load();
            
            return null;
        }
 
        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
        	pDialog.dismiss();

        	// show error, TODO: fix back button behaviour
        	if ( !MyData.getInstance().fError.equals("") ) {
        		AlertDialog.Builder b = new AlertDialog.Builder(DisplayListActivity.this);
        		b	.setMessage( MyData.getInstance().fError )
        			.setTitle( "Error" )
        			.create()
        			.show();
        		return;
        	}

        	// updating UI from Background Thread
            runOnUiThread(new Runnable() {
                public void run() {
                    /**
                     * Updating parsed JSON data into ListView
                     * */
                    SimpleAdapter adapter = new SimpleAdapter(
                            DisplayListActivity.this, MyData.getInstance().fDataList, R.layout.list_item,
                            new String[] { "name",    "distance",    "cats_vlevel",   "short_description",    "uri",    "weighted_rating" },
                            new int[]    { R.id.name, R.id.distance, R.id.categories, R.id.short_description, R.id.uri, R.id.ratingBar });
                    
                    adapter.setViewBinder( new MyBinder() );
                    
                    // updating listview
                    setListAdapter(adapter);
                    
                    ListView lv = getListView();
                    
                    // Launching new screen on Selecting Single ListItem
                    lv.setOnItemClickListener(new OnItemClickListener() {
             
                        // @Override
                        public void onItemClick(AdapterView<?> parent, View view,
                                int position, long id) {
                            // getting values from selected ListItem
                            String uri = ((TextView) view.findViewById(R.id.uri)).getText().toString();
             
                            // Starting new intent
                            Intent in = new Intent(getApplicationContext(), EntryActivity.class);
                            in.putExtra( "uri", uri );
                            Log.i( "MyApp", "putting extra uri: '" + uri + "'" );
                            startActivity(in);
                        }
                    });
                }
            });
 
        }
    }
}

// http://stackoverflow.com/questions/7380865/android-how-can-you-set-the-value-of-a-ratingbar-within-a-listadapter
class MyBinder implements android.widget.SimpleAdapter.ViewBinder {
    //@Override
    public boolean setViewValue(View view, Object data, String textRepresentation) {
        if (view.getId() == R.id.ratingBar) {
            String stringval = (String) data;
            Log.i( "MyApp", "rating: "+ stringval );
            float ratingValue = 0f;
            try {
            	ratingValue = Float.parseFloat(stringval);
            } catch (Throwable e) {};
            RatingBar ratingBar = (RatingBar) view;
            ratingBar.setRating(ratingValue);
            return true;
        }
        return false;
    }
}

