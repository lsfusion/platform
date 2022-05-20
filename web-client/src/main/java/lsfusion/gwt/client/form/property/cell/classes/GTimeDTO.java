package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

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

    // should correspond ClientTimeIntervalClass, TimeIntervalClass
    public Date toTime() {
        return new Date(90, 0, 1, hour, minute, second);
    }

    public static GTimeDTO fromDate(Date date) {
        return new GTimeDTO(date.getHours(), date.getMinutes(), date.getSeconds());
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
