package lsfusion.interop.connection;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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

    public TFormats(Integer twoDigitYearStart, String datePattern, String timePattern) {
        this.twoDigitYearStart = twoDigitYearStart;
        this.datePattern = datePattern;
        this.timePattern = timePattern;
        String dateTimePattern = datePattern + " " + timePattern;
        this.dateTimePattern = dateTimePattern;
        this.zDateTimePattern = dateTimePattern;

//        Date twoDigitYearStartDate = null;
//        if (twoDigitYearStart != null) {
//            GregorianCalendar c = new GregorianCalendar(twoDigitYearStart, Calendar.JANUARY, 1);
//            twoDigitYearStartDate = c.getTime();
//        }

        //dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        date = new SimpleDateFormat(datePattern);
//        if (twoDigitYearStartDate != null) {
//            date.set2DigitYearStart(twoDigitYearStartDate);
//        }

        //timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        time = new SimpleDateFormat(timePattern);

        //dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        dateTime = new SimpleDateFormat(dateTimePattern);
//        if (twoDigitYearStartDate != null) {
//            dateTime.set2DigitYearStart(twoDigitYearStartDate);
//        }
        zDateTime = dateTime;

        dateParser = DateTimeFormatter.ofPattern(datePattern);
        timeParser = DateTimeFormatter.ofPattern(timePattern);
        dateTimeParser = DateTimeFormatter.ofPattern(dateTimePattern);
        zDateTimeParser = DateTimeFormatter.ISO_DATE_TIME;

        dateFormatter = dateParser;
        timeFormatter = timeParser;
        dateTimeFormatter = dateTimeParser;
        zDateTimeFormatter = zDateTimeParser;
    }
}
