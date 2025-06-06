package lsfusion.base;

import lsfusion.base.lambda.EFunction;
import lsfusion.interop.form.property.cell.IntervalValue;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DateConverter {

    public static java.sql.Date safeDateToSql(Date date) {
        if (date == null) return null;
        
        if (date instanceof java.sql.Date)
            return (java.sql.Date) date;
        else {
            return new java.sql.Date(date.getTime());
        }
    }

    public static Date stampToDate(Timestamp stamp) {
        return new Date(stamp.getTime());
    }

    public static Date timeToDate(Time time) {
        return new Date(time.getTime());
    }

    public static java.sql.Time dateToTime(Date date) {
        if (date == null) return null;

        return new Time(date.getTime());
    }

    public static java.sql.Timestamp dateToStamp(Date date) {
        if (date == null) return null;

        return new Timestamp(date.getTime());
    }

    public static SimpleDateFormat createDateEditFormat(SimpleDateFormat simpleFormat) {
        //преобразует данный формат в новый, в котором всем числовым полям даётся максимум места
        //это нужно для того, чтобы можно было создать корректную маску для эдитора

        String doubleSymbols = "GMwdaHhKkms";

        String pattern = simpleFormat.toPattern();
        int patternLength = pattern.length();
        StringBuilder resultPattern = new StringBuilder(patternLength);
        for (int i = 0; i < patternLength;) {
            char ch = pattern.charAt(i);

            int chCnt = 1;
            while (i + chCnt < patternLength && pattern.charAt(i + chCnt) == ch) ++chCnt;
            i += chCnt;

            if (ch == 'Y' || ch == 'y') {
                if (chCnt > 2) {
                    chCnt = 4;
                } else {
                    chCnt = 2;
                }
            } else if (ch == 'S') {
                chCnt = 3;
            } else if (doubleSymbols.indexOf(ch) != -1) {
                //округляем до верхнего чётного
                chCnt = ((chCnt + 1) >> 1) << 1;
            }
            for (int j = 0; j < chCnt; ++j) {
                resultPattern.append(ch);
            }
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(resultPattern.toString());
//        simpleDateFormat.set2DigitYearStart(new Date(45, 1, 1));
        return simpleDateFormat;
    }

    private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<>();
    private static final Map<String, String> DATETIME_FORMAT_REGEXPS = new HashMap<>();
    private static final Map<String, String> ZONED_DATETIME_FORMAT_REGEXPS = new HashMap<>();

    static {
        DATE_FORMAT_REGEXPS.put("^\\d{1,2}\\.\\d{1,2}\\.\\d{1,2}$", "dd.MM.yy");
        DATE_FORMAT_REGEXPS.put("^\\d{8}$", "yyyyMMdd");
        DATE_FORMAT_REGEXPS.put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
        DATE_FORMAT_REGEXPS.put("^\\d{1,2}\\.\\d{1,2}\\.\\d{4}$", "dd.MM.yyyy");
        DATE_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
        DATE_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}[+-]\\d{2}:\\d{2}$", "yyyy-MM-ddXXX");
        DATE_FORMAT_REGEXPS.put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
        DATE_FORMAT_REGEXPS.put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
        DATE_FORMAT_REGEXPS.put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
        DATE_FORMAT_REGEXPS.put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
    }

    static {
        DATETIME_FORMAT_REGEXPS.put("^\\d{12}$", "yyyyMMddHHmm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy H:mm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\.\\d{1,2}\\.\\d{1,2}\\s\\d{1,2}:\\d{2}$", "dd.MM.yy H:mm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\.\\d{1,2}\\.\\d{4}\\s\\d{1,2}:\\d{2}$", "dd.MM.yyyy H:mm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd H:mm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy H:mm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd H:mm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy H:mm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy H:mm");
        DATETIME_FORMAT_REGEXPS.put("^\\d{14}$", "yyyyMMddHHmmss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}-\\d{1,2}-\\d{4}t\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy'T'H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\.\\d{1,2}\\.\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd.MM.yy H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\.\\d{1,2}\\.\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd.MM.yyyy H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\.\\d{1,2}\\.\\d{4}t\\d{1,2}:\\d{2}:\\d{2}$", "dd.MM.yyyy'T'H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}.\\d{1,7}$", "yyyy-MM-dd H:mm:ss[.[SSSSSSS][SSSSSS][SSSSS][SSSS][SSS][SS][S]]");
        DATETIME_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}t\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd'T'H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}/\\d{1,2}/\\d{4}t\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy'T'H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{4}/\\d{1,2}/\\d{1,2}t\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd'T'H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}t\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy'T'H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}t\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy'T'H:mm:ss");
        DATETIME_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}t\\d{1,2}:\\d{2}:\\d{2}\\.\\d{1,7}$", "yyyy-MM-dd'T'H:mm:ss[.[SSSSSSS][SSSSSS][SSSSS][SSSS][SSS][SS][S]]");
    }
    private static final String DATE_SYMBOLS_REGEXP = "[.-/:]";

    static {
        ZONED_DATETIME_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}t\\d{1,2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}$", "yyyy-MM-dd'T'H:mm:ssXXX");
        ZONED_DATETIME_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}t\\d{1,2}:\\d{2}:\\d{2}\\.\\d[+-]\\d{2}:\\d{2}$", "yyyy-MM-dd'T'H:mm:ss.SXXX");
        ZONED_DATETIME_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}t\\d{1,2}:\\d{2}(([+-]\\d{2}:\\d{2})|z)$", "yyyy-MM-dd'T'H:mmXXX");
        ZONED_DATETIME_FORMAT_REGEXPS.put("^\\d{4}-\\d{1,2}-\\d{1,2}t\\d{1,2}:\\d{2}:\\d{2}\\.\\d{1,7}(([+-]\\d{2}:\\d{2})|z)$", "yyyy-MM-dd'T'H:mm:ss[.[SSSSSSS][SSSSSS][SSSSS][SSSS][SSS][SS][S]]XXX");
    }

    public static LocalDateTime smartParse(String dateString) {
        dateString = dateString.trim();
        if(dateString.isEmpty())
            return null;            
            
        for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(DATE_FORMAT_REGEXPS.get(regexp))).atStartOfDay();
            }
        }

        for (String regexp : DATETIME_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(DATETIME_FORMAT_REGEXPS.get(regexp)));
            }
        }

        for (String regexp : ZONED_DATETIME_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return ZonedDateTime.parse(dateString, DateTimeFormatter.ofPattern(ZONED_DATETIME_FORMAT_REGEXPS.get(regexp))).toLocalDateTime();
            }
        }
    
        dateString = dateString.replaceAll(DATE_SYMBOLS_REGEXP, "").trim();
        if(dateString.isEmpty())
            return null;

        throw new RuntimeException("Error parsing date: " + dateString);
    }

    public static Instant smartParseInstant(String dateString) {
        dateString = dateString.trim();
        if(dateString.isEmpty())
            return null;

        try {
            return ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        for (String regexp : ZONED_DATETIME_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return ZonedDateTime.parse(dateString, DateTimeFormatter.ofPattern(ZONED_DATETIME_FORMAT_REGEXPS.get(regexp))).toInstant();
            }
        }

        dateString = dateString.replaceAll(DATE_SYMBOLS_REGEXP, "").trim();
        if(dateString.isEmpty())
            return null;

        throw new RuntimeException("Error parsing date: " + dateString);
    }

    public static LocalDate sqlDateToLocalDate(java.sql.Date value) {
        return value != null ? value.toLocalDate() : null;
    }

    public static java.sql.Date localDateToSqlDate(LocalDate value) {
        return value != null ? java.sql.Date.valueOf(value) : null;
    }

    public static LocalDateTime sqlTimestampToLocalDateTime(java.sql.Timestamp value) {
        return value != null ? value.toLocalDateTime() : null;
    }

    public static java.sql.Timestamp localDateTimeToSqlTimestamp(LocalDateTime value) {
        return value != null ? java.sql.Timestamp.valueOf(value) : null;
    }

    public static Instant sqlTimestampToInstant(java.sql.Timestamp value) {
        return value != null ? value.toInstant() : null;
    }

    public static java.sql.Timestamp instantToSqlTimestamp(Instant value) {
        return value != null ? java.sql.Timestamp.from(value) : null;
    }

    public static Long localDateTimeToUTCEpoch(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public static LocalDateTime epochToLocalDateTime(long epoch) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.of("UTC"));
    }

    public static <E extends Exception> Object parseInterval(String s, EFunction<String, Long, E> parseFunction) throws E {
        String[] dates = s.split(" - ");
        Long epochFrom = parseFunction.apply(dates[0]);
        Long epochTo = parseFunction.apply(dates[1]);
        return epochFrom <= epochTo ? new IntervalValue(epochFrom, epochTo) : null;
    }

    public static String formatInterval(Object obj, Function<Long, String> formatFunction) {
        IntervalValue interval = IntervalValue.parseIntervalValue(obj);
        return formatFunction.apply(interval.from) + " - " + formatFunction.apply(interval.to);
    }
}
