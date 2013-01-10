package de.thiemonagel.vegdroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
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

        // make sure MyData is instantiated
        MyData.initInstance(this);

        // loading in background thread
        new Load().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.activity_display_list, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:  // no idea what this is for
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_filter_cat:
                CreateDialog().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // TODO: maybe some of these things may be skipped upon a repeated call of update() ???
    public void update() {
        SimpleAdapter adapter = new SimpleAdapter(
                DisplayListActivity.this, MyData.getInstance().getList(), R.layout.list_item,
                new String[] { "name",    "distance",    "cats_vlevel",   "short_description",    "uri",    "weighted_rating" },
                new int[]    { R.id.name, R.id.distance, R.id.categories, R.id.short_description, R.id.uri, R.id.ratingBar } );

        adapter.setViewBinder( new MyBinder() );

        // method inherited from ListActivity
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
                startActivity(in);
            }
        });
    }


    /**
     * Background Async Task to Load all product by making HTTP Request
     **/
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
            MyData.getInstance().updateList();

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();

            // show error, TODO: fix back button behaviour
            if ( !MyData.getInstance().getError().equals("") ) {
                AlertDialog.Builder b = new AlertDialog.Builder(DisplayListActivity.this);
                b   .setMessage( MyData.getInstance().getError() )
                    .setTitle( "Error" )
                    .create()
                    .show();
                return;
            }

            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                public void run() {
                    DisplayListActivity.this.update();
                }
            });

        }
    }

    // adapted from https://developer.android.com/guide/topics/ui/dialogs.html
    public Dialog CreateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle( R.string.popup_title_filter_cat )
        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
            .setMultiChoiceItems(R.array.categories, MyData.getInstance().getCatFilterBool(),
                        new DialogInterface.OnMultiChoiceClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which,
                        boolean isChecked) {
                    MyData.getInstance().setCatFilter(which, isChecked);
                }
            })
        // Set the action buttons
            .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int id) {
                    MyData.getInstance().updateList();
                    DisplayListActivity.this.update();
                    MyData.getInstance().commitCatFilter();
                }
            });

        return builder.create();
    }

}   // DisplayListActivity class


// http://stackoverflow.com/questions/7380865/android-how-can-you-set-the-value-of-a-ratingbar-within-a-listadapter
class MyBinder implements android.widget.SimpleAdapter.ViewBinder {
    private static final String LOG_TAG = "VegDroid";

    //@Override
    public boolean setViewValue(View view, Object data, String textRepresentation) {
        if (view.getId() == R.id.ratingBar) {
            String stringval = (String) data;
            Log.v( LOG_TAG, "rating: "+ stringval );
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

}   // MyBinder class
