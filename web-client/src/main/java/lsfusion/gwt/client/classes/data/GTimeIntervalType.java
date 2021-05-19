package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;

import static lsfusion.gwt.client.base.GwtSharedUtils.getTimeFormat;

public class GTimeIntervalType extends GIntervalType {

    public static GTimeIntervalType instance = new GTimeIntervalType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getTimeFormat(pattern);
    }

    @Override
    public String format(Long epoch) {
        return getFormat(null).format(GTimeDTO.fromEpoch(epoch).toTime());
    }

    @Override
    public String getIntervalType() {
        return "TIME";
    }
}
