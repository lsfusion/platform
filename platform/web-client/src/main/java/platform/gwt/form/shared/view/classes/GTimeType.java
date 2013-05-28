package platform.gwt.form.shared.view.classes;

import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.TimeGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.DateGridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.sql.Time;
import java.text.ParseException;

public class GTimeType extends GDataType {
    public static GTimeType instance = new GTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer(property, GwtSharedUtils.getDefaultTimeFormat());
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new TimeGridCellEditor(editManager, editProperty);
    }

    @Override
    public Time parseString(String s) throws ParseException {
        try {
            if (s.split(":").length == 2) {
                s += ":00";
            }
            return s.isEmpty() ? null : new Time(GwtSharedUtils.getDefaultTimeFormat().parse(s).getTime());
        } catch(IllegalArgumentException e) {
            throw new ParseException("string " + s + "can not be converted to time", 0);
        }
    }

    @Override
    public String getPreferredMask() {
        return "00:00:00";
    }

    @Override
    public String toString() {
        return "Время";
    }
}
