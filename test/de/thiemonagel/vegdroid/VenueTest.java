package de.thiemonagel.vegdroid;

import static org.junit.Assert.*;

import org.junit.Test;

public class VenueTest {

    @Test
    public void testSetId() {
        Venue v = new Venue();
        v.setId( "http://www.vegguide.org/entry/1"    ); assertEquals( 1,  v.getId() );
        v.setId( "http://www.vegguide.org/entry/2/"   ); assertEquals( 2,  v.getId() );
        v.setId( "http://www.vegguide.org/entry/34//" ); assertEquals( 34, v.getId() );
        v.setId( "https://www.vegguide.org/entry/1"   ); assertEquals( 1,  v.getId() );
    }

}
