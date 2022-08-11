package lsfusion.base;

import org.apache.http.ParseException;

import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TimeConverter {

    private static final Map<String, String> TIME_FORMAT_REGEXPS = new HashMap<>();
    static {
        TIME_FORMAT_REGEXPS.put("^\\d{1,2}:\\d{2}:\\d{2}$", "H:mm:ss");
        TIME_FORMAT_REGEXPS.put("^\\d{1,2}:\\d{2}$", "H:mm");
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

    public static Time getWriteTime(Object value) {
        if(value instanceof LocalTime) {
            return localTimeToSqlTime((LocalTime) value);
        } else {
            return (Time) value;
        }
    }

    public static LocalTime parseTime(String pattern, String value) {
        return LocalTime.parse(value, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalTime sqlTimeToLocalTime(java.sql.Time value) {
        return value != null ? value.toLocalTime() : null;
    }

    public static java.sql.Time localTimeToSqlTime(LocalTime value) {
        return value != null ? java.sql.Time.valueOf(value) : null;
    }
}