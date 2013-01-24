package de.thiemonagel.vegdroid;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import org.junit.Test;

public class Rfc3339Test {

    @Test
    public void testParseDate() throws Exception {
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01")               .getTime() );
        assertEquals(      86400000l, Rfc3339.parseDate("1970-01-02")               .getTime() );
        assertEquals( 1325376000000l, Rfc3339.parseDate("2012-01-01")               .getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01t00:00:00z")     .getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T00:00:00z")     .getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01t00:00:00Z")     .getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T00:00:00Z")     .getTime() );
        assertEquals(          1000l, Rfc3339.parseDate("1970-01-01T00:00:01Z")     .getTime() );
        assertEquals(       3600000l, Rfc3339.parseDate("1970-01-01T01:00:00Z")     .getTime() );
        assertEquals(      86400000l, Rfc3339.parseDate("1970-01-02T00:00:00Z")     .getTime() );
        assertEquals( 1325376000000l, Rfc3339.parseDate("2012-01-01T00:00:00Z")     .getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T00:00:00+00:00").getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T00:00:00-00:00").getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T11:11:00+11:11").getTime() );

        /* maybe some day fractional seconds will be implemented
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T00:00:00.0Z")   .getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T00:00:00.00Z")  .getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T00:00:00.000Z") .getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T00:00:00.0000Z").getTime() );
        assertEquals(             0l, Rfc3339.parseDate("1970-01-01T00:00:00.0001Z").getTime() );
        assertEquals(           100l, Rfc3339.parseDate("1970-01-01T00:00:00.1Z")   .getTime() );
        assertEquals(           100l, Rfc3339.parseDate("1970-01-01T00:00:00.10Z")  .getTime() );
        assertEquals(           100l, Rfc3339.parseDate("1970-01-01T00:00:00.100Z") .getTime() );
        assertEquals(            10l, Rfc3339.parseDate("1970-01-01T00:00:00.010Z") .getTime() );
        assertEquals(             1l, Rfc3339.parseDate("1970-01-01T00:00:00.001Z") .getTime() );
        */
    }

    @Test(expected=ParseException.class)
    public void testParseDateError1() throws Exception {
        Rfc3339.parseDate("1970-01-1");
    }

    @Test(expected=ParseException.class)
    public void testParseDateError2() throws Exception {
        Rfc3339.parseDate("1970-01-01T00:00:0");
    }

    @Test(expected=ParseException.class)
    public void testParseDateError3() throws Exception {
        Rfc3339.parseDate("1970-01-01T00:00:00");
    }

    @Test(expected=ParseException.class)
    public void testParseDateError4() throws Exception {
        Rfc3339.parseDate("1970-00-01T00:00:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseDateError5() throws Exception {
        Rfc3339.parseDate("1970-01-00T00:00:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseDateError6() throws Exception {
        Rfc3339.parseDate("1970-01-01T24:00:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseDateError7() throws Exception {
        Rfc3339.parseDate("1970-01-01T00:60:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseDateError8() throws Exception {
        Rfc3339.parseDate("1970-01-01T00:00:62Z");
    }

    @Test(expected=ParseException.class)
    public void testParseDateError9() throws Exception {
        Rfc3339.parseDate("1970-02-29T00:00:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseDateError10() throws Exception {
        Rfc3339.parseDate("1970-01-01T00:00:00.0Z");
    }

}
