package platform.base;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateConverter {

    public static java.sql.Date dateToSql(Date date) {
        if(date==null) return null;

        if(date instanceof java.sql.Date)
            return (java.sql.Date) date;
        else
            return new java.sql.Date(date.getTime());
    }

    public static Date sqlToDate(java.sql.Date date) {
        return date;
    }

}
