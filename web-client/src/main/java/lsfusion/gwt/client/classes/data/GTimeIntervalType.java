package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtSharedUtils.getTimeFormat;

public class GTimeIntervalType extends GIntervalType {

    public static GTimeIntervalType instance = new GTimeIntervalType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getTimeFormat(pattern);
    }

    @Override
    protected Object getDefaultWidthValue() {
        return getWidthString(null, getTimeFormat(null));
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return getWidthString(null, getTimeFormat(null));
    }

    @Override
    public String getIntervalType() {
        return "TIME";
    }
}
