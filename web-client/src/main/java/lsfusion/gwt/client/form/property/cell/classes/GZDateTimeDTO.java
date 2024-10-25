package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.PValue;

import java.io.Serializable;

public class GZDateTimeDTO implements Serializable {
    public long instant;

    @SuppressWarnings("UnusedDeclaration")
    public GZDateTimeDTO() {}

    public GZDateTimeDTO(long instant) {
        this.instant = instant;
    }

    public static PValue fromJsDate(JsDate date) {
        return PValue.getPValue(new GZDateTimeDTO((long) date.getTime()));
    }

    public JsDate toJsDate() {
        return GwtClientUtils.createJsDate(instant);
    }

    @Override
    public String toString() {
        return toJsDate().toString();
    }
}
