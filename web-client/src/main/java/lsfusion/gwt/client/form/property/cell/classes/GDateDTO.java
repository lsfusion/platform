package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GDateType;
import lsfusion.gwt.client.form.property.PValue;

import java.io.Serializable;
import java.util.Objects;

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

    public static GDateDTO fromJsDate(JsDate date) {
        return new GDateDTO(date.getFullYear(), date.getMonth() + 1, date.getDate());
    }

    public JsDate toJsDate() {
        return GwtClientUtils.createJsDate(year, month - 1, day);
    }
    
    @Override
    public String toString() {
        return GDateType.instance.formatString(PValue.getPValue(this), null);
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
