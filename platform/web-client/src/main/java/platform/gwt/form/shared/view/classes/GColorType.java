package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.ColorGridCellEditor;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.ColorGridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.text.ParseException;

public class GColorType extends GDataType {
    public static GColorType instance = new GColorType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new ColorGridCellEditor(editManager, editProperty);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ColorGridCellRenderer();
    }

    @Override
    public String getPreferredMask() {
        return "";
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        return 40;
    }

    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        return 40;
    }

    @Override
    public Object parseString(String s) throws ParseException {
        throw new ParseException("Color class doesn't support conversion from string", 0);
    }

    @Override
    public String toString() {
        return "Цвет";
    }
}
