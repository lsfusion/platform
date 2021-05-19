package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultDateTimeFormat;

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

    public static GDateTimeDTO fromEpoch(Long epoch) {
        DateTimeFormat dateTimeFormat = getDefaultDateTimeFormat(false);
        return fromDate(dateTimeFormat.parse(dateTimeFormat.format(new Date(epoch * 1000), TimeZone.createTimeZone(0))));
    }

    public Timestamp toDateTime() {
        return new Timestamp(year - 1900, month - 1, day, hour, minute, second, 0);
    }

    @Override
    public String toString() {
        return toDateTime().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GDateTimeDTO)) return false;
        GDateTimeDTO that = (GDateTimeDTO) o;
        return year == that.year && month == that.month && day == that.day && hour == that.hour && minute == that.minute && second == that.second;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day, hour, minute, second);
    }
}
