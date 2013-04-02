package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.TextGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.TextGridCellRenderer;

import java.text.ParseException;

public class GTextType extends GDataType {
    public static GTextType instance = new GTextType();

    @Override
    public String getMinimumMask() {
        return "999 999";
    }

    @Override
    public String getPreferredMask() {
        return "9 999 999";
    }

    @Override
    public int getMinimumPixelHeight(GFont font) {
        return super.getMinimumPixelHeight(font) * 4;
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new TextGridCellRenderer(property);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new TextGridCellEditor(editManager, editProperty);
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        int minCharWidth = getMinimumCharWidth(minimumCharWidth);
        return font == null || font.size == null ? minCharWidth * 10 : minCharWidth * font.size * 5 / 8;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        int prefCharWidth = getPreferredCharWidth(preferredCharWidth);
        return font == null || font.size == null ? prefCharWidth * 10 : prefCharWidth * font.size * 5 / 8;
    }

    @Override
    public Object parseString(String s) throws ParseException {
        return s;
    }
}
