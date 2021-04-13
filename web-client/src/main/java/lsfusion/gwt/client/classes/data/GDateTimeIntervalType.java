package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GDateTimeIntervalType extends GIntervalType {

    public static GDateTimeIntervalType instance = new GDateTimeIntervalType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getDateTimeFormat(pattern, false);
    }

    @Override
    protected Object getDefaultWidthValue() {
        return getWidthString(null, getDateTimeFormat(null, false));
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return getWidthString(null, getDateTimeFormat(null, false));
    }

    @Override
    public String getIntervalType() {
        return "DATETIME";
    }
}
