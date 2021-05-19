package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDateTimeFormat;

public class GDateTimeIntervalType extends GIntervalType {

    public static GDateTimeIntervalType instance = new GDateTimeIntervalType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getDateTimeFormat(pattern, false);
    }

    @Override
    public String format(Long epoch) {
        return getFormat(null).format(GDateTimeDTO.fromEpoch(epoch).toDateTime());
    }

    @Override
    public String getIntervalType() {
        return "DATETIME";
    }
}
