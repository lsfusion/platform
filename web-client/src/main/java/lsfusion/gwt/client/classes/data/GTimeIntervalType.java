package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;

import static lsfusion.gwt.client.base.GwtSharedUtils.getTimeFormat;

public class GTimeIntervalType extends GIntervalType {

    public static GTimeIntervalType instance = new GTimeIntervalType();

    @Override
    public DateTimeFormat getSingleFormat(String pattern) {
        return getTimeFormat(pattern);
    }

    @Override
    public String getIntervalType() {
        return "TIME";
    }
}
