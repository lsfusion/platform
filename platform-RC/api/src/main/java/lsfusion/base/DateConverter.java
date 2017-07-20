package lsfusion.base;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConverter {

    public static java.sql.Date getCurrentDate() {
        return dateToSql(new Date());
    }
    public static java.sql.Date dateToSql(Date date) {
        if (date == null) return null;

        if (date instanceof java.sql.Date)
            return (java.sql.Date) date;
        else
            return new java.sql.Date(date.getYear(), date.getMonth(), date.getDate());
    }
    
    public static java.sql.Date safeDateToSql(Date date) {
        if (date == null) return null;
        
        if (date instanceof java.sql.Date)
            return (java.sql.Date) date;
        else {
            return new java.sql.Date(date.getTime());
        }
    }

    public static void assertDateToSql(Date date) {
        if(date!=null) {
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            assert dateToSql(date).equals(sqlDate);
        }
    }

    public static Date sqlToDate(java.sql.Date date) {
        return date;
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

    public static SimpleDateFormat createTimeEditFormat(DateFormat dateFormat) {
        if (!(dateFormat instanceof SimpleDateFormat)) {
            //используем паттерн по умолчанию
            return new SimpleDateFormat("HH:mm:ss");
        }
        return createDateEditFormat((SimpleDateFormat)dateFormat);
    }


    public static SimpleDateFormat createDateEditFormat(DateFormat dateFormat) {
        SimpleDateFormat result;
        if (!(dateFormat instanceof SimpleDateFormat)) {
            //используем паттерн по умолчанию
            result = new SimpleDateFormat("dd.MM.yy");
        } else {
            result = createDateEditFormat((SimpleDateFormat) dateFormat);
            result.set2DigitYearStart(((SimpleDateFormat) dateFormat).get2DigitYearStart());
        }
        
        return result;
    }

    public static SimpleDateFormat createDateTimeEditFormat(DateFormat dateFormat) {
        SimpleDateFormat result;
        if (!(dateFormat instanceof SimpleDateFormat)) {
            //используем паттерн по умолчанию
            result = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        } else {
            result = createDateEditFormat((SimpleDateFormat) dateFormat);
            result.set2DigitYearStart(((SimpleDateFormat) dateFormat).get2DigitYearStart());
        }
        
        return result;
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
}
