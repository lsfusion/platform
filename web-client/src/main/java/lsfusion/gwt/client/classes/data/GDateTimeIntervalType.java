package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateTimeIntervalCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.DateTimeIntervalCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GDateTimeIntervalType extends GFormatType<com.google.gwt.i18n.client.DateTimeFormat>{

    public static GDateTimeIntervalType instance = new GDateTimeIntervalType();

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateTimeIntervalCellRenderer(property);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateTimeIntervalCellEditor(editManager);
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("DateTimeInterval doesn't support conversion from string", 0);
    }

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return getDateTimeFormat(pattern, false);
    }

    @Override
    protected Object getDefaultWidthValue() {
        return DateTimeIntervalCellRenderer.getWidthString(null, getDateTimeFormat(null, false));
    }

    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return DateTimeIntervalCellRenderer.getWidthString(null, getDateTimeFormat(null, false));
    }
}
