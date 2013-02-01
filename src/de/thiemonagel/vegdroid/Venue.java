package de.thiemonagel.vegdroid;

import java.io.IOException;
import java.util.Date;

import android.content.Context;

import com.google.gson.stream.JsonReader;

// all public members are initialized to non-null values
public class Venue {
    // TODO: veg_level
    public  volatile String name              = "";
    public  volatile String shortDescription  = "";
    public  volatile String longDescription   = "";

    public  volatile Date   closeDate         = new Date( ~0l>>>1 );  // in UTC
    public  volatile float  rating            = 0.f;
    public  volatile int    catMask           = 0;                    // bitmask of categories

    public  volatile String address1          = "";
    public  volatile String address2          = "";
    public  volatile String city              = "";
    public  volatile String postCode          = "";
    public  volatile String neighborhood      = "";
    public  volatile String phone             = "";
    public  volatile String website           = "";

    private volatile int    mid               = -1;


    // parse JSON stream into member variables
    public void parseJson( JsonReader reader, Context context ) throws IOException {
        String[] cats = context.getResources().getStringArray(R.array.categories);
        reader.beginObject();
        while ( reader.hasNext() ) {
            String item2 = reader.nextName();
            if ( item2.equals("name") ) {
                name = reader.nextString();
            } else if ( item2.equals("short_description") ) {
                shortDescription = reader.nextString();
            } else if ( item2.equals("long_description") ) {
                reader.beginObject();
                while ( reader.hasNext() ) {
                    String item3 = reader.nextName();
                    if ( item3.equals("text/html") ) {
                        longDescription = reader.nextString();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } else if ( item2.equals("close_date") ) {
                try {
                    // the time zone is ill-defined, therefore local time is used as a best guess
                    closeDate = DateParser.parseYmd( reader.nextString() );
                } catch ( java.text.ParseException e ) {
                    // when close_date exists, but the date cannot be parsed,
                    // the closing date is assumed to lie in the past
                    closeDate = new Date(0);
                }
            } else if ( item2.equals("weighted_rating") ) {
                try {
                    rating = Float.parseFloat( reader.nextString() );
                } catch (NumberFormatException e) {
                    // invalid numbers are ignored
                }
            } else if ( item2.equals("categories") ) {
                reader.beginArray();
                while ( reader.hasNext() ) {
                    String catItem = reader.nextString();
                    for ( int i = 0; i < cats.length; i++ ) {
                        if ( catItem.equals( cats[i] ) ) {
                            catMask |= (1<<i);
                            break;
                        }
                    }
                }
                reader.endArray();
            } else if ( item2.equals("address1") ) {
                address1 = reader.nextString();
            } else if ( item2.equals("address2") ) {
                address2 = reader.nextString();
            } else if ( item2.equals("city") ) {
                city = reader.nextString();
            } else if ( item2.equals("postal_code") ) {
                postCode = reader.nextString();
            } else if ( item2.equals("neighborhood") ) {
                neighborhood = reader.nextString();
            } else if ( item2.equals("phone") ) {
                phone = reader.nextString();
            } else if ( item2.equals("website") ) {
                website = reader.nextString();
            } else if ( item2.equals("uri") ) {
                setId( reader.nextString() );
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    // for use in geocoding
    public String locString() {
        String loc = "";
        loc += ( address1.equals("") ? "" : (loc.equals("")?"":", ") + address1 );
        loc += ( address2.equals("") ? "" : (loc.equals("")?"":", ") + address2 );
        loc += ( city    .equals("") ? "" : (loc.equals("")?"":", ") + city     );
        loc += ( postCode.equals("") ? "" : (loc.equals("")?"":", ") + postCode );
        return loc;
    }

    // for display use
    public String locHtml() {
        String a  = "";
        a += ( address1    .equals("") ? "" : (a.equals("")?"":"<br />") + address1 );
        a += ( address2    .equals("") ? "" : (a.equals("")?"":"<br />") + address2 );
        a += ( city        .equals("") ? "" : (a.equals("")?"":"<br />") + city     );
        a += ( postCode    .equals("") ? "" : (a.equals("")?"":"<br />") + postCode );
        a += ( neighborhood.equals("") ? "" : (a.equals("")?"":"<br />") + "<i>" + neighborhood + "</i>" );
        return a;
    }

    public void setId( String uri ) {
        String u = uri.trim();

        try {
            // remove trailing slashes
            while ( u.endsWith("/") ) {
                u = u.substring( 0, u.length()-1 );
            }
            mid = Integer.parseInt( u.substring( u.lastIndexOf('/')+1 ) );
        } catch ( IndexOutOfBoundsException e ) {
            // don't update mid in case of parse error
        } catch ( NumberFormatException e ) {
            // don't update mid in case of parse error
        }
    }

    public int getId() throws IllegalStateException {
        if ( mid == -1 )
            throw new IllegalStateException("Id has not been set!");
        return mid;
    }

    public boolean closed() {
        Date now = new Date();  // UTC
        return closeDate.before(now);
    }
}
