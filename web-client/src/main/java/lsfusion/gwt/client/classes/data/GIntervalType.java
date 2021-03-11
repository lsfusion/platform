package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.IntervalCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.FormatCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.sql.Timestamp;
import java.text.ParseException;

public abstract class GIntervalType extends GFormatType<com.google.gwt.i18n.client.DateTimeFormat> {

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new FormatCellRenderer<Object, DateTimeFormat>(property) {
            @Override
            public String format(Object value) {
                return getWidthString(value, format);
            }
        };
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new IntervalCellEditor(editManager, getIntervalType());
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("GInterval doesn't support conversion from string", 0);
    }

    public static Timestamp getTimestamp(String value) {
        return new Timestamp(Long.parseLong(value) * 1000);
    }

    public String getWidthString(Object value, DateTimeFormat format) {
        if (value == null)
            value = 1634245200.1634331600; // some dateTimeInterval for default width
        String object = String.valueOf(value);
        int indexOfDecimal = object.indexOf(".");
        return format.format(getTimestamp(object.substring(0, indexOfDecimal)))
                + " - " + format.format(getTimestamp(object.substring(indexOfDecimal + 1)));
    }

    public abstract String getIntervalType();
}
