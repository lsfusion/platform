package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.TextGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.TextGridCellRenderer;

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
    public int getMinimumPixelHeight(Integer fontSize) {
        return super.getMinimumPixelHeight(fontSize) * 4;
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
    public int getMinimumPixelWidth(int minimumCharWidth, Integer fontSize) {
        int minCharWidth = getMinimumCharWidth(minimumCharWidth);
        return fontSize == null ? minCharWidth * 10 : minCharWidth * fontSize * 5 / 8;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, Integer fontSize) {
        int prefCharWidth = getPreferredCharWidth(preferredCharWidth);
        return fontSize == null ? prefCharWidth * 10 : prefCharWidth * fontSize * 5 / 8;
    }
}
