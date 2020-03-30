package lsfusion.base;

import java.time.LocalTime;

public class TimeConverter {

    public static LocalTime sqlTimeToLocalTime(java.sql.Time value) {
        return value != null ? value.toLocalTime() : null;
    }

    public static java.sql.Time localTimeToSqlTime(LocalTime value) {
        return value != null ? java.sql.Time.valueOf(value) : null;
    }
}