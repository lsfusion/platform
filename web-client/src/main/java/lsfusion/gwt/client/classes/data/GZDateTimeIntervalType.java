package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDateTimeFormat;

public class GZDateTimeIntervalType extends GIntervalType {

    public static GZDateTimeIntervalType instance = new GZDateTimeIntervalType();

    @Override
    public String getIntervalType() {
        return "ZDATETIME";
    }

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getDateTimeFormat(pattern, false);
    }

    public String formatDate(String value){
        return getFormat(null).format(getTimestamp(value));
    }
}
