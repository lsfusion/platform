package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultDateFormat;

public class GDateDTO implements Serializable {
    public int year;
    public int month;
    public int day;

    @SuppressWarnings("UnusedDeclaration")
    public GDateDTO() {}

    public GDateDTO(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static GDateDTO fromDate(Date date) {
        return new GDateDTO(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
    }

    public static GDateDTO fromEpoch(Long epoch) {
        DateTimeFormat dateFormat = getDefaultDateFormat(false);
        return fromDate(dateFormat.parse(dateFormat.format(new Date(epoch * 1000), TimeZone.createTimeZone(0))));
    }

    public Date toDate() {
        return new java.sql.Date(year - 1900, month - 1, day);
    }
    
    @Override
    public String toString() {
        return toDate().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GDateDTO)) return false;
        GDateDTO gDateDTO = (GDateDTO) o;
        return year == gDateDTO.year && month == gDateDTO.month && day == gDateDTO.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }
}
