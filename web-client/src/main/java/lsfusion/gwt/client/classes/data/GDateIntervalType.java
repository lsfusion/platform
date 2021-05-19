package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDateFormat;

public class GDateIntervalType extends GIntervalType{

    public static GDateIntervalType instance = new GDateIntervalType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getDateFormat(pattern, false);
    }

    @Override
    public String format(Long epoch) {
        return getFormat(null).format(GDateDTO.fromEpoch(epoch).toDate());
    }

    @Override
    public String getIntervalType() {
        return "DATE";
    }
}
