package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDateFormat;
import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultDateFormat;

public class GDateIntervalType extends GIntervalType{

    public static GDateIntervalType instance = new GDateIntervalType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getDateFormat(pattern, false);
    }

    @Override
    public String format(Long epoch) {
        return getFormat(null).format(fromEpoch(epoch, getDefaultDateFormat(false)).toGDateDTO().toDate());
    }

    @Override
    public String getIntervalType() {
        return "DATE";
    }
}
