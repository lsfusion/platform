package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GDateTimeType;
import lsfusion.gwt.client.form.property.PValue;

import java.io.Serializable;
import java.util.Objects;

public class GDateTimeDTO implements Serializable {
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;
    public int second;
    public int millisecond;

    @SuppressWarnings("UnusedDeclaration")
    public GDateTimeDTO() {}

    public GDateTimeDTO(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;
    }

    public static GDateTimeDTO fromJsDate(JsDate date) {
        return new GDateTimeDTO(date.getFullYear(), date.getMonth() + 1, date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
    }

    public JsDate toJsDate() {
        return GwtClientUtils.createJsDate(year, month - 1, day, hour, minute, second, millisecond);
    }

    @Override
    public String toString() {
        assert false;
        return toJsDate().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GDateTimeDTO)) return false;
        GDateTimeDTO that = (GDateTimeDTO) o;
        return year == that.year && month == that.month && day == that.day && hour == that.hour && minute == that.minute && second == that.second && millisecond == that.millisecond;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day, hour, minute, second, millisecond);
    }
}
