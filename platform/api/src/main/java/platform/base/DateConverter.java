package platform.base;

import java.sql.Timestamp;
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
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            assert dateToSql(date).equals(sqlDate);
            return sqlDate;
        }
    }

    public static Date sqlToDate(java.sql.Date date) {
        return date;
    }

    public static java.sql.Timestamp dateToStamp(Date date) {
        if (date == null) return null;

        return new Timestamp(date.getTime());
    }

    public static Date stampToDate(Timestamp date) {
        return new Date(date.getTime());
    }

}
