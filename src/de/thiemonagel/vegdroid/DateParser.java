package de.thiemonagel.vegdroid;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateParser {

    // parse yyyy-MM-dd format which is assumed to be in the local time zone,
    // return Date in UTC
    public static java.util.Date parseYmd(String str) throws java.text.ParseException {
        if ( str.length() != 10 ) throw new ParseException( "Ymd:  Illegal string size!", str.length()-1 );

        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        sdf.setLenient ( false );
        sdf.applyPattern("yyyy-MM-dd");
        return sdf.parse(str);
    }

    // parse RFC 3339 datetime, return Date in UTC
    public static java.util.Date parseRfc3339(String str) throws java.text.ParseException {
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        sdf.setLenient ( false );

        int len = str.length();
        if ( len < 20 ) throw new ParseException( "Rfc3339:  String too short!", len-1 );

        String pattern = "yyyy-MM-dd";
        if ( str.charAt(10) == 'T' ) {
            pattern += "'T'";
        } else if ( str.charAt(10) == 't' ) {
            pattern += "'t'";
        } else {
            throw new ParseException( "Rfc3339:  T not found!", 10 );
        }
        pattern += "HH:mm:ssz";

        // SimpleDateFormat parses milliseconds whereas RFC 3339 specifies fractional seconds
        // so that using the ".SSS" pattern would lead to wrong results.  As fractional seconds
        // are rarely used, they're not (yet) implemented here.  TODO
        if ( str.charAt(19) == '.' ) {
            throw new ParseException( "Rfc3339:  fractional seconds not implemented!", 19 );
        }

        // modify time zone representation to conform to SDF's "general time zone" format
        char last = str.charAt(len-1);
        if ( last == 'Z' || last == 'z' ) {
            str = str.substring(0, len-1) + "GMT+00:00";
        } else {
            str = str.substring(0, len-6) + "GMT" + str.substring(len-6, len);
        }

        sdf.applyPattern(pattern);
        return sdf.parse(str);
    }
}
