package lsfusion.server.logics.classes.data.time;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateTimeConverter {

    public static LocalDate getLocalDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        } else {
            return sqlDateToLocalDate((Date) value);
        }
    }

    public static LocalDate getWriteDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        } else {
            return sqlDateToLocalDate((Date) value);
        }
    }

    public static LocalTime getLocalTime(Object value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        } else {
            return sqlTimeToLocalTime((Time) value);
        }
    }

    public static LocalTime getWriteTime(Object value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        } else {
            return sqlTimeToLocalTime((Time) value);
        }
    }

    public static LocalDateTime getLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else {
            return sqlTimestampToLocalDateTime((Timestamp) value);
        }
    }

    public static LocalDateTime getWriteDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else {
            return sqlTimestampToLocalDateTime((Timestamp) value);
        }
    }

    public static java.sql.Time localTimeToSqlTime(LocalTime value) {
        return value != null ? java.sql.Time.valueOf(value) : null;
    }

    public static LocalTime sqlTimeToLocalTime(java.sql.Time value) {
        return value != null ? value.toLocalTime() : null;
    }

    public static java.sql.Date localDateToSqlDate(LocalDate value) {
        return value != null ? java.sql.Date.valueOf(value) : null;
    }

    public static LocalDate sqlDateToLocalDate(java.sql.Date value) {
        return value != null ? value.toLocalDate() : null;
    }

    public static java.sql.Timestamp localDateTimeToSqlTimestamp(LocalDateTime value) {
        return value != null ? java.sql.Timestamp.valueOf(value) : null;
    }

    public static LocalDateTime sqlTimestampToLocalDateTime(java.sql.Timestamp value) {
        return value != null ? value.toLocalDateTime() : null;
    }


}
