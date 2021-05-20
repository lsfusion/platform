package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDateTimeFormat;
import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultDateTimeFormat;

public class GDateTimeIntervalType extends GIntervalType {

    public static GDateTimeIntervalType instance = new GDateTimeIntervalType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getDateTimeFormat(pattern, false);
    }

    @Override
    public String format(Long epoch) {
        return getFormat(null).format(fromEpoch(epoch, getDefaultDateTimeFormat(false)).toDateTime());
    }

    @Override
    public String getIntervalType() {
        return "DATETIME";
    }
}
