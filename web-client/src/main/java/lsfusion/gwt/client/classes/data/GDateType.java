package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.DateCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;
import java.util.Date;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDateFormat;

public class GDateType extends GFormatType<DateTimeFormat> {

    public static GDateType instance = new GDateType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return GwtSharedUtils.getDateFormat(pattern);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateCellEditor(editManager, editProperty);
    }

    @Override
    public GDateDTO parseString(String value, String pattern) throws ParseException {
        return value.isEmpty() ? null : GDateDTO.fromDate(parseDate(value, getDateFormat(pattern)));
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateCellRenderer(property);
    }

    @Override
    protected Object getDefaultWidthValue() {
        return GDateTimeType.getWideFormattableDateTime();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDateCaption();
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }

    public static Date parseDate(String value, DateTimeFormat... formats) throws ParseException {
        for (DateTimeFormat format : formats) {
            try {
                Date date = format.parse(value);
                if(isValidDate(date))
                    return date;
            } catch (IllegalArgumentException ignore) {
            }
        }
        throw new ParseException("string " + value + "can not be converted to date", 0);
    }

    private static native boolean isValidDate(Date d)/*-{
        var date = new Date(d);
        return date instanceof Date && !isNaN(date);
    }-*/;

}
