package de.thiemonagel.vegdroid;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class Global {
    private volatile static Global sInstance;

    public Map<Integer,Venue> venues = Collections.synchronizedMap( new TreeMap<Integer,Venue>() );

    // Obtain instance, constructing it if necessary.
    // Double-checked locking is ok with Java 5 or later because writing to volatile fInstance is atomic,
    // cf. http://www.javamex.com/tutorials/double_checked_locking_fixing.shtml
    public static Global getInstance() {
        if ( sInstance == null ) {
            synchronized (MyData.class) {
                if ( sInstance == null ) {
                    sInstance = new Global();
                }
            }
        }
        return sInstance;
    }

}
