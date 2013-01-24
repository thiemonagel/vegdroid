package de.thiemonagel.vegdroid;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Calendar;

import org.junit.Test;

public class DateParserTest {

    @Test
    public void testParseYmd() throws Exception {
        Calendar cal = Calendar.getInstance();
        long offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        assertEquals(             0l - offset, DateParser.parseYmd("1970-01-01").getTime() );
        assertEquals(      86400000l - offset, DateParser.parseYmd("1970-01-02").getTime() );
        assertEquals( 1325376000000l - offset, DateParser.parseYmd("2012-01-01").getTime() );
    }

    @Test(expected=ParseException.class)
    public void testParseYmdError1() throws Exception {
        DateParser.parseYmd("1970-1-1");
    }

    @Test(expected=ParseException.class)
    public void testParseYmdError2() throws Exception {
        DateParser.parseYmd("1970-00-01");
    }

    @Test(expected=ParseException.class)
    public void testParseYmdError3() throws Exception {
        DateParser.parseYmd("1970-01-00");
    }

    @Test(expected=ParseException.class)
    public void testParseYmdError4() throws Exception {
        DateParser.parseYmd("1970-02-30");
    }

    @Test
    public void testParseRfc3339() throws Exception {
        assertEquals(             0l, DateParser.parseRfc3339("1970-01-01t00:00:00z")     .getTime() );
        assertEquals(             0l, DateParser.parseRfc3339("1970-01-01T00:00:00z")     .getTime() );
        assertEquals(             0l, DateParser.parseRfc3339("1970-01-01t00:00:00Z")     .getTime() );
        assertEquals(             0l, DateParser.parseRfc3339("1970-01-01T00:00:00Z")     .getTime() );
        assertEquals(          1000l, DateParser.parseRfc3339("1970-01-01T00:00:01Z")     .getTime() );
        assertEquals(       3600000l, DateParser.parseRfc3339("1970-01-01T01:00:00Z")     .getTime() );
        assertEquals(      86400000l, DateParser.parseRfc3339("1970-01-02T00:00:00Z")     .getTime() );
        assertEquals( 1325376000000l, DateParser.parseRfc3339("2012-01-01T00:00:00Z")     .getTime() );
        assertEquals(             0l, DateParser.parseRfc3339("1970-01-01T00:00:00+00:00").getTime() );
        assertEquals(             0l, DateParser.parseRfc3339("1970-01-01T00:00:00-00:00").getTime() );
        assertEquals(             0l, DateParser.parseRfc3339("1970-01-01T11:11:00+11:11").getTime() );

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
    public void testParseRfc3339Error1() throws Exception {
        DateParser.parseRfc3339("1970-01-01T00:00:0");
    }

    @Test(expected=ParseException.class)
    public void testParseRfc3339Error2() throws Exception {
        DateParser.parseRfc3339("1970-01-01T00:00:00");
    }

    @Test(expected=ParseException.class)
    public void testParseRfc3339Error3() throws Exception {
        DateParser.parseRfc3339("1970-00-01T00:00:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseRfc3339Error4() throws Exception {
        DateParser.parseRfc3339("1970-01-00T00:00:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseRfc3339Error5() throws Exception {
        DateParser.parseRfc3339("1970-01-01T24:00:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseRfc3339Error6() throws Exception {
        DateParser.parseRfc3339("1970-01-01T00:60:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseRfc3339Error7() throws Exception {
        DateParser.parseRfc3339("1970-01-01T00:00:62Z");
    }

    @Test(expected=ParseException.class)
    public void testParseRfc3339Error8() throws Exception {
        DateParser.parseRfc3339("1970-02-29T00:00:00Z");
    }

    @Test(expected=ParseException.class)
    public void testParseRfc3339Error9() throws Exception {
        DateParser.parseRfc3339("1970-01-01T00:00:00.0Z");
    }

}
