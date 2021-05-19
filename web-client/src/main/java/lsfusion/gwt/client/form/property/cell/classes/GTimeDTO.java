package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;
import java.util.Objects;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultTimeFormat;

public class GTimeDTO implements Serializable {
    public int hour;
    public int minute;
    public int second;

    @SuppressWarnings("UnusedDeclaration")
    public GTimeDTO() {}

    public GTimeDTO(int hour, int minute, int second) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    public Time toTime() {
        return new Time(hour, minute, second);
    }

    public static GTimeDTO fromDate(Date date) {
        return new GTimeDTO(date.getHours(), date.getMinutes(), date.getSeconds());
    }

    public static GTimeDTO fromEpoch(Long epoch) {
        DateTimeFormat timeFormat = getDefaultTimeFormat();
        return fromDate(timeFormat.parse(timeFormat.format(new Date(epoch * 1000), TimeZone.createTimeZone(0))));
    }

    @Override
    public String toString() {
        return toTime().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GTimeDTO)) return false;
        GTimeDTO gTimeDTO = (GTimeDTO) o;
        return hour == gTimeDTO.hour && minute == gTimeDTO.minute && second == gTimeDTO.second;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hour, minute, second);
    }
}
