package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDateFormat;

public class GDateIntervalType extends GIntervalType{

    public static GDateIntervalType instance = new GDateIntervalType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getDateFormat(pattern, false);
    }

    @Override
    protected Object getDefaultWidthValue() {
        return getWidthString(null, getDateFormat(null, false));
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return getWidthString(null, getDateFormat(null, false));
    }


    @Override
    public String getIntervalType() {
        return "DATE";
    }
}
