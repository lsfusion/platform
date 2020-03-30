package lsfusion.base;

import org.apache.http.ParseException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TimeConverter {

    private static final Map<String, String> TIME_FORMAT_REGEXPS = new HashMap<>();
    static {
        TIME_FORMAT_REGEXPS.put("^\\d{1,2}:\\d{1,2}:\\d{1,2}$", "HH:mm:ss");
        TIME_FORMAT_REGEXPS.put("^\\d{1,2}:\\d{1,2}$", "HH:mm");
    }
    
    public static LocalTime smartParse(String timeString) {
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

    public static LocalTime parseTime(String pattern, String value) {
        return LocalTime.parse(value, DateTimeFormatter.ofPattern(pattern));
    }
}