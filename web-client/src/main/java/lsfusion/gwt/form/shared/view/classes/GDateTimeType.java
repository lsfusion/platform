package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.DateTimeGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.DateGridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;

import static lsfusion.gwt.base.shared.GwtSharedUtils.*;

public class GDateTimeType extends GDataType {
    public static GDateTimeType instance = new GDateTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer(property, getDefaultDateTimeFormat());
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateTimeGridCellEditor(editManager, editProperty);
    }

    @Override
    public Timestamp parseString(String value) throws ParseException {
        Date date = GDateType.parseDate(value, getDefaultDateTimeFormat(), getDefaultDateTimeShortFormat(), getDefaultDateFormat());
        return value.isEmpty() ? null : new Timestamp(date.getTime());
    }

    @Override
    public String getPreferredMask() {
        return "01.01.2001 00:00:00";
    }

    @Override
    public String toString() {
        return "Дата со временем";
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }
}
