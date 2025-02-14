package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.GwtClientUtils;

import java.io.Serializable;
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

    public static GTimeDTO fromJsDate(JsDate date) {
        return new GTimeDTO(date.getHours(), date.getMinutes(), date.getSeconds());
    }

    // should correspond ClientTimeIntervalClass, TimeIntervalClass
    public JsDate toJsDate() {
        return GwtClientUtils.createJsDate(90, 0, 1, hour, minute, second);
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
        return hour == gTimeDTO.hour && minute == gTimeDTO.minute && second == gTimeDTO.second;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hour, minute, second);
    }
}
