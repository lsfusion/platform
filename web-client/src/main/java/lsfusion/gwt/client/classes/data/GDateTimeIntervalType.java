package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDateTimeFormat;

public class GDateTimeIntervalType extends GIntervalType {

    public static GDateTimeIntervalType instance = new GDateTimeIntervalType();

    @Override
    public DateTimeFormat getSingleFormat(String pattern) {
        return getDateTimeFormat(pattern, false);
    }

    @Override
    public String getIntervalType() {
        return "DATETIME";
    }
}
