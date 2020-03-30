package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public class GDateTimeDTO implements Serializable {
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;
    public int second;

    @SuppressWarnings("UnusedDeclaration")
    public GDateTimeDTO() {}

    public GDateTimeDTO(int year, int month, int day, int hour, int minute, int second) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    public static GDateTimeDTO fromDate(Date date) {
        return new GDateTimeDTO(date.getYear() + 1900, date.getMonth() + 1, date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds());
    }

    public Timestamp toDateTime() {
        return new Timestamp(year - 1900, month - 1, day, hour, minute, second, 0);
    }

    @Override
    public String toString() {
        return toDateTime().toString();
    }
}
