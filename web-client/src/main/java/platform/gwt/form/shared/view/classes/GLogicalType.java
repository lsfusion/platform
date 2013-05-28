package platform.gwt.form.shared.view.classes;

import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.LogicalGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.LogicalGridCellRenderer;

import java.text.ParseException;

public class GLogicalType extends GDataType {
    public static GLogicalType instance = new GLogicalType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new LogicalGridCellRenderer();
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new LogicalGridCellEditor(editManager);
    }

    @Override
    public String getPreferredMask() {
        return "";
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        return 30;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        return 30;
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            return GwtSharedUtils.nullBoolean(Boolean.parseBoolean(s));
        } catch (NumberFormatException nfe) {
            throw new ParseException("string " + s + "can not be converted to logical", 0);
        }
    }

    @Override
    public String toString() {
        return "Логический класс";
    }
}
