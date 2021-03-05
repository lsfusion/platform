package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.sql.Timestamp;

public class DateTimeIntervalCellRenderer extends FormatCellRenderer<Object, DateTimeFormat> {

    public DateTimeIntervalCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public String format(Object value) {
        return getWidthString(value, format);
    }

    public static String getWidthString(Object value, DateTimeFormat format) {
        if (value == null)
            value = 1634245200.1634331600; // some dateTimeInterval for default width
        String object = String.valueOf(value);
        int indexOfDecimal = object.indexOf(".");
        return format.format(getTimestamp(object.substring(0, indexOfDecimal)))
                + " - " + format.format(getTimestamp(object.substring(indexOfDecimal + 1)));
    }

    public static Timestamp getTimestamp(String value) {
        return new Timestamp(Long.parseLong(value) * 1000);
    }
}
