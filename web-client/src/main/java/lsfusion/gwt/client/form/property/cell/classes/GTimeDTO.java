package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GTimeType;
import lsfusion.gwt.client.form.property.PValue;

import java.io.Serializable;
import java.util.Objects;

public class GTimeDTO implements Serializable {
    public int hour;
    public int minute;
    public int second;
    public int millisecond;

    @SuppressWarnings("UnusedDeclaration")
    public GTimeDTO() {}

    public GTimeDTO(int hour, int minute, int second, int millisecond) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;
    }

    public static GTimeDTO fromJsDate(JsDate date) {
        return new GTimeDTO(date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());
    }

    // should correspond ClientTimeIntervalClass, TimeIntervalClass
    public JsDate toJsDate() {
        return GwtClientUtils.createJsDate(90, 0, 1, hour, minute, second, millisecond);
    }

    @Override
    public String toString() {
        assert false;
        return toJsDate().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GTimeDTO)) return false;
        GTimeDTO gTimeDTO = (GTimeDTO) o;
        return hour == gTimeDTO.hour && minute == gTimeDTO.minute && second == gTimeDTO.second && millisecond == gTimeDTO.millisecond;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hour, minute, second, millisecond);
    }
}
