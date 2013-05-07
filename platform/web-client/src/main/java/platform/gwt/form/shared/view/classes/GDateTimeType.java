package platform.gwt.form.shared.view.classes;

import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.DateTimeGridCellEditor;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.DateGridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.sql.Timestamp;
import java.text.ParseException;

public class GDateTimeType extends GDataType {
    public static GDateTimeType instance = new GDateTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer(property, GwtSharedUtils.getDefaultDateTimeFormat());
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateTimeGridCellEditor(editManager, editProperty);
    }

    @Override
    public Timestamp parseString(String s) throws ParseException {
        try {
            return new Timestamp(GwtSharedUtils.getDefaultDateTimeFormat().parse(s).getTime());
        } catch (IllegalArgumentException e) {
            throw new ParseException("string " + s + "can not be converted to datetime", 0);
        }
    }

    @Override
    public String getPreferredMask() {
        return "01.01.2001 00:00:00";
    }

    @Override
    public String toString() {
        return "Дата со временем";
    }
}
