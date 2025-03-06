package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GZDateTimeType;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;

public class GZDateTimeDTO implements Serializable {
    public long instant;

    @SuppressWarnings("UnusedDeclaration")
    public GZDateTimeDTO() {}

    public GZDateTimeDTO(long instant) {
        this.instant = instant;
    }

    public static PValue fromJsDate(JsDate date) {
        return PValue.getPValue(new GZDateTimeDTO(GwtClientUtils.getDateTimeMillis(date, MainFrame.timeZone).longValue()));
    }

    public JsDate toJsDate() {
        return GwtClientUtils.createJsDate(instant, MainFrame.timeZone);
    }

    @Override
    public String toString() {
        return GZDateTimeType.instance.formatString(PValue.getPValue(this), null);
    }
}
