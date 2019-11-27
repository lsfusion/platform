package lsfusion.server.logics.classes.data.time;

import org.apache.http.ParseException;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class TimeConverter {

    private static final Map<String, String> TIME_FORMAT_REGEXPS = new HashMap<>();
    static {
        TIME_FORMAT_REGEXPS.put("^\\d{1,2}:\\d{1,2}:\\d{1,2}$", "HH:mm:ss");
        TIME_FORMAT_REGEXPS.put("^\\d{1,2}:\\d{1,2}$", "HH:mm");
    }
    
    public static Time smartParse(String timeString) {
        timeString = timeString.trim();
        if(timeString.isEmpty())
            return null;            
            
        for (String regexp : TIME_FORMAT_REGEXPS.keySet()) {
            if (timeString.toLowerCase().matches(regexp)) {
                return parseTime(TIME_FORMAT_REGEXPS.get(regexp), timeString);
            }
        }
        throw new ParseException("error parsing time: " + timeString);
    }

    public static Time parseTime(String pattern, String value) {
        try {
            return new Time(new SimpleDateFormat(pattern).parse(value).getTime());
        } catch (java.text.ParseException e) {
            return null;
        }
    }
}