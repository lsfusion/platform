package lsfusion.interop.connection;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TFormats {
    // deprecated usage in desktop-client (everywhere except parsing)
    public final SimpleDateFormat date;
    public final SimpleDateFormat time;
    public final SimpleDateFormat dateTime;
    public final SimpleDateFormat zDateTime;

    public final Integer twoDigitYearStart;
    public final String datePattern;
    public final String timePattern;
    public final String dateTimePattern;
    public final String zDateTimePattern;

    public final DateTimeFormatter dateParser;
    public final DateTimeFormatter timeParser;
    public final DateTimeFormatter dateTimeParser;
    public final DateTimeFormatter zDateTimeParser;

    public final DateTimeFormatter dateFormatter;
    public final DateTimeFormatter timeFormatter;
    public final DateTimeFormatter dateTimeFormatter;
    public final DateTimeFormatter zDateTimeFormatter;

    public final TimeZone timeZone;

    public TFormats(Integer twoDigitYearStart, String datePattern, String timePattern, TimeZone timeZone) {
        this.twoDigitYearStart = twoDigitYearStart;
        this.datePattern = datePattern;
        this.timePattern = timePattern;
        String dateTimePattern = datePattern + " " + timePattern;
        this.dateTimePattern = dateTimePattern;
        this.zDateTimePattern = dateTimePattern;

        Date twoDigitYearStartDate = new GregorianCalendar(twoDigitYearStart != null ? twoDigitYearStart : Calendar.getInstance().get(Calendar.YEAR) - 80, Calendar.JANUARY, 1).getTime();

        date = new SimpleDateFormat(datePattern);
        date.set2DigitYearStart(twoDigitYearStartDate);

        time = new SimpleDateFormat(timePattern);

        dateTime = new SimpleDateFormat(dateTimePattern);
        dateTime.set2DigitYearStart(twoDigitYearStartDate);
        zDateTime = dateTime;

        dateParser = DateTimeFormatter.ofPattern(datePattern);
        timeParser = DateTimeFormatter.ofPattern(timePattern);
        dateTimeParser = DateTimeFormatter.ofPattern(dateTimePattern);
        zDateTimeParser = DateTimeFormatter.ofPattern(dateTimePattern);

        dateFormatter = dateParser;
        timeFormatter = timeParser;
        dateTimeFormatter = dateTimeParser;
        zDateTimeFormatter = zDateTimeParser;

        this.timeZone = timeZone;
    }
}
