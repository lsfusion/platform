package lsfusion.interop.connection;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalePreferences implements Serializable {
    public Locale locale;
    public String timeZone;
    public Integer twoDigitYearStart;
    public String dateFormat;
    public String timeFormat;
    public String dateTimeFormat;

    public LocalePreferences(Locale locale, String timeZone, Integer twoDigitYearStart, String dateFormat, String timeFormat) {
        this.locale = locale;
        this.timeZone = getTimeZone(timeZone);
        this.twoDigitYearStart = twoDigitYearStart;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
        this.dateTimeFormat = dateFormat + " " + timeFormat; //correspond to TFormats dateTimePattern
    }

    private static final Pattern offsetTimeZone = Pattern.compile("(?:UTC|GMT|UT)?([+-])(\\d{1,2})(?::?(\\d{2}))?", Pattern.CASE_INSENSITIVE);

    // a time zone written as an offset (UTC+5, GMT-8:00, +05:00) means that many hours east of UTC (west for a minus),
    // but the database reads UTC+5 and GMT-8:00 the other way round (the POSIX notation), TimeZone.getTimeZone (the server, the desktop client)
    // does not know UTC+5 and +05:00 and takes UTC, and moment.tz (the web client) knows none of them,
    // so it is replaced with the Etc/GMT zone of the same offset, which all of them read the same way (the tz database inverts its sign)
    // an offset that is not whole hours has no such zone and is left as it is
    public static String getTimeZone(String timeZone) {
        if (timeZone == null)
            return null;

        Matcher offset = offsetTimeZone.matcher(timeZone);
        if (!offset.matches())
            return timeZone;

        int hours = Integer.parseInt(offset.group(2));
        if (offset.group(1).equals("-"))
            hours = -hours;
        if ((offset.group(3) != null && Integer.parseInt(offset.group(3)) != 0) || hours < -12 || hours > 14)
            return timeZone;

        return hours == 0 ? "Etc/UTC" : "Etc/GMT" + (hours > 0 ? "-" : "+") + Math.abs(hours);
    }

    public static Locale getLocale(String language, String country) {
        if (language != null) {
            return new Locale(language, country == null ? "" : country);
        }
        return null; // default
    }
}
