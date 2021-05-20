package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;

import java.util.Date;

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

    @Override
    public String format(Long epoch) {
        return getFormat(null).format(new Date(epoch * 1000));
    }
}
