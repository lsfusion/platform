package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.IntervalCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.FormatCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;
import java.util.Date;

public abstract class GIntervalType extends GFormatType<com.google.gwt.i18n.client.DateTimeFormat> {

    public static GIntervalType getInstance(String type) {
        switch (type) {
            case "DATE":
                return GDateIntervalType.instance;
            case "TIME":
                return GTimeIntervalType.instance;
            case "DATETIME":
                return GDateTimeIntervalType.instance;
            case "ZDATETIME":
                return GZDateTimeIntervalType.instance;
        }
        return null;
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new FormatCellRenderer<Object, DateTimeFormat>(property) {
            @Override
            public String format(Object value) {
                return formatObject(value);
            }
        };
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new IntervalCellEditor(editManager, getIntervalType(), this);
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("GInterval doesn't support conversion from string", 0);
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return formatObject(1634245200.1634331600); // some dateTimeInterval for default width
    }

    public abstract String format(Long epoch);

    public String formatObject(Object value) {
        String object = String.valueOf(value);
        int delimiterIndex = object.indexOf(".");
        return format(Long.parseLong(object.substring(0, delimiterIndex))) + " - " + format(Long.parseLong(object.substring(delimiterIndex + 1)));
    }

    public abstract String getIntervalType();

    public static GDateTimeDTO fromEpoch(Long epoch, DateTimeFormat format) {
        return GDateTimeDTO.fromDate(format.parse(format.format(new Date(epoch * 1000), TimeZone.createTimeZone(0))));
    }
}
